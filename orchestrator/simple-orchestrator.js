// warm-pool-orchestrator.js — Final: Nur Warm Pool, mit Retry-Wartezeit, Code-Bereinigung
"use strict";

const http = require("http");
const net = require("net");
const { spawnSync, spawn } = require("child_process");
const { v4: uuidv4 } = require("uuid");

/* ─── Config ─── */
const PORT  = Number(process.env.ORCH_PORT || 4000);
const HOST  = process.env.ORCH_HOST || "0.0.0.0";
const DOCKER = process.env.DOCKER_BIN || "/usr/bin/docker";

// FIX: Ports auf 25571 erhöht (6 Ports) und MAX_TOTAL auf 6
const PORT_MIN = 25566, PORT_MAX = 25571;
const MAX_TOTAL = 6, MATCH_CAP = 4, WARM_CAP = 2;

const MANAGED_LABEL = "projectm.managed=true";
const GAME_IMAGE = process.env.GAME_IMAGE || "realpingi/game:ready";

const RCON_PORT = Number(process.env.RCON_PORT || 25575);
const RCON_PASS = process.env.RCON_PASS || "changeme";

const RUN_TIMEOUT_MS   = 10_000;
// KRITISCHER FIX: READY_TIMEOUT_MS auf 90 Sekunden erhöht (für langsame Server-Starts)
const READY_TIMEOUT_MS = 90_000;
// RCON_READY_TIMEOUT_MS wird nicht mehr verwendet
const TCP_TIMEOUT_MS   = 600;
const GAME_PING_HOST   = process.env.GAME_PING_HOST || "127.0.0.1";

/* ─── State ─── */
// Merkt sich Teams-B64 für warm->match Assigns (da Env nachträglich fehlt)
const warmAssignedCache = new Map();

/* 🔒 Player-Locks gegen Race-Window */
const playerLocks = new Map();              // Map<playerId, "match:ID" | true>
const MATCH_LOCK_PREFIX = "match:";         // Marker, um permanente Locks zu identifizieren


/* 🧾 Idempotency (gegen doppelte create-match) */
const idempCache = new Map();               // Map<key, { expiresAt, responseJson }>
const IDEMP_TTL_MS = 30_000;

/* ─── Utils ─── */
const sh = (cmd, args, opts={}) => {
  const r = spawnSync(cmd, args, { encoding: "utf8", ...opts });
  if (r.error) throw r.error;
  return { code: r.status ?? 0, out: (r.stdout || "").trim(), err: (r.stderr || "").trim() };
};

const psManaged = (all=false) => {
  const args = ["ps","--format","{{.ID}}|{{.Names}}|{{.Ports}}|{{.Status}}","--filter",`label=${MANAGED_LABEL}`];
  if (all) args.splice(1,0,"-a");
  const { code, out } = sh(DOCKER, args);
  if (code !== 0 || !out) return [];
  return out.split("\n").filter(Boolean).map(l=>{
    const [id,name,ports,status] = l.split("|");
    return { id, name, ports: ports||"", status: status||"" };
  });
};

const listWarm    = (all=false)=> psManaged(all).filter(c => c.name?.startsWith("warm-"));
const listMatches = (all=false)=> psManaged(all).filter(c => c.name?.startsWith("match-"));
const runningMatchesCount = ()=> listMatches(false).length;

const tcp = (host,port,ms=TCP_TIMEOUT_MS)=> new Promise(res=>{
  const s=net.createConnection({host,port});
  let done=false; const finish=(ok)=>{ if(!done){ done=true; try{s.destroy();}catch{}; res(ok);} };
  s.setTimeout(ms,()=>finish(false));
  s.on("error",()=>finish(false));
  s.on("connect",()=>finish(true));
});

const waitPort = async (host, port, maxMs=READY_TIMEOUT_MS, step=200) => {
  const start=Date.now();
  while (Date.now()-start < maxMs) {
    if (await tcp(host, port)) return true;
    await new Promise(r=>setTimeout(r, step));
  }
  return false;
};

