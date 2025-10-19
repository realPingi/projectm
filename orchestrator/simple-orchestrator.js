// warm-pool-orchestrator.js — lean pool, fast warm-assign, auto-cleanup
"use strict";

const http = require("http");
const net = require("net");
const { spawnSync, spawn } = require("child_process");
const { v4: uuidv4 } = require("uuid");

/* ─── Config ─── */
const PORT  = Number(process.env.ORCH_PORT || 4000);
const HOST  = process.env.ORCH_HOST || "0.0.0.0";
const DOCKER = process.env.DOCKER_BIN || "/usr/bin/docker";

const PORT_MIN = 25566, PORT_MAX = 25569;
const MAX_TOTAL = 4, MATCH_CAP = 4, WARM_CAP = 2;

const MANAGED_LABEL = "projectm.managed=true";
const GAME_IMAGE = process.env.GAME_IMAGE || "realpingi/game:ready";

const RCON_PORT = Number(process.env.RCON_PORT || 25575);
const RCON_PASS = process.env.RCON_PASS || "changeme";

const RUN_TIMEOUT_MS   = 10_000;
const READY_TIMEOUT_MS = 20_000;
const TCP_TIMEOUT_MS   = 600;
const GAME_PING_HOST   = process.env.GAME_PING_HOST || "127.0.0.1";

/* ─── State ─── */
// Merkt sich Teams-B64 für warm->match Assigns (da Env nachträglich fehlt)
const warmAssignedCache = new Map();

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
      const envs = js?.Config?.Env || [];
      const t = envs.find(e=>e.startsWith("TEAMS_CONFIG_B64="));
      if (t) teamsB64 = t.substring("TEAMS_CONFIG_B64=".length);
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
        clearTimeout(timer);
        return end(null, resp.subarray(12, resp.length-2).toString("utf8"));
      } catch (e) { clearTimeout(timer); return end(e); }
    }).once("error", e => { clearTimeout(timer); end(e); });
  });
}

/* ─── Docker run ─── */
function dockerRunArgs({ name, hostGamePort, hostRconPort, envs=[], poolMode=false }) {
  return [
    "run","-d","--rm","--pull=missing",
    "--label", MANAGED_LABEL,
    "--name", name,
    "-p", `${hostGamePort}:25565`,
    "-p", `${hostRconPort}:${RCON_PORT}`,
    ...envs.flatMap(e=>["-e",e]),
    GAME_IMAGE
  ];
}

/* ─── Starters ─── */
function startWarmContainer(){
  return new Promise((resolve,reject)=>{
    const port = getFreePort();
    if (!port) return reject(new Error(`no free ports (${PORT_MIN}–${PORT_MAX})`));
    const hostGamePort = port, hostRconPort = port + 1000;
    const name = `warm-${uuidv4()}`;
    const envs = ["POOL_MODE=true","MATCH_ID=dummy","MAP_ID=default","TEAMS_CONFIG_B64="];
    const child = spawn(DOCKER, dockerRunArgs({name,hostGamePort,hostRconPort,envs}), {stdio:["ignore","pipe","pipe"]});
    let out=""; let timed=false; const t=setTimeout(()=>{ timed=true; try{child.kill("SIGKILL");}catch{}; }, RUN_TIMEOUT_MS);
    child.stdout.on("data",d=>out+=d.toString());
    child.on("close", async (code)=>{
      clearTimeout(t);
      if (timed || code!==0) return reject(new Error("docker run (warm) failed"));
      const id = (out.trim().split("\n").pop()||"").trim(); if (!id) return reject(new Error("no container id (warm)"));
      // Nur Game-Port warten (RCON ist für WarmAssign; dort gibt's schnelles Retry)
      const ready = await waitPort(GAME_PING_HOST, hostGamePort, READY_TIMEOUT_MS);
      if (!ready){ rmContainer(id); return reject(new Error("warm not ready")); }
      resolve({ id, name, port: hostGamePort, rconPort: hostRconPort });
    });
  });
}

function startMatchCold({ mapId, teamsConfigBase64 }){
  return new Promise((resolve,reject)=>{
    const matches = runningMatchesCount();
    const total   = psManaged(false).length;
    if (matches >= MATCH_CAP) return reject(new Error(`match cap reached (${MATCH_CAP})`));
    if (total   >= MAX_TOTAL) return reject(new Error(`total cap reached (${MAX_TOTAL})`));

    const port = getFreePort();
    if (!port) return reject(new Error(`no free ports (${PORT_MIN}–${PORT_MAX})`));

    const hostGamePort = port, hostRconPort = port + 1000;
    const matchId=uuidv4(), name=`match-${matchId}`;
    const envs=[`MATCH_ID=${matchId}`,`MAP_ID=${mapId}`,`TEAMS_CONFIG_B64=${teamsConfigBase64}`];
    const child=spawn(DOCKER, dockerRunArgs({name,hostGamePort,hostRconPort,envs}), {stdio:["ignore","pipe","pipe"]});
    let out=""; let timed=false; const t=setTimeout(()=>{ timed=true; try{child.kill("SIGKILL");}catch{}; }, RUN_TIMEOUT_MS);
    child.stdout.on("data",d=>out+=d.toString());
    child.on("close", async (code)=>{
      clearTimeout(t);
      if (timed || code!==0) return reject(new Error("docker run (match) failed"));
      const id=(out.trim().split("\n").pop()||"").trim(); if (!id) return reject(new Error("no container id"));
      const ready = await waitPort(GAME_PING_HOST, hostGamePort, READY_TIMEOUT_MS);
      if (!ready){ rmContainer(id); return reject(new Error("match not ready")); }
      resolve({ matchId, name, gamePort: hostGamePort, containerId: id });
    });
  });
}

