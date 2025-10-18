// simple-orchestrator.js (stateless, port-range + /matches + robust cleanup)
"use strict";

const http = require("http");
const net = require("net");
const { spawnSync, spawn } = require("child_process");
const { v4: uuidv4 } = require("uuid");

/* ───────── Konfiguration ───────── */

const PORT = Number(process.env.ORCH_PORT || 4000);
const HOST = process.env.ORCH_HOST || "0.0.0.0";
const DOCKER = process.env.DOCKER_BIN || "/usr/bin/docker";

const PORT_MIN = 25566;
const PORT_MAX = 25569;
const MAX_MATCHES = 4; // parallel limit
const LABEL = "projectm=game";

// Docker run Timeout (bis Container-ID zurückkommt)
const RUN_TIMEOUT_MS = Number(process.env.RUN_TIMEOUT_MS || 10_000);

// Ready-Warteparameter (MC-Status)
const READY_TIMEOUT_MS = Number(process.env.READY_TIMEOUT_MS || 60_000);
const PING_TIMEOUT_MS = Number(process.env.PING_TIMEOUT_MS || 1500);
const REQUIRED_OK = Number(process.env.REQUIRED_OK || 3);
const GAP_OK_MS = Number(process.env.GAP_OK_MS || 300);
const GRACE_FIRST_MS = Number(process.env.GRACE_FIRST_MS || 500);

// Host, gegen den gepingt wird (127.0.0.1 wenn Docker und Orchestrator auf gleichem Host)
const GAME_PING_HOST = process.env.GAME_PING_HOST || "127.0.0.1";

// Cleanup immer aktiv, mit Schonfrist (damit Debug möglich ist)
const CLEANUP_GRACE_MS = Number(process.env.CLEANUP_GRACE_MS || 5 * 60 * 1000); // 5 min
const CLEANUP_INTERVAL = Number(process.env.CLEANUP_INTERVAL || 30_000); // 30 s
// Sofortiges Löschen bei Ready-Fehler?
const EAGER_FAIL_RM = (process.env.EAGER_FAIL_RM || "false").toLowerCase() === "true";

/* ───────── helpers ───────── */

function sh(cmd, args, opts = {}) {
  const res = spawnSync(cmd, args, { encoding: "utf8", ...opts });
  if (res.error) throw res.error;
  return {
    code: res.status ?? 0,
    out: (res.stdout || "").trim(),
    err: (res.stderr || "").trim(),
  };
}

function dockerInspect(idOrName) {
  const { code, out } = sh(DOCKER, ["inspect", idOrName]);
  if (code !== 0 || !out) return null;
  try {
    return JSON.parse(out)[0];
  } catch {
    return null;
  }
}

function listGameContainers(all = false) {
  const args = [
    "ps",
    "--format",
    "{{.ID}}|{{.Names}}|{{.Ports}}|{{.Status}}",
    "--filter",
    `label=${LABEL}`,
  ];
  if (all) args.splice(1, 0, "-a");
  const { code, out } = sh(DOCKER, args);
  if (code !== 0 || !out) return [];
  return out
    .split("\n")
    .filter(Boolean)
    .map((l) => {
      const [id, name, ports, status] = l.split("|");
      return { id, name, ports: ports || "", status: status || "" };
    });
}

function usedHostPorts() {
  const arr = listGameContainers(true);
  const ports = new Set();
  for (const c of arr) {
    // examples: "0.0.0.0:25566->25565/tcp" or ":::25566->25565/tcp"
    const m = (c.ports || "").match(/:(\d+)->25565\/tcp/);
    if (m) ports.add(Number(m[1]));
  }
  return ports;
}

function findFreePort() {
  const used = usedHostPorts();
  for (let p = PORT_MIN; p <= PORT_MAX; p++) {
    if (!used.has(p)) return p;
  }
  throw new Error("No free ports in 25566-25569");
}

function runningMatchesCount() {
  return listGameContainers(false).length;
}

function rmContainer(nameOrId) {
  try {
    sh(DOCKER, ["rm", "-f", nameOrId]);
    return true;
  } catch {
    return false;
  }
}

function resolveByPort(port) {
  const all = listGameContainers(true);
  return all.find((c) => (c.ports || "").includes(`:${port}->25565`));
}

/* ───────── Status & Cleanup ───────── */

