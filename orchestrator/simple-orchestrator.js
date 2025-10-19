// warm-pool-orchestrator.js — Lean, Priority Warm Pool, Fixed Limits & /matches Endpoint
"use strict";

const http = require("http");
const net = require("net");
const { spawnSync, spawn } = require("child_process");
const { v4: uuidv4 } = require("uuid");

/* ───── Config & Cache ───── */
const PORT = Number(process.env.ORCH_PORT || 4000);
const HOST = process.env.ORCH_HOST || "0.0.0.0";
const DOCKER = process.env.DOCKER_BIN || "/usr/bin/docker";

// Feste Limits
const PORT_MIN = 25566;
const PORT_MAX = 25569;
const MAX_TOTAL = 4;
const MATCH_CAP = 4;
const WARM_CAP  = 2;

// KRITISCHER FIX: Trennung der Label-Schlüssel
const MANAGED_LABEL = "projectm.managed=true"; // Hauptfilter für ALLE Container
const POOL_STATE_LABEL = "projectm.pool=true"; // Spezifischer Flag für Warm-Container

const GAME_IMAGE = process.env.GAME_IMAGE || "realpingi/game:ready";

const RCON_PORT = Number(process.env.RCON_PORT || 25575);
// PASSWORT-FIX: Auf changeme zurückgesetzt.
const RCON_PASS = process.env.RCON_PASS || "changeme";

const RUN_TIMEOUT_MS   = 10_000;
const READY_TIMEOUT_MS = 30_000;
const TCP_TIMEOUT_MS   = 800;
const EAGER_FAIL_RM    = true;
// NETZWERK-FIX: Für Host-Zugriff
const GAME_PING_HOST   = process.env.GAME_PING_HOST || "127.0.0.1";

// NEU: Lokaler Cache für Warm-Assigned Matches
// Speichert: { containerId: teamsConfigBase64 }
const warmAssignedCache = new Map();

/* ───── helpers ───── */

function sh(cmd, args, opts = {}) {
  const r = spawnSync(cmd, args, { encoding: "utf8", ...opts });
  if (r.error) throw r.error;
  return { code: r.status ?? 0, out: (r.stdout || "").trim(), err: (r.stderr || "").trim() };
}

function listGameContainers(all = false) {
  const args = ["ps","--format","{{.ID}}|{{.Names}}|{{.Ports}}","--filter",`label=${MANAGED_LABEL}`];
  if (all) args.splice(1,0,"-a");
  const { code, out } = sh(DOCKER, args);
  if (code !== 0 || !out) return [];
  return out.split("\n").filter(Boolean).map(l => {
    const [id,name,ports]=l.split("|"); return {id,name,ports:ports||""};
  });
}
const listWarm    = (all=false)=> listGameContainers(all).filter(c => c?.name?.startsWith("warm-"));
const listMatches = (all=false)=> listGameContainers(all).filter(c => c?.name?.startsWith("match-"));
const runningMatchesCount = ()=> listMatches(false).length;

