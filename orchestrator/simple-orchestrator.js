// simple-orchestrator.js (stateless, port-range + auto-clean)
const http = require('http');
const { spawnSync, spawn } = require('child_process');
const { v4: uuidv4 } = require('uuid');

const PORT = Number(process.env.ORCH_PORT || 4000);
const HOST = process.env.ORCH_HOST || '0.0.0.0';
const DOCKER = process.env.DOCKER_BIN || '/usr/bin/docker';

const PORT_MIN = 25566;
const PORT_MAX = 25569;
const MAX_MATCHES = 4;              // via running containers (label)
const RUN_TIMEOUT_MS = 10_000;
const LABEL = 'projectm=game';

/* ───────── Helpers ───────── */

function sh(cmd, args, opts={}) {
  const res = spawnSync(cmd, args, { encoding: 'utf8', ...opts });
  if (res.error) throw res.error;
  return { code: res.status ?? 0, out: (res.stdout||'').trim(), err: (res.stderr||'').trim() };
}

function listGameContainers(all=false) {
  // returns array of {id,name,ports,status}
  const args = ['ps', '--format', '{{.ID}}|{{.Names}}|{{.Ports}}|{{.Status}}', '--filter', `label=${LABEL}`];
  if (all) args.splice(1, 0, '-a');
  const { code, out } = sh(DOCKER, args);
  if (code !== 0 || !out) return [];
  return out.split('\n').map(l => {
    const [id,name,ports,status] = l.split('|');
    return { id, name, ports: (ports||''), status: (status||'') };
  });
}

function usedHostPorts() {
  const arr = listGameContainers(true);
  const ports = new Set();
  for (const c of arr) {
    // example c.ports: "0.0.0.0:25566->25565/tcp"
    const m = c.ports.match(/:(\d+)->25565\/tcp/);
    if (m) ports.add(Number(m[1]));
  }
  return ports;
}

function findFreePort() {
  const used = usedHostPorts();
  for (let p = PORT_MIN; p <= PORT_MAX; p++) {
    if (!used.has(p)) return p;
  }
  throw new Error('No free ports in 25566-25569');
}

function runningMatchesCount() {
  return listGameContainers(false).length;
}

function rmContainer(nameOrId) {
  try {
    sh(DOCKER, ['rm', '-f', nameOrId]);
    return true;
  } catch { return false; }
}

function resolveByPort(port) {
  const all = listGameContainers(true);
  return all.find(c => (c.ports || '').includes(`:${port}->25565`));
}

/* ───────── Core ops ───────── */

function startGameServer({ mapId, teamsConfigBase64 }) {
  return new Promise((resolve, reject) => {
    try {
      if (!mapId) return reject(new Error('mapId missing'));
      if (!teamsConfigBase64) return reject(new Error('teamsConfigBase64 missing'));
      if (runningMatchesCount() >= MAX_MATCHES) return reject(new Error('Max server limit reached'));

      const port = findFreePort();
      const matchId = uuidv4();
      const name = `match-${matchId}`;

      const args = [
        'run','-d',
        '--label', LABEL,
        '--name', name,
        '-p', `${port}:25565`,
        '-e', `MATCH_ID=${matchId}`,
        '-e', `MAP_ID=${mapId}`,
        '-e', `TEAMS_CONFIG_B64=${teamsConfigBase64}`,
        'realpingi/game:ready'
      ];

      const child = spawn(DOCKER, args, { stdio: ['ignore','pipe','pipe'] });
      let stdout = '', stderr = '';
      let timedOut = false;
      const t = setTimeout(() => { timedOut = true; child.kill('SIGKILL'); }, RUN_TIMEOUT_MS);

      child.stdout.on('data', d => stdout += d.toString());
      child.stderr.on('data', d => stderr += d.toString());
      child.on('error', err => { clearTimeout(t); reject(err); });
      child.on('close', code => {
        clearTimeout(t);
        if (timedOut) return reject(new Error('docker run timed out'));
        if (code !== 0) return reject(new Error(`docker run failed (${code}): ${(stderr || stdout).trim()}`));
        const containerId = stdout.trim().split('\n').pop();
        if (!containerId) return reject(new Error('no container id'));
        resolve({ matchId, name, gamePort: port, containerId });
      });
    } catch (e) { reject(e); }
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

/* ───────── Auto-clean loop ─────────
   Entfernt alle mit Label markierten Container,
   die nicht mehr laufen (exited, dead). Jede 30s.
*/
setInterval(() => {
  try {
    const all = listGameContainers(true);
    for (const c of all) {
      const st = (c.status || '').toLowerCase();
      if (st.startsWith('exited') || st.includes('dead') || st.includes('removing') || st.includes('created')) {
        rmContainer(c.name);
        // optional: console.log(`[cleanup] removed ${c.name} (${st})`);
      }
    }
  } catch {/* ignore */}
}, 30_000);

/* ───────── HTTP API ───────── */

const server = http.createServer(async (req, res) => {
  // CORS
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Headers', 'content-type');
  if (req.method === 'OPTIONS') { res.writeHead(204); return res.end(); }

  try {
    if (req.method === 'GET' && req.url === '/health') {
      const used = Array.from(usedHostPorts()).sort((a,b)=>a-b);
      const free = [];
      for (let p = PORT_MIN; p <= PORT_MAX; p++) if (!used.includes(p)) free.push(p);
      const running = runningMatchesCount();
      res.writeHead(200, {'content-type':'application/json'});
      return res.end(JSON.stringify({ ok:true, running, used, free, max: MAX_MATCHES }));
    }

    if (req.method === 'POST' && req.url === '/create-match') {
      let body = ''; req.on('data', c => body += c);
      req.on('end', async () => {
        try {
          const { mapId, teamsConfigBase64 } = JSON.parse(body || '{}');
          const { matchId, name, gamePort, containerId } = await startGameServer({ mapId, teamsConfigBase64 });
          res.writeHead(201, {'content-type':'application/json'});
          res.end(JSON.stringify({ ok:true, matchId, name, gamePort, containerId }));
        } catch (e) {
          res.writeHead(400, {'content-type':'application/json'});
          res.end(JSON.stringify({ ok:false, error: e.message }));
        }
      });
      return;
    }

    if (req.method === 'POST' && req.url === '/end-match') {
      let body = ''; req.on('data', c => body += c);
      req.on('end', () => {
        try {
          const { name, port } = JSON.parse(body || '{}');
          const ok = endGame({ name, port });
          res.writeHead(ok ? 200 : 404, {'content-type':'application/json'});
          res.end(JSON.stringify({ ok }));
        } catch (e) {
          res.writeHead(400, {'content-type':'application/json'});
          res.end(JSON.stringify({ ok:false, error: e.message }));
        }
      });
      return;
    }

    res.writeHead(404, {'content-type':'application/json'});
    res.end(JSON.stringify({ ok:false, error:'Not Found' }));
  } catch (e) {
    res.writeHead(500, {'content-type':'application/json'});
    res.end(JSON.stringify({ ok:false, error: e.message }));
  }
});

server.listen(PORT, HOST, () => console.log(`[orchestrator] listening on ${HOST}:${PORT} (range ${PORT_MIN}-${PORT_MAX})`));
