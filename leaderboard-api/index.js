const express = require('express');
const redis = require('redis');

// Sicherer Import für node-fetch (kompatibel mit v2 und v3)
const fetch = (...args) => import('node-fetch').then(({default: fetch}) => fetch(...args));

const app = express();

/* =========================
   REDIS CLIENT SETUP
========================= */
const client = redis.createClient({
    url: 'redis://redis:6379',
    socket: {
        reconnectStrategy: (retries) => {
            console.log(`🔁 Redis reconnect attempt ${retries}`);
            return Math.min(retries * 50, 2000);
        }
    }
});

client.on('connect', () => console.log('✅ Redis verbunden!'));
client.on('error', (err) => console.error('❌ Redis Fehler:', err));

/* =========================
   MOJANG NAME FETCH
========================= */
async function fetchNameFromMojang(uuid) {
    try {
        const cleanUuid = uuid.replace(/-/g, '');
        const response = await fetch(`https://sessionserver.mojang.com/session/minecraft/profile/${cleanUuid}`);

        if (!response.ok) return null;

        const data = await response.json();
        if (data?.name) {
            await client.hSet('player_index:uuid_to_name', uuid.toLowerCase(), data.name);
            return data.name;
        }
    } catch (e) {
        console.error(`❌ Mojang API Fehler für ${uuid}:`, e.message);
    }
    return null;
}

/* =========================
   ENDPOINTS
========================= */
app.get('/api/leaderboard', async (req, res) => {
    const queue = (req.query.queue || 'solo').toLowerCase();
    const leaderboardKey = `leaderboard:elo:${queue}`;

    try {
        const topPlayers = await client.zRangeWithScores(leaderboardKey, 0, 99, { REV: true });

        const results = await Promise.all(
            topPlayers.map(async (player) => {
                const uuid = player.value.replace('player:', '').toLowerCase();
                let name = await client.hGet('player_index:uuid_to_name', uuid);

                if (!name) {
                    name = await fetchNameFromMojang(uuid);
                }

                return {
                    uuid,
                    name: name || 'Unknown',
                    elo: player.score
                };
            })
        );

        res.json({ queue, count: results.length, results });
    } catch (err) {
        console.error('❌ Leaderboard Fehler:', err);
        res.status(500).json({ error: "Interner Serverfehler" });
    }
});

app.get('/health', async (req, res) => {
    try {
        await client.ping();
        res.json({ status: 'ok', redis: 'connected' });
    } catch (e) {
        res.status(500).json({ status: 'error', redis: 'disconnected' });
    }
});

/* =========================
   START LOGIK (Der Fix)
========================= */
async function startServer() {
    try {
        console.log('⏳ Verbinde mit Redis...');
        // Hier ist das await jetzt innerhalb einer async function erlaubt
        await client.connect();
        
        app.listen(8080, () => {
            console.log('🚀 Leaderboard API läuft auf Port 8080');
        });
    } catch (err) {
        console.error('❌ Kritischer Fehler beim Starten:', err);
        process.exit(1);
    }
}

startServer();