/** NEU: Liest MatchDetails und ergänzt fehlende TeamsConfigB64 aus Cache */
function fetchMatchDetails(containerId, initialMatchData = {}) {
  let hp = null, rp = null, teamsConfigBase64 = null;

  try {
    const { code, out } = sh(DOCKER, ["inspect", containerId]);
    if (code === 0 && out) {
      const js = JSON.parse(out)[0] || {};

      // 1. Ports ermitteln
      const gp = js?.NetworkSettings?.Ports?.["25565/tcp"];
      // Wir suchen den Host-Port, der dem Container-RCON-Port zugeordnet ist (z.B. 25575)
      const rk = `${RCON_PORT}/tcp`;
      const rpMap = js?.NetworkSettings?.Ports?.[rk];

      // Game Host Port
      hp = Number(Array.isArray(gp) && gp[0]?.HostPort);
      // RCON Host Port (wird automatisch von Docker zugewiesen)
      rp = Number(Array.isArray(rpMap) && rpMap[0]?.HostPort);

      // 2. TeamsConfig aus Umgebungsvariablen ermitteln (Nur Cold Start)
      const envs = js?.Config?.Env || [];
      const teamsEnv = envs.find(e => e.startsWith("TEAMS_CONFIG_B64="));
      if (teamsEnv) {
        teamsConfigBase64 = teamsEnv.substring("TEAMS_CONFIG_B64=".length);
      }
    }
  } catch(e) {
    console.error(`[inspect] Failed to inspect container ${containerId}: ${e.message}`);
  }

  // 3. NEU: Ergänzung aus lokalem Cache, falls Umgebungsvariable leer ist
  if (!teamsConfigBase64 && warmAssignedCache.has(containerId)) {
      teamsConfigBase64 = warmAssignedCache.get(containerId);
  }

  // Fallback für Ports (falls inspect fehlschlägt, aber 'docker ps' Ports bekannt sind)
  if (!hp && initialMatchData.ports) {
    const m = (initialMatchData.ports || "").match(/:(\d+)->25565\/tcp/);
    if (m) hp = Number(m[1]);
  }

  // RCON-Port-FIX-FALLBACK: Wenn Docker Inspect den RCON-Port nicht meldet,
  // gehen wir zur +1000-Konvention zurück, da die Startlogik dies verwendet.
  if (!rp && hp) rp = hp + 1000;

  return {
    containerId,
    name: initialMatchData.name,
    port: hp,
    rconPort: rp,
    teamsConfigBase64: teamsConfigBase64
  };
}

function usedHostPorts() {
  const { code, out } = sh(DOCKER, ["ps","-a","--format","{{.Ports}}"]);
  if (code !== 0 || !out) return new Set();
  const s = new Set();
  for (const line of out.split("\n")) {
    if (!line) continue;
    // Sucht nur nach Game-Ports (25565)
    const m = line.match(/(?::|0\.0\.0\.0:|\[::\]:)(\d+)->25565\/tcp/g);
    if (m) {
      for (const hit of m) {
        const num = Number((hit.match(/(\d+)->25565\/tcp/)||[])[1]);
        if (num) s.add(num);
      }
    }
  }
  return s;
}

function getFreePort(){
  const used = usedHostPorts();
  const availablePorts = [];

  for (let p = PORT_MIN; p <= PORT_MAX; p++) {
      if (!used.has(p)) {
          availablePorts.push(p);
      }
  }

  // HINWEIS: WARUM SIND PORTS BELEGT?
  if (availablePorts.length === 0 && used.size > 0) {
      const labeledCount = listGameContainers(false).length;
      if (labeledCount < MAX_TOTAL) {
          console.warn(`[PORT WARNING] All ports (${PORT_MIN}-${PORT_MAX}) are used, but only ${labeledCount} labeled containers are running. Ports are blocked by non-managed containers.`);
      }
  }

  return availablePorts.length > 0 ? availablePorts[0] : null;
}

function rmContainer(idOrName){
  // NEU: Cache löschen, wenn Container entfernt wird
  const containerId = (listGameContainers(true).find(c => c.id === idOrName || c.name === idOrName) || {}).id;
  if (containerId) warmAssignedCache.delete(containerId);

  try{ sh(DOCKER,["rm","-f",idOrName]); return true; }catch{ return false; }
}

/* Readiness: schlank (TCP) */
function tcp(host,port,ms=TCP_TIMEOUT_MS){
  return new Promise(res=>{
    const s=net.createConnection({host,port});
    let done=false; const finish=(ok)=>{ if(!done){ done=true; try{s.destroy();}catch{}; res(ok);} };
    s.setTimeout(ms,()=>finish(false));
    s.on("error",()=>finish(false));
    s.on("connect",()=>finish(true));
  });
}
async function waitReady(port){
  const start=Date.now();
  while (Date.now()-start < READY_TIMEOUT_MS) {
    if (await tcp(GAME_PING_HOST,port,TCP_TIMEOUT_MS)) return true;
    await new Promise(r=>setTimeout(r,200));
  }
  return false;
}

