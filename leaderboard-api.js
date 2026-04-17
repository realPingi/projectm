const express = require('express');
const redis = require('redis');
const fetch = require('node-fetch'); // Falls node-fetch nicht installiert ist: npm install node-fetch@2
const app = express();

const client = redis.createClient({ url: 'redis://localhost:6379' });
client.connect().catch(console.error);

// Funktion, um den Namen bei Mojang zu holen, falls er in Redis fehlt
async function fetchNameFromMojang(uuid) {
    try {
        console.log(`🔍 Frage Mojang nach Name für UUID: ${uuid}`);
        const response = await fetch(`https://sessionserver.mojang.com/session/minecraft/profile/${uuid}`);
        if (!response.ok) return null;
        const data = await response.json();
        if (data && data.name) {
            // Namen direkt in Redis speichern, damit wir nicht nochmal fragen müssen
            await client.hSet('player_index:uuid_to_name', uuid.toLowerCase(), data.name);
            return data.name;
        }
    } catch (e) {
        console.error(`❌ Mojang API Fehler für ${uuid}:`, e.message);
    }
    return null;
}

app.get('/api/leaderboard', async (req, res) => {
    const queue = req.query.queue || 'solo';
    const leaderboardKey = `leaderboard:elo:${queue}`;

    try {
        const topPlayers = await client.zRangeWithScores(leaderboardKey, 0, 99, { REV: true });

        const results = await Promise.all(topPlayers.map(async (player) => {
            const uuidNoPrefix = player.value.replace('player:', '').toLowerCase();
            
            // 1. In Redis nachschauen
            let name = await client.hGet('player_index:uuid_to_name', uuidNoPrefix);
            
            // 2. Wenn Redis nix weiß -> Mojang fragen
            if (!name || name === "Unknown") {
                const mojangName = await fetchNameFromMojang(uuidNoPrefix);
                if (mojangName) name = mojangName;
            }
            
            return {
                uuid: uuidNoPrefix,
                name: name || "Unknown",
                elo: player.score
            };
        }));

        res.json({ results });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

app.listen(8080, () => console.log("Leaderboard-API mit Mojang-Auto-Repair auf Port 8080"));