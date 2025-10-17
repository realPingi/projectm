// simple-orchestrator.js
const http = require('http');
const { spawn } = require('child_process');
const { v4: uuidv4 } = require('uuid');

const MAX_ACTIVE_GAMES = 4;
const BASE_GAME_PORT = 25566;
const ACTIVE_MATCHES = {}; // matchId -> { containerId, port, name }

const PORT = Number(process.env.ORCH_PORT || 4000);
const HOST = process.env.ORCH_HOST || '0.0.0.0';
const DOCKER_BIN = process.env.DOCKER_BIN || '/usr/bin/docker';
const DOCKER_RUN_TIMEOUT = 10_000;

function nextPort() {
  const used = new Set(Object.values(ACTIVE_MATCHES).map(m => m.port));
  for (let p = BASE_GAME_PORT; p < BASE_GAME_PORT + 50; p++) if (!used.has(p)) return p;
  throw new Error('No free game port');
}

function startGameServer(mapId, teamsConfigBase64) {
  return new Promise((resolve, reject) => {
    try {
      if (!mapId) return reject(new Error('mapId missing'));
      if (!teamsConfigBase64) return reject(new Error('teamsConfigBase64 missing'));
      if (Object.keys(ACTIVE_MATCHES).length >= MAX_ACTIVE_GAMES) {
        return reject(new Error('Max server limit reached'));
      }

      const matchId = uuidv4();
      const port = nextPort();
      const name = `match-${matchId}`;

      const args = [
        'run', '-d',
        '-p', `${port}:25565`,
        '--name', name,
        '-e', `MATCH_ID=${matchId}`,
        '-e', `MAP_ID=${mapId}`,
        '-e', `TEAMS_CONFIG_B64=${teamsConfigBase64}`,
        'realpingi/game:ready'
      ];

      const child = spawn(DOCKER_BIN, args, { stdio: ['ignore', 'pipe', 'pipe'] });
      let stdout = '', stderr = '';
      let timedOut = false;
      const t = setTimeout(() => { timedOut = true; child.kill('SIGKILL'); }, DOCKER_RUN_TIMEOUT);

      child.stdout.on('data', d => stdout += d.toString());
      child.stderr.on('data', d => stderr += d.toString());
      child.on('error', err => { clearTimeout(t); reject(err); });
      child.on('close', code => {
        clearTimeout(t);
        if (timedOut) return reject(new Error('docker run timed out'));
        if (code !== 0) return reject(new Error(`docker run failed (${code}): ${stderr || stdout}`.trim()));

        const containerId = stdout.trim().split('\n').pop();
        if (!containerId) return reject(new Error('no container id'));
        ACTIVE_MATCHES[matchId] = { containerId, port, name };
        resolve({ matchId, port, containerId });
      });
    } catch (e) { reject(e); }
  });
}

function endGameServer(matchId) {
  const m = ACTIVE_MATCHES[matchId];
  if (!m) return false;
  spawn(DOCKER_BIN, ['rm', '-f', m.name], { stdio: 'ignore' });
  delete ACTIVE_MATCHES[matchId];
  return true;
}

// EIN Server, EIN Handler
const server = http.createServer(async (req, res) => {
  try {
    // CORS + JSON
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Headers', 'content-type');
    if (req.method === 'OPTIONS') { res.writeHead(204); return res.end(); }

    if (req.method === 'GET' && req.url === '/health') {
      res.writeHead(200, {'content-type':'application/json'});
      return res.end(JSON.stringify({ ok: true, active: Object.keys(ACTIVE_MATCHES).length }));
    }

    if (req.method === 'POST' && req.url === '/create-match') {
      let body = ''; req.on('data', c => body += c);
      req.on('end', async () => {
        try {
          const { mapId, teamsConfigBase64 } = JSON.parse(body || '{}');
          const { matchId, port } = await startGameServer(mapId, teamsConfigBase64);
          res.writeHead(201, {'content-type':'application/json'});
          res.end(JSON.stringify({ ok: true, matchId, gamePort: port }));
        } catch (e) {
          res.writeHead(400, {'content-type':'application/json'});
          res.end(JSON.stringify({ ok: false, error: e.message }));
        }
      });
      return;
    }

    if (req.method === 'POST' && req.url === '/end-match') {
      let body = ''; req.on('data', c => body += c);
      req.on('end', () => {
        try {
          const { matchId } = JSON.parse(body || '{}');
          const ok = endGameServer(matchId);
          res.writeHead(ok ? 200 : 404, {'content-type':'application/json'});
          res.end(JSON.stringify({ ok }));
        } catch (e) {
          res.writeHead(400, {'content-type':'application/json'});
          res.end(JSON.stringify({ ok: false, error: e.message }));
        }
      });
      return;
    }

    res.writeHead(404, {'content-type':'application/json'});
    res.end(JSON.stringify({ ok: false, error: 'Not Found' }));
  } catch (e) {
    res.writeHead(500, {'content-type':'application/json'});
    res.end(JSON.stringify({ ok: false, error: e.message }));
  }
});

server.listen(PORT, HOST, () => console.log(`[orchestrator] listening on ${HOST}:${PORT}`));