/* RCON (kompakt) */
// Das Timeout wurde auf 5000ms erhöht
async function rconSend(host, port, pass, cmd, timeoutMs = 8000) {
  return new Promise((resolve, reject) => {
    const net = require("net");
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

    const readPacket = () =>
      new Promise((res, rej) => {
        let chunks = Buffer.alloc(0);
        sock.on("data", (c) => {
          chunks = Buffer.concat([chunks, c]);
          if (chunks.length >= 4) {
            const len = chunks.readInt32LE(0) + 4;
            if (chunks.length >= len) {
              sock.removeAllListeners("data");
              res(chunks.subarray(0, len));
            }
          }
        });
        setTimeout(() => rej(new Error("rcon read timeout")), 3000);
      });

    sock.once("connect", async () => {
      try {
        // AUTH
        sock.write(pack(1, 3, pass));
        const authResp = await readPacket();
        const id = authResp.readInt32LE(4);
        if (id === -1) throw new Error("rcon auth failed");

        // COMMAND
        sock.write(pack(2, 2, cmd));
        sock.write(pack(3, 2, "")); // terminator
        const resp = await readPacket();
        const msg = resp.subarray(12, resp.length - 2).toString("utf8");
        sock.end();
        resolve(msg);
      } catch (e) {
        try { sock.destroy(); } catch {}
        reject(e);
      }
    });

    sock.once("error", reject);
  });
}


/* Docker run: Alle Game-Container erhalten das 'projectm.managed=true' Label */
function dockerRunArgs({ name, hostGamePort, hostRconPort, envs = [], poolMode = false }) {
  const args = [
    "run","-d","--rm","--pull=missing",
    "--label", MANAGED_LABEL, // <--- KRITISCHES MANAGED LABEL
    ...(poolMode ? ["--label", POOL_STATE_LABEL] : []), // <--- SAUBERER POOL STATE LABEL
    "--name", name,
    "-p", `${hostGamePort}:25565`, // Game Port Mapping
    "-p", `${hostRconPort}:${RCON_PORT}`, // RCON Port Mapping
    ...envs.flatMap(e => ["-e", e]),
    GAME_IMAGE
  ];
  return args;
}

/* Startfunktionen */
function startWarmContainer(){
  return new Promise((resolve,reject)=>{
    const port = getFreePort();
    if (!port) return reject(new Error(`no free ports (${PORT_MIN}–${PORT_MAX} only)`));

    // REVERT-FIX: RCON Host Port ist nun wieder +1000, um Kollisionen zu vermeiden
    const hostGamePort = port;
    const hostRconPort = port + 1000;

    const name=`warm-${uuidv4()}`;
    const envs=["POOL_MODE=true","MATCH_ID=dummy","MAP_ID=default","TEAMS_CONFIG_B64="];

    const args = dockerRunArgs({name,hostGamePort,hostRconPort,envs,poolMode:true});
    const child=spawn(DOCKER, args, {stdio:["ignore","pipe","pipe"]});
    let out="", err=""; let timed=false;
    const t=setTimeout(()=>{timed=true; try{child.kill("SIGKILL");}catch{};}, RUN_TIMEOUT_MS);
    child.stdout.on("data",d=>out+=d.toString());
    child.stderr.on("data",d=>err+=d.toString());
    child.on("close", async (code)=>{
      clearTimeout(t);
      if (timed || code!==0) {
        return reject(new Error(`docker run (warm) failed${err?`: ${err.trim()}`:""}`));
      }
      const id=(out.trim().split("\n").pop()||"").trim();
      if (!id) return reject(new Error("no container id (warm)"));

      // WICHTIG: Warte auf TCP-Readiness
      const ready = await waitReady(hostGamePort);
      if (!ready){ if(EAGER_FAIL_RM) rmContainer(id); return reject(new Error("warm not ready")); }

      resolve({ id, name, port: hostGamePort, rconPort: hostRconPort });
    });
  });
}