const usedHostPorts = () => {
  const { code, out } = sh(DOCKER, ["ps","-a","--format","{{.Ports}}"]);
  if (code !== 0 || !out) return new Set();
  const s = new Set();
  for (const line of out.split("\n")) {
    const m = line.match(/(?::|0\.0\.0\.0:|\[::\]:)(\d+)->25565\/tcp/g);
    if (m) for (const hit of m) {
      const num = Number((hit.match(/(\d+)->25565\/tcp/)||[])[1]);
      if (num) s.add(num);
    }
  }
  return s;
};

const getFreePort = () => {
  const used = usedHostPorts();
  for (let p=PORT_MIN; p<=PORT_MAX; p++) if (!used.has(p)) return p;
  const labeled = psManaged(false).length;
  if (used.size && labeled < MAX_TOTAL) {
    console.warn(`[PORT] all ${PORT_MIN}-${PORT_MAX} used but only ${labeled} managed running → unmanaged blocking`);
  }
  return null;
};

const rmContainer = (idOrName) => {
  try {
    const all = psManaged(true);
    const found = all.find(c => c.id===idOrName || c.name===idOrName);
    if (found) warmAssignedCache.delete(found.id);
    sh(DOCKER,["rm","-f",idOrName]);
    return true;
  } catch { return false; }
};

/* ─── Inspect helpers ─── */
function fetchMatchDetails(containerId, initial = {}) {
  let hp=null, rp=null, teamsB64=null;
  try{
    const { code, out } = sh(DOCKER, ["inspect", containerId]);
    if (code === 0 && out) {
      const js = JSON.parse(out)[0] || {};
      const gp = js?.NetworkSettings?.Ports?.["25565/tcp"];
      const rk = `${RCON_PORT}/tcp`;
      const rpMap = js?.NetworkSettings?.Ports?.[rk];
      hp = Number(Array.isArray(gp) && gp[0]?.HostPort);
      rp = Number(Array.isArray(rpMap) && rpMap[0]?.HostPort);
      // NOTE: TEAMS_CONFIG_B64 wird nur für Cold Start gesetzt (den wir jetzt nicht mehr nutzen).
      // Daher bleibt die Cache-Lookup-Logik unten essenziell.
    }
  }catch(e){ /* ignore; we'll use fallbacks */ }

  if (!teamsB64 && warmAssignedCache.has(containerId)) teamsB64 = warmAssignedCache.get(containerId);
  if (!hp && initial.ports) {
    const m = (initial.ports||"").match(/:(\d+)->25565\/tcp/);
    if (m) hp = Number(m[1]);
  }
  if (!rp && hp) rp = hp + 1000; // deterministic fallback
  return { containerId, name: initial.name, port: hp, rconPort: rp, teamsConfigBase64: teamsB64 };
}

/* 🔎 Spieler-Extraktion (robust, beliebige Strukturen) */
function extractPlayerIds(teamsB64) {
  if (!teamsB64) return [];
  let root;
  try {
    const s = Buffer.from(String(teamsB64), "base64").toString("utf8");
    root = JSON.parse(s);
  } catch { return []; }

  const out = new Set();
  // Regex für UUIDs (mit und ohne Bindestriche)
  const UUID_ANY = /\b[0-9a-fA-F]{32}\b|\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\b/g;
  const norm = s => s.replace(/-/g,"").toLowerCase();

  const visit = (node) => {
    if (node == null) return;

    if (typeof node === "string") {
      const m = node.match(UUID_ANY);
      if (m) for (const hit of m) out.add(norm(hit));
      return;
    }
    if (typeof node === "number" || typeof node === "boolean") return;

    if (Array.isArray(node)) { for (const el of node) visit(el); return; }

    if (typeof node === "object") {
      // Prüft bekannte Schlüssel (player ID im Objekt)
      for (const key of ["id","uuid","player","playerId"]) {
        if (key in node && typeof node[key] === "string") {
          const m = node[key].match(UUID_ANY);
          if (m) for (const hit of m) out.add(norm(hit));
        }
      }
      // Rekursiver Aufruf für alle Werte im Objekt
      for (const k of Object.keys(node)) visit(node[k]);
    }
  };

  visit(root);
  return Array.from(out);
}