/* ─── Warm → Match (fast) ─── */
async function assignWarmToMatch({ mapId, teamsConfigBase64 }){
  if (runningMatchesCount() >= MATCH_CAP) return null;
  const warm = listWarm(false); if (!warm.length) return null;

  const first = warm[0];
  const details = fetchMatchDetails(first.id, first);
  if (!details.port || !details.rconPort) return null;

  const matchId = uuidv4();

  // Fast-path: RCON sofort probieren, mit kurzen Retries (weil RCON oft 0.5–2s nach Game bereit ist)
  const trySend = async () => rconSend(GAME_PING_HOST, details.rconPort, RCON_PASS,
    `assignmatch ${matchId} ${mapId} ${teamsConfigBase64}`, 6000);

  let ok=false, lastErr=null;
  for (const delay of [0, 200, 400, 800]) {
    if (delay) await new Promise(r=>setTimeout(r, delay));
    try { await trySend(); ok=true; break; } catch(e){ lastErr=e; }
  }
  if (!ok) { console.error(`[RCON] warm assign failed: ${lastErr?.message||"unknown"}`); return null; }

  try { sh(DOCKER, ["rename", first.name, `match-${matchId}`]); } catch {}
  warmAssignedCache.set(first.id, teamsConfigBase64);
  ensureWarmPool(); // top-up in background

  return { matchId, name:`match-${matchId}`, gamePort: details.port, containerId:first.id, teamsConfigBase64 };
}

/* ─── Pool + Cleanup ─── */
let poolTickRunning = false;
async function ensureWarmPool(){
  if (poolTickRunning) return; poolTickRunning = true;
  try{
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
      try { await startWarmContainer(); } catch(e){ console.error("[pool] warm start failed:", e.message); break; }
    }
  } finally { poolTickRunning = false; }
}

/* Cleanup:
   - Entfernt alle managed Container, deren Status NICHT mit "Up " beginnt.
   - Optional: entfernt "Up" Matches, die auf keinem Game/RCON-Port antworten (stale) nach kurzer Prüfung.
*/
function cleanup({ checkStale=true } = {}) {
  const all = psManaged(true);
  let removed = 0;

  for (const c of all) {
    const isUp = /^Up\s/.test(c.status||"");
    if (!isUp) { if (rmContainer(c.id)) removed++; continue; }

    if (checkStale && c.name?.startsWith("match-")) {
      const det = fetchMatchDetails(c.id, c);
      const gameOk = det.port ? false : false; // if no port -> suspicious
      // Wenn Ports fehlen oder beide Ports in kurzer Probe nicht erreichbar → weg
      const bothClosed = (!det.port || !det.rconPort);
      if (bothClosed) { if (rmContainer(c.id)) removed++; continue; }
    }
  }
  return removed;
}

/* Periodic maintenance */
setInterval(ensureWarmPool, 3000);
setInterval(()=>cleanup({checkStale:false}), 15_000); // schnelle Entsorgung beendeter Container
ensureWarmPool();

/* ─── HTTP ─── */
const server = http.createServer((req,res)=>{
  res.setHeader("Access-Control-Allow-Origin","*");
  res.setHeader("Access-Control-Allow-Headers","content-type");
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
    const matches = listMatches(false).map(m => fetchMatchDetails(m.id, m))
      .filter(d => d.port && d.name && d.teamsConfigBase64);
    return send(200, { ok:true, matches });
  }

  if (req.method==="POST" && req.url==="/create-match"){
    let body=""; req.on("data",c=>body+=c);
    req.on("end", async ()=>{
      try{
        const { mapId, teamsConfigBase64 } = JSON.parse(body||"{}");
        if(!mapId) throw new Error("mapId missing");
        if(!teamsConfigBase64) throw new Error("teamsConfigBase64 missing");

        const fromPool = await assignWarmToMatch({ mapId, teamsConfigBase64 });
        if (fromPool) return send(201,{ ok:true, ...fromPool, fromPool:true });

        const cold = await startMatchCold({ mapId, teamsConfigBase64 });
        return send(201,{ ok:true, ...cold, fromPool:false });
      }catch(e){
        const code = /cap reached|no free ports/.test(e.message) ? 429 : 400;
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
        const ok = name ? rmContainer(name) :
          (port ? (rmContainer((psManaged(true).find(c=>(c.ports||"").includes(`:${Number(port)}->25565`))||{}).id||"")) : false);
        ensureWarmPool();
        return send(ok?200:404,{ ok });
      } catch(e){ return send(400,{ ok:false, error:e.message }); }
    });
    return;
  }

  if (req.method==="POST" && req.url==="/gc"){
    const removed = cleanup({checkStale:true});
    ensureWarmPool();
    return send(200, { ok:true, removed });
  }

  return send(404,{ ok:false, error:"Not Found" });
});

server.listen(PORT, HOST, ()=>{
  console.log(`[orchestrator] listening on ${HOST}:${PORT} | caps: total=${MAX_TOTAL}, matches=${MATCH_CAP}, warm<=${WARM_CAP} | ports=${PORT_MIN}-${PORT_MAX}`);
});