function startMatchCold({ mapId, teamsConfigBase64 }){
  return new Promise((resolve,reject)=>{
    const matches = runningMatchesCount();
    const totalLabeled = listGameContainers(false).length;

    if (matches >= MATCH_CAP) return reject(new Error(`match cap reached (${MATCH_CAP})`));
    if (totalLabeled >= MAX_TOTAL) return reject(new Error(`total cap reached (${MAX_TOTAL})`));

    const port = getFreePort();
    if (!port) return reject(new Error(`no free ports (${PORT_MIN}–${PORT_MAX} only). Ports are likely blocked by unmanaged containers.`));

    // REVERT-FIX: RCON Host Port ist nun wieder +1000
    const hostGamePort = port;
    const hostRconPort = port + 1000;

    const matchId=uuidv4(), name=`match-${matchId}`;
    const envs=[`MATCH_ID=${matchId}`,`MAP_ID=${mapId}`,`TEAMS_CONFIG_B64=${teamsConfigBase64}`];

    const args = dockerRunArgs({name,hostGamePort,hostRconPort,envs});
    const child=spawn(DOCKER, args, {stdio:["ignore","pipe","pipe"]});
    let out="", err="", timed=false;
    const t=setTimeout(()=>{timed=true; try{child.kill("SIGKILL");}catch{};}, RUN_TIMEOUT_MS);
    child.stdout.on("data",d=>out+=d.toString());
    child.stderr.on("data",d=>err+=d.toString());
    child.on("close", async (code)=>{
      clearTimeout(t);
      if (timed || code!==0) {
        return reject(new Error(`docker run (match) failed${err?`: ${err.trim()}`:""}`));
      }
      const id=(out.trim().split("\n").pop()||"").trim();
      if (!id) return reject(new Error("no container id"));

      // WICHTIG: Warte auf TCP-Readiness
      const ready = await waitReady(hostGamePort);
      if (!ready){ if(EAGER_FAIL_RM) rmContainer(id); return reject(new Error("match not ready")); }

      resolve({ matchId, name, gamePort: hostGamePort, containerId: id });
    });
  });
}

/* Warm → Match (verbraucht keinen neuen Port) */
async function assignWarmToMatch({ mapId, teamsConfigBase64 }){
  if (runningMatchesCount() >= MATCH_CAP) return null;

  const warm = listWarm(false);
  if (!warm.length) return null;

  const first = warm[0];

  // Ports und RCON Port ermitteln
  const details = fetchMatchDetails(first.id, first);
  if (!details.port) return null;

  const matchId = uuidv4();

  // RCON-Befehl zum Umkonfigurieren des Warm-Containers
  try {
    await rconSend(GAME_PING_HOST, details.rconPort, RCON_PASS, `assignmatch ${matchId} ${mapId} ${teamsConfigBase64}`, 5000);
    console.log(`[RCON] Successfully assigned matchId ${matchId}.`);
  } catch(e) {
    console.error(`[RCON ERROR] Failed to assign match to warm container ${first.name}. Host: ${GAME_PING_HOST}, Port: ${details.rconPort}. Error: ${e.message}`);
    return null;
  }

  // Container umbenennen
  try { sh(DOCKER, ["rename", first.name, `match-${matchId}`]); } catch {}

  // WICHTIG: Speichert die TeamsConfig lokal, da sie nicht als Env-Var gespeichert wird.
  warmAssignedCache.set(first.id, teamsConfigBase64);

  ensureWarmPool(); // non-blocking

  return {
    matchId,
    name: `match-${matchId}`,
    gamePort: details.port,
    containerId: first.id,
    teamsConfigBase64
  };
}

/* ───── Pool-Logik (unverändert) ───── */
let poolTickRunning = false;
async function ensureWarmPool(){
  if (poolTickRunning) return;
  poolTickRunning = true;
  try{
    const matches = runningMatchesCount();
    const all     = listGameContainers(false).length;
    const warmArr = listWarm(false);
    const targetWarm = Math.max(0, Math.min(WARM_CAP, MAX_TOTAL - matches));

    if (warmArr.length > targetWarm){
      const excess = warmArr.slice(0, warmArr.length - targetWarm);
      for (const c of excess) rmContainer(c.id);
      return;
    }

    const canCreate = Math.max(0, MAX_TOTAL - all);
    const need = Math.max(0, targetWarm - warmArr.length);
    const toCreate = Math.min(need, canCreate);

    for (let i=0; i<toCreate; i++){
      try{ await startWarmContainer(); }catch(e){ console.error("[pool] warm start failed:", e.message); break; }
    }
  } finally {
    poolTickRunning = false;
  }
}