function listMatchesWithTeams() {
  const rows = listGameContainers(true);
  const result = [];
  for (const c of rows) {
    const ins = dockerInspect(c.id);
    if (!ins) continue;

    // Host-Port (25565 im Container)
    let hostPort = null;
    const p = ins.NetworkSettings?.Ports?.["25565/tcp"];
    if (Array.isArray(p) && p.length > 0 && p[0]?.HostPort) {
      hostPort = Number(p[0].HostPort);
    }

    // Teams Base64 aus Env lesen
    let teamsB64 = null;
    for (const kv of ins.Config?.Env || []) {
      if (kv.startsWith("TEAMS_CONFIG_B64=")) {
        teamsB64 = kv.substring("TEAMS_CONFIG_B64=".length);
        break;
      }
    }

    if (hostPort) {
      result.push({
        containerId: c.id,
        name: c.name,
        port: hostPort,
        teamsConfigBase64: teamsB64,
      });
    }
  }
  return result;
}

// Cleanup-Loop: löscht NICHT laufende Container nach Schonfrist
setInterval(() => {
  try {
    const all = listGameContainers(true);
    const now = Date.now();
    for (const c of all) {
      const ins = dockerInspect(c.id);
      if (!ins || !ins.State) continue;
      const running = !!ins.State.Running;

      const finishedAt =
        ins.State.FinishedAt && ins.State.FinishedAt !== "0001-01-01T00:00:00Z"
          ? Date.parse(ins.State.FinishedAt)
          : null;
      const createdAt = ins.Created ? Date.parse(ins.Created) : null;

      const ageMs = finishedAt
        ? now - finishedAt
        : createdAt
        ? now - createdAt
        : Infinity;

      if (!running && ageMs > CLEANUP_GRACE_MS) {
        rmContainer(c.id);
        // console.log(`[cleanup] removed ${c.name} after ${Math.round(ageMs/1000)}s`);
      }
    }
  } catch {
    // ignore
  }
}, CLEANUP_INTERVAL);

/* ───────── Ready-Logic (TCP + MC Status) ───────── */

// schneller TCP-Check
function tcpConnectOk(host, port, timeoutMs = 800) {
  return new Promise((resolve) => {
    const s = net.createConnection({ host, port });
    let settled = false;
    const done = (ok) => {
      if (!settled) {
        settled = true;
        try {
          s.destroy();
        } catch {}
        resolve(ok);
      }
    };
    s.setTimeout(timeoutMs, () => done(false));
    s.on("error", () => done(false));
    s.on("connect", () => done(true));
  });
}

function isContainerRunning(idOrName) {
  const ins = dockerInspect(idOrName);
  return !!(ins && ins.State && ins.State.Running);
}

// VarInt & String utils für MC-Protokoll
function writeVarInt(n) {
  const bytes = [];
  do {
    let temp = n & 0x7f;
    n >>>= 7;
    if (n !== 0) temp |= 0x80;
    bytes.push(temp);
  } while (n !== 0);
  return Buffer.from(bytes);
}
function writeString(s) {
  const data = Buffer.from(s, "utf8");
  return Buffer.concat([writeVarInt(data.length), data]);
}

// Robust: liest das MC-Statuspaket korrekt (VarInt framing)
function mcStatusPing(host, port, timeoutMs = PING_TIMEOUT_MS, proto = 758) {
  return new Promise((resolve) => {
    const socket = net.createConnection({ host, port });
    socket.setNoDelay(true);

    let done = false;
    const finish = (ok) => {
      if (!done) {
        done = true;
        try { socket.destroy(); } catch {}
        resolve(ok);
      }
    };

    socket.setTimeout(timeoutMs, () => finish(false));
    socket.on('error', () => finish(false));

    socket.on('connect', () => {
      try {
        // ---- Handshake (state=1 status) ----
        const hostBuf = writeString(host);
        const portBuf = Buffer.allocUnsafe(2); portBuf.writeUInt16BE(port);
        const hsPayload = Buffer.concat([
          writeVarInt(0x00),      // packet id
          writeVarInt(proto),     // protocol version (beliebig ok)
          hostBuf,                // server address (string)
          portBuf,                // server port (unsigned short)
          writeVarInt(0x01)       // next state = status (1)
        ]);
        const hsPacket = Buffer.concat([writeVarInt(hsPayload.length), hsPayload]);

        // ---- Status Request (packet id 0x00) ----
        const reqPayload = writeVarInt(0x00);
        const reqPacket  = Buffer.concat([writeVarInt(reqPayload.length), reqPayload]);

        socket.write(hsPacket);
        socket.write(reqPacket);
      } catch {
        return finish(false);
      }
    });

    // Puffer & Parser: VarInt-Länge → PacketId → JSON-Länge → JSON
    let buf = Buffer.alloc(0);

    function readVarIntFrom(b, offset) {
      let numRead = 0, result = 0, read;
      do {
        if (offset + numRead >= b.length) return null; // noch nicht genug Daten
        read = b[offset + numRead];
        const value = (read & 0x7F);
        result |= (value << (7 * numRead));
        numRead++;
        if (numRead > 5) return null; // defekt
      } while ((read & 0x80) === 0x80);
      return { value: result, size: numRead };
    }

    socket.on('data', (chunk) => {
      if (done) return;
      buf = Buffer.concat([buf, chunk]);

      // Paket-Länge
      const len = readVarIntFrom(buf, 0);
      if (!len) return; // warten
      const totalLen = len.value;
      const afterLen = len.size;

      if (buf.length < afterLen + totalLen) return; // noch nicht komplett

      // PacketId
      const pid = readVarIntFrom(buf, afterLen);
      if (!pid) return; // ungewöhnlich, aber weiter warten
      if (pid.value !== 0x00) return finish(false); // Status-Response muss 0x00 sein
      let off = afterLen + pid.size;

      // JSON-Länge
      const jlen = readVarIntFrom(buf, off);
      if (!jlen) return;
      off += jlen.size;

      if (off + jlen.value > buf.length) return; // noch nicht komplett

      // JSON
      const jsonStr = buf.slice(off, off + jlen.value).toString('utf8');
      try {
        const parsed = JSON.parse(jsonStr);
        // minimale Plausibilitätsprüfung
        if (parsed && typeof parsed === 'object' && parsed.version) {
          return finish(true);
        }
      } catch {
        // invalid JSON → fail
      }
      return finish(false);
    });

    socket.on('end', () => finish(false));
  });
}