/* 🔒 Locks + Idempotency GC */
function gcExpiredLocks(now=Date.now()) {
  // NOTE: Diese Funktion ist jetzt leer, da die Locks nur durch den Erfolgs-/Fehler-Pfad gelöscht werden.
  // Das Entfernen von temporären Locks ist an den Request gebunden.
}

function gcIdempotency(now=Date.now()) {
  for (const [k, v] of idempCache.entries()) if (v.expiresAt <= now) idempCache.delete(k);
}

function playersInActiveMatches() {
  const set = new Set();
  for (const [pid, status] of playerLocks.entries()) {
    // Zählt Spieler, deren Lock-Wert mit "match:" beginnt (permanente Locks)
    if (typeof status === 'string' && status.startsWith(MATCH_LOCK_PREFIX)) {
      set.add(pid);
    }
  }
  return set;
}

/* Sichtbarkeits-Wait: erst antworten, wenn Match in /matches sichtbar ist */
async function waitMatchVisibleByName(name, timeoutMs = 5000, stepMs = 150) {
  const start = Date.now();
  while (Date.now() - start < timeoutMs) {
    const hit = listMatches(false).find(c => c.name === name);
    if (hit) {
      const d = fetchMatchDetails(hit.id, hit);
      // Prüft, ob Port und TeamsConfig verfügbar sind (Match ist initialisiert)
      if (d.port && d.teamsConfigBase64) return true;
    }
    await new Promise(r => setTimeout(r, stepMs));
  }
  return false;
}

/* ─── RCON (robust) ─── */
async function rconSend(host, port, pass, cmd, timeoutMs=8000) {
  return new Promise((resolve, reject) => {
    const sock = net.createConnection({ host, port });
    const pack = (id, type, payload) => {
      const msg = Buffer.from(payload || "", "utf8");
      const buf = Buffer.alloc(14 + msg.length);
      buf.writeInt32LE(10 + msg.length, 0);
      buf.writeInt32LE(id, 4);
      buf.writeInt32LE(type, 8);
      msg.copy(buf, 12);
      buf.writeInt16LE(0, 12 + msg.length);
      return buf;
    };
    const readPacket = () => new Promise((res, rej) => {
      let chunks = Buffer.alloc(0);
      const onData = c => {
        chunks = Buffer.concat([chunks, c]);
        if (chunks.length >= 4) {
          const len = chunks.readInt32LE(0) + 4;
          if (chunks.length >= len) { sock.off("data", onData); res(chunks.subarray(0, len)); }
        }
      };
      sock.on("data", onData);
      // Reduziert read timeout, um schneller auf Fehler zu reagieren, aber gibt
      // dem globalen timer (timeoutMs) genug Spielraum.
      setTimeout(()=>{ sock.off("data", onData); rej(new Error("rcon read timeout")); }, 3000);
    });
    const end = (err, data) => { try{ sock.destroy(); }catch{}; err ? reject(err) : resolve(data); };

    let timer = setTimeout(()=>end(new Error("rcon timeout")), timeoutMs);
    sock.once("connect", async () => {
      try {
        sock.write(pack(1,3,pass));
        const auth = await readPacket();
        if (auth.readInt32LE(4) === -1) return end(new Error("rcon auth failed"));
        sock.write(pack(2,2,cmd));
        sock.write(pack(3,2,"")); // terminator
        const resp = await readPacket();
        // Optionale zweite Antwort lesen (Final RCON Fix)
        try { await readPacket(); } catch (e) { /* ignore read timeout on optional packet */ }

        clearTimeout(timer);
        return end(null, resp.subarray(12, resp.length-2).toString("utf8"));
      } catch (e) { clearTimeout(timer); return end(e); }
    }).once("error", e => { clearTimeout(timer); end(e); });
  });
}