setInterval(ensureWarmPool, 3500);
ensureWarmPool();

/* ───── HTTP ───── */
const server = http.createServer((req,res)=>{
  res.setHeader("Access-Control-Allow-Origin","*");
  res.setHeader("Access-Control-Allow-Headers","content-type");
  if (req.method==="OPTIONS"){ res.writeHead(204); return res.end(); }

  const send = (code,obj)=>{ res.writeHead(code,{"content-type":"application/json"}); res.end(JSON.stringify(obj)); };

  if (req.method==="GET" && req.url==="/health"){
    const usedSet = usedHostPorts();
    const used = Array.from(usedSet).filter(p => p >= PORT_MIN && p <= PORT_MAX).sort((a,b)=>a-b);
    const free=[]; for(let p=PORT_MIN;p<=PORT_MAX;p++) if(!usedSet.has(p)) free.push(p);
    return send(200,{
      ok:true,
      warm:listWarm(false).length,
      matches:runningMatchesCount(),
      total:listGameContainers(false).length,
      targetWarm: Math.max(0, Math.min(WARM_CAP, MAX_TOTAL - runningMatchesCount())),
      ports: { used, free, range:[PORT_MIN,PORT_MAX] },
      caps: { total:MAX_TOTAL, matches:MATCH_CAP, warmMax:WARM_CAP }
    });
  }

  // NEUER ENDPUNKT: /matches
  if (req.method==="GET" && req.url==="/matches"){
    const matches = listMatches(false);
    const fetchPromises = matches.map(m => {
        return fetchMatchDetails(m.id, m);
    });

    // Asynchron alle Details der Matches sammeln
    Promise.all(fetchPromises)
    .then(detailsArray => {
      // Filtert alle Matches, die erfolgreich einen Port und eine TeamsConfig haben
      const matchInfos = detailsArray.filter(details =>
         details.port && details.name && details.teamsConfigBase64
      ).map(details => ({
           containerId: details.containerId,
           name: details.name,
           port: details.port,
           teamsConfigBase64: details.teamsConfigBase64,
      }));

      return send(200, { ok: true, matches: matchInfos });
    })
    .catch(e => {
      console.error("[matches] Error fetching details:", e.message);
      return send(500, { ok: false, error: "Failed to fetch match details" });
    });

    return;
  }

  if (req.method==="POST" && req.url==="/create-match"){
    let body=""; req.on("data",c=>body+=c);
    req.on("end", async ()=>{
      try{
        const { mapId, teamsConfigBase64 } = JSON.parse(body||"{}");
        if(!mapId) throw new Error("mapId missing");
        if(!teamsConfigBase64) throw new Error("teamsConfigBase64 missing");

        // 1) Warm → Match (STANDARD-FALL: Priorität)
        const fromPool = await assignWarmToMatch({ mapId, teamsConfigBase64 });
        if (fromPool) return send(201,{ ok:true, ...fromPool, fromPool:true });

        // 2) Cold Start (FALLBACK/NOTFALL: nur wenn Warm Pool leer)
        const cold = await startMatchCold({ mapId, teamsConfigBase64 });
        return send(201,{ ok:true, ...cold, fromPool:false });

      }catch(e){
        const code = e.message.includes("cap reached") || e.message.includes("no free ports") ? 429 : 400;
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

        // rmContainer löscht jetzt auch den Cache-Eintrag
        const ok = name ? rmContainer(name) :
          (port ? (rmContainer((listGameContainers(true).find(c=>(c.ports||"").includes(`:${Number(port)}->25565`))||{}).id||"")) : false);

        ensureWarmPool();
        return send(ok?200:404,{ ok });
      } catch(e){ return send(400,{ ok:false, error:e.message }); }
    });
    return;
  }

  return send(404,{ ok:false, error:"Not Found" });
});

server.listen(PORT, HOST, ()=>{
  console.log(`[orchestrator] listening on ${HOST}:${PORT} | caps: total=4, matches=4, warm<=2 | ports=${PORT_MIN}-${PORT_MAX}`);
});