function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms));
}

/**
 * Wartet robust auf Readiness:
 * - prüft seriell Hosts (z.B. [GAME_PING_HOST, '127.0.0.1', '172.17.0.1'])
 * - bricht ab, wenn Container in der Zwischenzeit stoppt
 * - nutzt erst TCP-Connect, dann MC-Status-Ping
 */
async function waitUntilReady(hosts, port, containerId) {
  const start = Date.now();
  const hostList = Array.isArray(hosts) ? hosts : [hosts];

  let okInRow = 0;
  let firstOkAt = -1;

  while (Date.now() - start < READY_TIMEOUT_MS) {
    if (containerId && !isContainerRunning(containerId)) {
      throw new Error("container stopped during readiness wait");
    }

    // 1) schneller TCP-Check auf irgendeinen Host
    let tcpOk = false;
    for (const h of hostList) {
      // eslint-disable-next-line no-await-in-loop
      if (await tcpConnectOk(h, port, Math.min(PING_TIMEOUT_MS, 800))) {
        tcpOk = true;
        break;
      }
    }
    if (!tcpOk) {
      okInRow = 0;
      firstOkAt = -1;
      // eslint-disable-next-line no-await-in-loop
      await sleep(250);
      continue;
    }

    // 2) Ein MC-Status-Ping (auf den ersten Host der Liste)
    const pingHost = hostList[0];
    // eslint-disable-next-line no-await-in-loop
    const ok = await mcStatusPing(pingHost, port, PING_TIMEOUT_MS);
    if (ok) {
      okInRow++;
      if (firstOkAt < 0) firstOkAt = Date.now();
      if (okInRow >= REQUIRED_OK && Date.now() - firstOkAt >= GRACE_FIRST_MS) return true;
      // eslint-disable-next-line no-await-in-loop
      await sleep(GAP_OK_MS);
    } else {
      okInRow = 0;
      firstOkAt = -1;
      // eslint-disable-next-line no-await-in-loop
      await sleep(250);
    }
  }
  return false;
}

/* ───────── core ops ───────── */