/** NEU: Wartet, bis der Container RCON-bereit ist (mit hartem Timeout). */
// NOTE: Diese Funktion war in der Version vor der Revertierung nicht in startNewContainer integriert
// und hatte die harte 120s-Grenze, die wir nun entfernen müssen.
/*
async function waitRconReady(containerId, rconPort) {
    // ... Logik ...
}
*/

/* ─── Docker run ─── */
function dockerRunArgs({ name, hostGamePort, hostRconPort, envs=[] }) {
  // FIX: Entferne --rm, um konsistentes manuelles Aufräumen zu erzwingen
  return [
    "run","-d","--pull=missing", // <-- --rm ENTFERNT
    "--label", MANAGED_LABEL,
    "--name", name,
    "-p", `${hostGamePort}:25565`,
    "-p", `${hostRconPort}:${RCON_PORT}`,
    ...envs.flatMap(e=>["-e",e]),
    GAME_IMAGE
  ];
}

/* ─── Starters ─── */

/** Startet einen neuen, leeren Container (für Pool-Befüllung). */
function startNewContainer() {
    return new Promise((resolve,reject)=>{
        const matches = runningMatchesCount();
        const total   = psManaged(false).length;

        // Allgemeine Cap-Prüfung vor dem Port-Check
        if (matches >= MATCH_CAP) return reject(new Error(`cap reached (${MATCH_CAP})`));
        if (total   >= MAX_TOTAL) return reject(new Error(`total cap reached (${MAX_TOTAL})`));

        const port = getFreePort();
        if (!port) return reject(new Error(`no free ports (${PORT_MIN}–${PORT_MAX})`));

        const hostGamePort = port, hostRconPort = port + 1000;
        const name = `warm-${uuidv4()}`; // Alle neu gestarteten sind Warm-Container

        // Dummy-Environment für Warm-Pool-Modus
        const envs=["POOL_MODE=true","MATCH_ID=dummy","MAP_ID=default","TEAMS_CONFIG_B64="];

        const child = spawn(DOCKER, dockerRunArgs({name,hostGamePort,hostRconPort,envs}), {stdio:["ignore","pipe","pipe"]});
        let out=""; let timed=false; const t=setTimeout(()=>{ timed=true; try{child.kill("SIGKILL");}catch{}; }, RUN_TIMEOUT_MS);
        child.stdout.on("data",d=>out+=d.toString());
        child.on("close", async (code)=>{
            clearTimeout(t);
            if (timed || code!==0) {
              // Beim Fehlerfall Container entfernen, falls er kurz gestartet ist
              const id = (out.trim().split("\n").pop()||"").trim();
              if (id) rmContainer(id);
              return reject(new Error(`docker run (warm) failed`));
            }
            const id = (out.trim().split("\n").pop()||"").trim(); if (!id) return reject(new Error("no container id"));

            // Warte auf TCP-Readiness
            const ready = await waitPort(GAME_PING_HOST, hostGamePort, READY_TIMEOUT_MS);
            if (!ready){ rmContainer(id); return reject(new Error(`warm not ready`)); }

            resolve({ id, name, port: hostGamePort, rconPort: hostRconPort });
        });
    });
}