function startGameServer({ mapId, teamsConfigBase64 }) {
  return new Promise((resolve, reject) => {
    try {
      if (!mapId) return reject(new Error("mapId missing"));
      if (!teamsConfigBase64) return reject(new Error("teamsConfigBase64 missing"));
      if (runningMatchesCount() >= MAX_MATCHES) return reject(new Error("Max server limit reached"));

      const port = findFreePort();
      const matchId = uuidv4();
      const name = `match-${matchId}`;

      const args = [
        "run",
        "-d",
        "--label",
        LABEL,
        "--name",
        name,
        "-p",
        `${port}:25565`,
        "-e",
        `MATCH_ID=${matchId}`,
        "-e",
        `MAP_ID=${mapId}`,
        "-e",
        `TEAMS_CONFIG_B64=${teamsConfigBase64}`,
        "realpingi/game:ready",
      ];

      console.log(`[orchestrator] starting match ${matchId} on port ${port}…`);

      const child = spawn(DOCKER, args, { stdio: ["ignore", "pipe", "pipe"] });
      let stdout = "",
        stderr = "";
      let timedOut = false;
      const t = setTimeout(() => {
        timedOut = true;
        try {
          child.kill("SIGKILL");
        } catch {}
      }, RUN_TIMEOUT_MS);

      child.stdout.on("data", (d) => (stdout += d.toString()));
      child.stderr.on("data", (d) => (stderr += d.toString()));
      child.on("error", (err) => {
        clearTimeout(t);
        reject(err);
      });
      child.on("close", async (code) => {
        clearTimeout(t);
        if (timedOut) return reject(new Error("docker run timed out"));
        if (code !== 0)
          return reject(new Error(`docker run failed (${code}): ${(stderr || stdout).trim()}`));
        const containerId = (stdout.trim().split("\n").pop() || "").trim();
        if (!containerId) return reject(new Error("no container id"));

        console.log(
          `[orchestrator] container ${containerId} started; waiting ready on ${GAME_PING_HOST}:${port}`
        );

        try {
          const hostsToTry = [GAME_PING_HOST, "127.0.0.1", "172.17.0.1"].filter(
            (v, i, a) => v && a.indexOf(v) === i
          );
          const ready = await waitUntilReady(hostsToTry, port, containerId);
          console.log(
            `[orchestrator] ready=${ready} on ${hostsToTry[0]}:${port} (match ${matchId})`
          );
          if (!ready) {
            if (EAGER_FAIL_RM) rmContainer(containerId);
            return reject(new Error("game not ready in time"));
          }
        } catch (e) {
          console.error(`[orchestrator] readiness failed: ${e.message}`);
          if (EAGER_FAIL_RM) rmContainer(containerId);
          return reject(e);
        }

        resolve({ matchId, name, gamePort: port, containerId });
      });
    } catch (e) {
      reject(e);
    }
  });
}

function endGame({ name, port }) {
  if (name) return rmContainer(name);
  if (port) {
    const c = resolveByPort(Number(port));
    return c ? rmContainer(c.name) : false;
  }
  return false;
}

/* ───────── http api ───────── */

const server = http.createServer(async (req, res) => {
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Headers", "content-type");
  if (req.method === "OPTIONS") {
    res.writeHead(204);
    return res.end();
  }

  try {
    if (req.method === "GET" && req.url === "/health") {
      const usedSet = usedHostPorts();
      const used = Array.from(usedSet).sort((a, b) => a - b);
      const free = [];
      for (let p = PORT_MIN; p <= PORT_MAX; p++) if (!usedSet.has(p)) free.push(p);
      const running = runningMatchesCount();
      res.writeHead(200, { "content-type": "application/json" });
      return res.end(
        JSON.stringify({
          ok: true,
          running,
          used,
          free,
          max: MAX_MATCHES,
          cleanup_grace_ms: CLEANUP_GRACE_MS,
          cleanup_interval_ms: CLEANUP_INTERVAL,
          ping_host: GAME_PING_HOST,
          ready_timeout_ms: READY_TIMEOUT_MS,
        })
      );
    }

    if (req.method === "GET" && req.url === "/matches") {
      const matches = listMatchesWithTeams();
      res.writeHead(200, { "content-type": "application/json" });
      return res.end(JSON.stringify({ ok: true, matches }));
    }

    if (req.method === "POST" && req.url === "/create-match") {
      let body = "";
      req.on("data", (c) => (body += c));
      req.on("end", async () => {
        try {
          const { mapId, teamsConfigBase64 } = JSON.parse(body || "{}");
          const { matchId, name, gamePort, containerId } = await startGameServer({
            mapId,
            teamsConfigBase64,
          });
          res.writeHead(201, { "content-type": "application/json" });
          res.end(JSON.stringify({ ok: true, matchId, name, gamePort, containerId }));
        } catch (e) {
          res.writeHead(400, { "content-type": "application/json" });
          res.end(JSON.stringify({ ok: false, error: e.message }));
        }
      });
      return;
    }

    if (req.method === "POST" && req.url === "/end-match") {
      let body = "";
      req.on("data", (c) => (body += c));
      req.on("end", () => {
        try {
          const { name, port } = JSON.parse(body || "{}");
          const ok = endGame({ name, port });
          res.writeHead(ok ? 200 : 404, { "content-type": "application/json" });
          res.end(JSON.stringify({ ok }));
        } catch (e) {
          res.writeHead(400, { "content-type": "application/json" });
          res.end(JSON.stringify({ ok: false, error: e.message }));
        }
      });
      return;
    }

    res.writeHead(404, { "content-type": "application/json" });
    res.end(JSON.stringify({ ok: false, error: "Not Found" }));
  } catch (e) {
    res.writeHead(500, { "content-type": "application/json" });
    res.end(JSON.stringify({ ok: false, error: e.message }));
  }
});

server.listen(PORT, HOST, () => {
  console.log(
    `[orchestrator] listening on ${HOST}:${PORT} (range ${PORT_MIN}-${PORT_MAX})`
  );
});