/* ─── Warm → Match (fast) ─── */
async function assignWarmToMatch({ mapId, teamsConfigBase64 }){
  if (runningMatchesCount() >= MATCH_CAP) return null;
  const warm = listWarm(false);

  // WICHTIG: Wenn der Pool leer ist, geben wir null zurück.
  // Der Aufrufer wird dann warten oder fehlschlagen.
  if (!warm.length) return null;

  const first = warm[0];
  const details = fetchMatchDetails(first.id, first);
  if (!details.port || !details.rconPort) return null;

  const matchId = uuidv4();
  const trySend = async () => rconSend(
    GAME_PING_HOST, details.rconPort, RCON_PASS,
    `assignmatch ${matchId} ${mapId} ${teamsConfigBase64}`, 6000
  );

  // KRITISCHER FIX: Polling-Schleife für RCON-Readiness
  let ok=false, lastErr=null;
  const RCON_RETRY_STEP_MS = 500;

  // Endlose Retry-Schleife, bis RCON klappt
  for (;;) {
      try {
          await trySend();
          ok = true;
          break;
      } catch (e) {
          lastErr = e;
          // Wenn der Fehler AUTH FAILED ist, ist der Container unbrauchbar -> Abbruch
          if (e.message.includes("rcon auth failed")) {
              console.error(`[RCON] Permanent auth failed for container ${first.name}. Aborting.`);
              // Container entfernen, da er ein Hard-Fail ist
              rmContainer(first.id);
              return null;
          }
          // Wenn der Fehler "rcon timeout" oder "Connection reset" ist, warten wir und versuchen es erneut.
          await new Promise(r => setTimeout(r, RCON_RETRY_STEP_MS));
      }
  }

  if (!ok) {
      // Sollte nur bei Fehlern passieren, die nicht 'auth failed' waren, aber nach der Schleife immer noch da sind.
      // Der Container ist bereits entfernt, wenn es auth failed war.
      console.error(`[RCON] warm assign failed after polling: ${lastErr?.message||"unknown"}.`);
      return null;
  }

  try { sh(DOCKER, ["rename", first.name, `match-${matchId}`]); } catch {}
  warmAssignedCache.set(first.id, teamsConfigBase64);
  ensureWarmPool(); // top-up in background

  return { matchId, name:`match-${matchId}`, gamePort: details.port, containerId:first.id, teamsConfigBase64 };
}

/** ─── Fix: Allgemeine Aufräumlogik, die sofort ausgeführt werden muss ─── */
function runImmediateCleanup() {
  const allContainers = psManaged(true);
  const allRunningIds = new Set(allContainers.filter(c => /^Up\s/.test(c.status||"")).map(c => c.id));
  let locksRemoved = 0;
  let containersRemoved = 0;

  // 1. Locks aufräumen (entfernt Locks von abgestürzten Containern)
  for (const [pid, status] of playerLocks.entries()) {
      if (typeof status === 'string' && status.startsWith(MATCH_LOCK_PREFIX)) {
          const containerId = status.substring(MATCH_LOCK_PREFIX.length);
          // Wenn der Container NICHT in der Liste der aktuell laufenden IDs ist, war er abgestürzt/beendet.
          if (!allRunningIds.has(containerId)) {
              playerLocks.delete(pid);
              locksRemoved++;
          }
      }
  }

  // 2. Container aufräumen (entfernt BEENDETE Container und Stale Matches)
  for (const c of allContainers) {
    const isUp = allRunningIds.has(c.id);

    // a) Entfernt BEENDETE Container (Status != "Up ")
    if (!isUp) {
        if (rmContainer(c.id)) containersRemoved++;
        continue;
    }

    // b) Entfernt laufende Matches ohne Ports (Stale Check - aggressiv)
    // Wenn das Match noch "Up" ist, aber die Ports nicht mehr funktionieren, muss es weg.
    if (c.name?.startsWith("match-")) {
      const det = fetchMatchDetails(c.id, c);
      const bothClosed = (!det.port || !det.rconPort);
      if (bothClosed) {
          if (rmContainer(c.id)) containersRemoved++;
          continue;
      }
    }
  }

  if (containersRemoved > 0 || locksRemoved > 0) {
      console.log(`[CLEANUP] Removed ${containersRemoved} exited/stale containers and cleaned up ${locksRemoved} expired player locks.`);
  }
}

/* ─── Pool + Cleanup ─── */
let poolTickRunning = false;
async function ensureWarmPool(){
  if (poolTickRunning) return; poolTickRunning = true;
  try{
    // Sofortiges Aufräumen von Exited Containern, um Ports freizugeben
    runImmediateCleanup();

    const matches = runningMatchesCount();
    const all     = psManaged(false).length;
    const warmArr = listWarm(false);
    const targetWarm = Math.max(0, Math.min(WARM_CAP, MAX_TOTAL - matches));

    if (warmArr.length > targetWarm) {
      for (const c of warmArr.slice(0, warmArr.length - targetWarm)) rmContainer(c.id);
      return;
    }
    const canCreate = Math.max(0, MAX_TOTAL - all);
    const toCreate = Math.min(Math.max(0, targetWarm - warmArr.length), canCreate);
    for (let i=0; i<toCreate; i++) {
      try { await startNewContainer(); } catch(e){ console.error("[pool] warm start failed:", e.message); break; }
    }
  } finally { poolTickRunning = false; }
}

/* Periodic maintenance */
// Führt den sofortigen Cleanup nun alle 2s aus (sehr schnell)
setInterval(runImmediateCleanup, 2000);
setInterval(ensureWarmPool, 3000);
// NOTE: Die manuelle gcExpiredLocks-Funktion ist nun redundant, da die Locks
// nur durch den Erfolgspfad und die Container-Hygiene gelöscht werden.
// Ich behalte die Aufrufe bei, da sie keinen Schaden anrichten.
setInterval(gcExpiredLocks, 5_000);
setInterval(gcIdempotency, 5_000);       // GC Idempotency
ensureWarmPool(); // Initialer Pool-Aufbau

/* ─── HTTP ─── */
const server = http.createServer((req,res)=>{
  res.setHeader("Access-Control-Allow-Origin","*");
  res.setHeader("Access-Control-Allow-Headers","content-type,x-idempotency-key");
  if (req.method==="OPTIONS"){ res.writeHead(204); return res.end(); }
  const send = (code,obj)=>{ res.writeHead(code,{"content-type":"application/json"}); res.end(JSON.stringify(obj)); };

  if (req.method==="GET" && req.url==="/health"){
    const usedSet = usedHostPorts();
    const used = Array.from(usedSet).filter(p => p>=PORT_MIN && p<=PORT_MAX).sort((a,b)=>a-b);
    const free=[]; for(let p=PORT_MIN;p<=PORT_MAX;p++) if(!usedSet.has(p)) free.push(p);
    return send(200,{
      ok:true,
      warm:listWarm(false).length,
      matches:runningMatchesCount(),
      total:psManaged(false).length,
      targetWarm: Math.max(0, Math.min(WARM_CAP, MAX_TOTAL - runningMatchesCount())),
      ports: { used, free, range:[PORT_MIN,PORT_MAX] },
      caps: { total:MAX_TOTAL, matches:MATCH_CAP, warmMax:WARM_CAP }
    });
  }

  if (req.method==="GET" && req.url==="/matches"){
    gcExpiredLocks();
    const matches = listMatches(false).map(m => fetchMatchDetails(m.id, m))
      .filter(d => d.port && d.name && d.teamsConfigBase64);
    const pendingPlayers = Array.from(playerLocks.keys()).filter(pid => playerLocks.get(pid) === true); // boolean Lock = pending
    return send(200, { ok:true, matches, pendingPlayers });
  }

  if (req.method==="GET" && req.url==="/in-use"){
    gcExpiredLocks();
    const active = Array.from(playersInActiveMatches());
    const pending = Array.from(playerLocks.keys()).filter(pid => playerLocks.get(pid) === true); // boolean Lock = pending
    const inUse = Array.from(new Set([...pending, ...active]));
    return send(200, { ok:true, inUse, pending, active });
  }

  if (req.method==="POST" && req.url==="/create-match"){
    let body=""; req.on("data",c=>body+=c);
    req.on("end", async ()=>{
      // Temporärer Speicher für die Spieler-IDs, falls Parsing fehlschlägt
      let candidateIds = [];
      let parsed = {};

      try{
        parsed = JSON.parse(body||"{}");
        const { mapId, teamsConfigBase64, players } = parsed;
        if(!mapId) throw new Error("mapId missing");
        if(!teamsConfigBase64 && !players) throw new Error("teamsConfigBase64 or players missing");

        // 🧾 Idempotency-Key (Header bevorzugt, sonst Body)
        const idempKey = (req.headers["x-idempotency-key"] ? String(req.headers["x-idempotency-key"]) :
                           (typeof parsed.idempotencyKey === "string" ? parsed.idempotencyKey : null));
        if (idempKey && idempCache.has(idempKey)) {
          const cached = idempCache.get(idempKey);
          return send(200, cached.responseJson);
        }

        // 🔒 Sofort-Lock (vor dem ersten await)
        // Setze den Lock auf 'true', damit der Spieler sofort permanent für diesen Request gesperrt ist.
        gcExpiredLocks();
        const candidateIdsRaw = (Array.isArray(players) && players.length ? players.map(String) : extractPlayerIds(teamsConfigBase64));
        candidateIds = Array.from(new Set(candidateIdsRaw)).filter(Boolean);
        if (candidateIds.length === 0) {
          return send(409, { ok:false, error:"no players to lock (provide 'players' or teamsConfig with player IDs)" });
        }

        // KRITISCHE PRÜFUNG: Konflikt mit aktiven Matches (permanente Locks) ODER Locks in Queue (temporäre Locks)
        const active = playersInActiveMatches();
        const conflicts = candidateIds.filter(pid => {
            const currentLock = playerLocks.get(pid);
            // Wenn der Lock existiert (unabhängig vom Wert), gibt es einen Konflikt
            return currentLock !== undefined || active.has(pid);
        });

        if (conflicts.length) return send(409, { ok:false, error:"players already in queue/match", conflicts });

        // Setze den temporären, unbegrenzten Lock (Wird am Ende des Requests gelöst)
        for (const pid of candidateIds) playerLocks.set(pid, true);

        // Match bauen + Sichtbarkeit abwarten
        let resp = null, success = false, matchName = null, containerId = null;

        const POOL_WAIT_STEP_MS = 500;  // Prüfe alle 0.5 Sekunden

        console.log("[POOL] Starting assignment attempt (will wait indefinitely until assigned or cap reached)...");

        // Polling-Schleife für Zuweisung (Unbegrenzte Wartezeit bis zum Cap)
        for (;;) {

            // 1. VERSUCH: Warm Pool Zuweisung
            resp = await assignWarmToMatch({ mapId, teamsConfigBase64 });

            // Wenn erfolgreich, breche Schleife ab
            if (resp) break;

            // Wenn Cap erreicht ist, sofort fehlschlagen (damit wir nicht ewig warten)
            const matches = runningMatchesCount();
            if (matches >= MATCH_CAP) {
                throw new Error("cap reached or pool failed to supply a container");
            }

            // Warten und Pool-Mechanismus Zeit geben
            await new Promise(r => setTimeout(r, POOL_WAIT_STEP_MS));
        }

        success = !!resp;
        matchName = resp?.name || null;
        containerId = resp?.containerId || null;

        // HIER ist der einzige Punkt, an dem wir fehlschlagen, wenn der Pool-Mechanismus
        // nicht liefern konnte.
        if (!success) {
            throw new Error("Unknown error: Pool polling loop exited without success");
        }

        if (success && matchName) {
            await waitMatchVisibleByName(matchName, 5000, 150); // schließt Restfenster
        }

        // --- Success Path ---
        // Wenn wir hier sind, war der Start erfolgreich (`success` ist true)
        const lockValue = `${MATCH_LOCK_PREFIX}${containerId}`;
        for (const pid of candidateIds) playerLocks.set(pid, lockValue);


        const payload = { ok:true, ...resp, fromPool: true }; // Immer fromPool: true, da Cold Start entfernt wurde
        if (idempKey) idempCache.set(idempKey, { expiresAt: Date.now() + IDEMP_TTL_MS, responseJson: payload });
        return send(201, payload);

      }catch(e){
        // Wenn wir hier landen, war der Start nicht erfolgreich, daher Lock entfernen
        // NOTE: Wir verwenden `parsed.players` und `parsed.teamsConfigBase64`, da `candidateIds` außerhalb
        // des try-Blocks deklariert ist, aber hier neu berechnet werden muss, falls der Fehler
        // im Polling-Block auftrat.
        const candidateIdsRaw = (Array.isArray(parsed.players) && parsed.players.length ? parsed.players.map(String) : extractPlayerIds(parsed.teamsConfigBase64));
        const finalCandidateIds = Array.from(new Set(candidateIdsRaw)).filter(Boolean);
        // LÖSCHE NUR DIE VOM FEHLGESCHLAGENEN REQUEST GESETZTEN LOCKS
        for (const pid of finalCandidateIds) playerLocks.delete(pid);

        // Äußerer Catch, falls etwas beim Parsen, Initial-Lock oder Timeout schief geht
        const code = /cap reached|no free ports|Timeout|Unknown error|RCON readiness check failed/.test(e.message) ? 429 : 400;
        return send(code,{ ok:false, error:e.message });
      }
    });
    return;
  }

  if (req.method==="POST" && req.url==="/end-match"){
    let body=""; req.on("data",c=>body+=c);
    req.on("end", ()=>{
      try{
        const { name, port } = JSON.parse(body||"{}");

        // 1. Container ID finden
        const foundContainer = psManaged(true).find(c => c.name === name || (c.ports||"").includes(`:${Number(port)}->25565`));
        if (!foundContainer) return send(404, { ok:false, error: "Match not found" });

        const containerId = foundContainer.id;

        // 2. Spieler freigeben (aus Cache/Match-ID Lock)
        // Finden Sie alle Spieler, die mit dieser Container-ID gelockt sind.
        const lockValue = `${MATCH_LOCK_PREFIX}${containerId}`;
        const playersToUnlock = [];
        for (const [pid, status] of playerLocks.entries()) {
            if (status === lockValue) {
                playersToUnlock.push(pid);
            }
        }

        for (const pid of playersToUnlock) {
            playerLocks.delete(pid);
        }

        // 3. Container entfernen
        const ok = rmContainer(containerId);

        // KRITISCHER FIX: Sofortiges Aufräumen von Exited-Containern und Locks
        runImmediateCleanup();

        ensureWarmPool();
        return send(ok?200:500,{ ok: ok, unlockedPlayers: playersToUnlock.length });
      } catch(e){ return send(400,{ ok:false, error:e.message }); }
    });
    return;
  }

  if (req.method==="POST" && req.url==="/gc"){
    // Dieser Endpunkt verwendet jetzt den aggressiven Cleanup
    runImmediateCleanup();
    ensureWarmPool();
    return send(200, { ok:true });
  }

  return send(404,{ ok:false, error:"Not Found" });
});

server.listen(PORT, HOST, ()=>{
  console.log(`[orchestrator] listening on ${HOST}:${PORT} | caps: total=${MAX_TOTAL}, matches=${MATCH_CAP}, warm<=${WARM_CAP} | ports=${PORT_MIN}-${PORT_MAX}`);
});


