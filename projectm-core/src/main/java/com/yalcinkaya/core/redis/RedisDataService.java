package com.yalcinkaya.core.redis;

import com.google.gson.Gson;
import com.yalcinkaya.core.ProjectM;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import redis.clients.jedis.*;
import redis.clients.jedis.params.ZAddParams;
import redis.clients.jedis.resps.Tuple;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class RedisDataService implements AutoCloseable {

    // --- Verbindungskonfiguration (bei dir: Redis läuft im Docker-Netz) ---
    private static final String REDIS_HOST = "redis";
    private static final int REDIS_PORT = 6379;

    // --- Defaults ---
    public static final int STARTING_ELO = 1000;
    private static final String DEFAULT_RANK_NAME = "DEFAULT";

    // --- Redis Keys ---
    private static final String KEY_LEADERBOARD_ELO_BASE = "leaderboard:elo:";  // + <queueKey>
    private static final String KEY_PLAYER_HASH_PREFIX   = "player:";           // + <normalizedUuid>

    // --- Hash-Felder ---
    private static final String FIELD_ELO_BASE   = "elo:"; // + <queueKey>
    private static final String FIELD_NAME       = "name";
    private static final String FIELD_RANK_NAME  = "rank_name";
    private static final String FIELD_WINS       = "wins";

    private final JedisPool pool;
    private final Gson gson = new Gson();

    public RedisDataService() {
        // Konfigurierter Pool (stabiler in Prod)
        JedisPoolConfig cfg = new JedisPoolConfig();
        cfg.setMaxTotal(16);
        cfg.setMaxIdle(8);
        cfg.setMinIdle(1);
        cfg.setTestOnBorrow(true);
        cfg.setTestOnReturn(false);
        cfg.setTestWhileIdle(true);
        cfg.setMinEvictableIdleTimeMillis(60_000);
        cfg.setTimeBetweenEvictionRunsMillis(30_000);

        this.pool = new JedisPool(cfg, REDIS_HOST, REDIS_PORT);

        try (Jedis jedis = pool.getResource()) {
            jedis.ping();
            System.out.println("[Redis] Connected to " + REDIS_HOST + " via pool.");
        } catch (Exception e) {
            System.err.println("[Redis] Connection failed: " + e.getMessage());
        }
    }

    // ---------- Key/Field Helpers ----------

    private String getLeaderboardKey(QueueType queueType) {
        return KEY_LEADERBOARD_ELO_BASE + queueType.getRedisKey();
    }

    private String getEloField(QueueType queueType) {
        return FIELD_ELO_BASE + queueType.getRedisKey();
    }

    private String getPlayerKey(String uuid) {
        return KEY_PLAYER_HASH_PREFIX + uuid.replace("-", "").toLowerCase(Locale.ROOT);
    }

    // ---------- Bootstrap / Ensure Defaults ----------

    /**
     * Initialisiert (falls nicht vorhanden) ELO und Name im Spieler-Hash
     * und den passenden Leaderboard-Eintrag. Komplett idempotent.
     */
    private void ensureDefaultForQueue(Jedis jedis, String uuid, String playerName, QueueType queueType) {
        final String playerKey     = getPlayerKey(uuid);
        final String eloField      = getEloField(queueType);
        final String leaderboardKey = getLeaderboardKey(queueType);

        // Name nur setzen, wenn nicht vorhanden
        jedis.hsetnx(playerKey, FIELD_NAME, playerName != null ? playerName : "");

        // ELO-Hashfeld: wenn fehlt -> auf 1000 setzen
        Long created = jedis.hsetnx(playerKey, eloField, String.valueOf(STARTING_ELO));

        // Leaderboard-Eintrag nur anlegen, wenn neu (NX)
        // (Falls bereits vorhanden, bleibt Score unverändert)
        try {
            ZAddParams params = ZAddParams.zAddParams().nx();
            jedis.zadd(leaderboardKey, STARTING_ELO, playerKey, params);
        } catch (NoSuchMethodError | Exception ignored) {
            // Fallback für ältere Jedis-Versionen ohne ZAddParams:
            if (created != null && created == 1L) {
                // Nur beim ersten Mal hinzufügen
                jedis.zadd(leaderboardKey, STARTING_ELO, playerKey);
            }
        }
    }

    /** Initialisiert einen Spieler für ALLE Queues (praktisch beim ersten Join). */
    public void bootstrapAllQueues(String uuid, String playerName) {
        try (Jedis jedis = pool.getResource()) {
            for (QueueType qt : QueueType.values()) {
                ensureDefaultForQueue(jedis, uuid, playerName, qt);
            }
        } catch (Exception e) {
            System.err.println("[Redis] bootstrapAllQueues failed for " + uuid + ": " + e.getMessage());
        }
    }

    // ---------- Public: ELO ----------

    /** Setzt ELO und aktualisiert Name sowie Leaderboard. */
    public void updateElo(String uuid, String playerName, QueueType queueType, int newElo) {
        final String playerKey = getPlayerKey(uuid);
        final String leaderboardKey = getLeaderboardKey(queueType);
        final String eloField = getEloField(queueType);

        try (Jedis jedis = pool.getResource()) {
            // Sicherstellen, dass Standardwerte existieren (idempotent)
            ensureDefaultForQueue(jedis, uuid, playerName, queueType);

            Transaction t = jedis.multi();
            t.hset(playerKey, eloField, String.valueOf(newElo));
            if (playerName != null && !playerName.isEmpty()) {
                t.hset(playerKey, FIELD_NAME, playerName);
            }
            t.zadd(leaderboardKey, newElo, playerKey);
            t.exec();
        } catch (Exception e) {
            System.err.printf("[Redis] updateElo failed for %s (%s): %s%n",
                    uuid, queueType.getRedisKey(), e.getMessage());
        }
    }

    /** Liefert ELO; legt fehlende Defaults automatisch an. */
    public int getElo(String uuid, QueueType queueType) {
        final String playerKey = getPlayerKey(uuid);
        final String eloField = getEloField(queueType);

        try (Jedis jedis = pool.getResource()) {
            // Lazy-Init: Falls Feld fehlt, direkt anlegen + Leaderboard NX
            if (!jedis.hexists(playerKey, eloField)) {
                // Name optional aus Hash (leerer String wenn nicht vorhanden)
                String name = jedis.hget(playerKey, FIELD_NAME);
                ensureDefaultForQueue(jedis, uuid, name, queueType);
            }
            String eloStr = jedis.hget(playerKey, eloField);
            return eloStr != null ? Integer.parseInt(eloStr) : STARTING_ELO;
        } catch (Exception e) {
            System.err.printf("[Redis] getElo failed for %s (%s): %s%n",
                    uuid, queueType.getRedisKey(), e.getMessage());
            return STARTING_ELO;
        }
    }

    /**
     * Erhöht/Verringert ELO atomar in Hash + Leaderboard.
     * Initialisiert bei Bedarf Defaults (1000).
     */
    public int addElo(String uuid, QueueType queueType, int delta) {
        final String leaderboardKey = getLeaderboardKey(queueType);
        final String eloField = getEloField(queueType);
        final String playerKey = getPlayerKey(uuid);

        int newElo = STARTING_ELO;

        try (Jedis jedis = pool.getResource()) {
            // Lazy-Init
            if (!jedis.hexists(playerKey, eloField)) {
                String name = jedis.hget(playerKey, FIELD_NAME);
                ensureDefaultForQueue(jedis, uuid, name, queueType);
            }

            // Aktuellen ELO lesen
            String currentEloString = jedis.hget(playerKey, eloField);
            int currentElo = (currentEloString != null) ? Integer.parseInt(currentEloString) : STARTING_ELO;
            newElo = currentElo + delta;

            Transaction t = jedis.multi();
            Response<Double> newScore = t.zincrby(leaderboardKey, delta, playerKey);
            Response<Long>   hsetRes  = t.hset(playerKey, eloField, String.valueOf(newElo));
            List<Object> exec = t.exec();

            // Debug/Validation
            if (exec == null) {
                System.err.printf("[Redis] addElo EXEC returned null for %s (%s)%n", uuid, queueType.name());
            } else {
                System.out.printf("[Redis] addElo OK %s (%s): newScore=%s, hsetRes=%s, newElo=%d%n",
                        uuid, queueType.name(),
                        newScore.get(), hsetRes.get(), newElo);
            }
        } catch (Exception e) {
            System.err.printf("[Redis] addElo failed for %s (%s): %s%n",
                    uuid, queueType.name(), e.getMessage());
        }

        return newElo;
    }

    // ---------- Public: Rank / Name ----------

    public void updateRank(String uuid, String rankName) {
        final String playerKey = getPlayerKey(uuid);
        try (Jedis jedis = pool.getResource()) {
            jedis.hset(playerKey, FIELD_RANK_NAME, rankName);
        } catch (Exception e) {
            System.err.println("[Redis] Failed to update rank for " + uuid + ": " + e.getMessage());
        }
    }

    public Rank getRank(String uuid) {
        String rankName = getRankName(uuid);
        try {
            return Rank.valueOf(rankName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Rank.DEFAULT;
        }
    }

    public String getRankName(String uuid) {
        final String playerKey = getPlayerKey(uuid);
        try (Jedis jedis = pool.getResource()) {
            String rank = jedis.hget(playerKey, FIELD_RANK_NAME);
            return (rank != null) ? rank : DEFAULT_RANK_NAME;
        } catch (Exception e) {
            System.err.println("[Redis] Failed to get rank name for " + uuid + ": " + e.getMessage());
            return DEFAULT_RANK_NAME;
        }
    }

    /** Setzt/aktualisiert den (zuletzt bekannten) Spieler-Namen im Hash. */
    public void updatePlayerName(String uuid, String playerName) {
        if (playerName == null) return;
        final String playerKey = getPlayerKey(uuid);
        try (Jedis jedis = pool.getResource()) {
            jedis.hset(playerKey, FIELD_NAME, playerName);
        } catch (Exception e) {
            System.err.println("[Redis] Failed to update player name for " + uuid + ": " + e.getMessage());
        }
    }

    /** Kann null zurückgeben, wenn kein Name im Hash liegt. */
    public String getPlayerName(String uuid) {
        final String playerKey = getPlayerKey(uuid);
        try (Jedis jedis = pool.getResource()) {
            return jedis.hget(playerKey, FIELD_NAME);
        } catch (Exception e) {
            System.err.println("[Redis] Failed to get player name for " + uuid + ": " + e.getMessage());
            return null;
        }
    }

    // ---------- Public: Leaderboard / Stats ----------

    public static final class LeaderboardEntry {
        public final String uuid;
        public final String name;
        public final int elo;
        public LeaderboardEntry(String uuid, String name, int elo) {
            this.uuid = uuid; this.name = name; this.elo = elo;
        }
    }

    /** Holt Top-N mit Namen asynchron und ohne Main-Thread-I/O. */
    public CompletableFuture<List<LeaderboardEntry>> getTopRanksWithNamesAsync(QueueType queueType, int topN) {
        return CompletableFuture.supplyAsync(() -> {
            String leaderboardKey = getLeaderboardKey(queueType);
            List<LeaderboardEntry> out = new ArrayList<>(topN);

            try (Jedis jedis = pool.getResource()) {
                List<Tuple> set = jedis.zrevrangeWithScores(leaderboardKey, 0, topN - 1);
                if (set == null || set.isEmpty()) return out;

                Pipeline p = jedis.pipelined();
                Map<String, Response<String>> nameResp = new LinkedHashMap<>();

                for (Tuple t : set) {
                    String memberKey = t.getElement();         // "player:<uuid>"
                    nameResp.put(memberKey, p.hget(memberKey, FIELD_NAME));
                }
                p.sync();

                for (Tuple t : set) {
                    String memberKey = t.getElement();
                    String uuidNoPrefix = memberKey.substring(KEY_PLAYER_HASH_PREFIX.length());
                    String name = Optional.ofNullable(nameResp.get(memberKey))
                            .map(Response::get).orElse("Unknown");
                    out.add(new LeaderboardEntry(uuidNoPrefix, name, (int) t.getScore()));
                }
            } catch (Exception e) {
                System.err.println("[Redis] getTopRanksWithNamesAsync failed: " + e.getMessage());
            }
            return out;
        }, runnable -> Bukkit.getScheduler().runTaskAsynchronously(ProjectM.getInstance(), runnable));
    }

    public void incrementStat(String uuid, String field, long increment) {
        final String playerKey = getPlayerKey(uuid);
        try (Jedis jedis = pool.getResource()) {
            jedis.hincrBy(playerKey, field, increment);
        } catch (Exception e) {
            System.err.println("[Redis] Failed to increment stat " + field + " for " + uuid + ": " + e.getMessage());
        }
    }

    // ---------- Async Admin Helpers ----------

    public CompletableFuture<String> resolvePlayerAndAddEloAsync(String targetName, QueueType type, int amount) {
        return CompletableFuture.supplyAsync(() -> {
            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) return null;

            String uuid = target.getUniqueId().toString();
            String actualName = target.getName();

            // Lazy init + Name pflegen
            updatePlayerName(uuid, actualName);
            addElo(uuid, type, amount);
            return actualName;
        }, runnable -> Bukkit.getScheduler().runTaskAsynchronously(ProjectM.getInstance(), runnable));
    }

    public CompletableFuture<String> resolvePlayerAndSetEloAsync(String targetName, QueueType type, int newElo) {
        return CompletableFuture.supplyAsync(() -> {
            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) return null;

            String uuid = target.getUniqueId().toString();
            String actualName = target.getName();

            updatePlayerName(uuid, actualName);
            updateElo(uuid, actualName, type, newElo);
            return actualName;
        }, runnable -> Bukkit.getScheduler().runTaskAsynchronously(ProjectM.getInstance(), runnable));
    }

    public CompletableFuture<String> resolvePlayerAndSetRankAsync(String targetName, String rankName) {
        return CompletableFuture.supplyAsync(() -> {
            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) return null;

            String uuid = target.getUniqueId().toString();
            String actualName = target.getName();

            updateRank(uuid, rankName);
            // Name sicher hinterlegen
            if (getPlayerName(uuid) == null) {
                updatePlayerName(uuid, actualName);
            }
            return actualName;
        }, runnable -> Bukkit.getScheduler().runTaskAsynchronously(ProjectM.getInstance(), runnable));
    }

    public CompletableFuture<Map<QueueType, Integer>> getEloStatsAsync(String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Map<QueueType, Integer> stats = new EnumMap<>(QueueType.class);
            for (QueueType type : QueueType.values()) {
                // getElo sorgt bereits für Lazy-Init (Default 1000 in Hash + ZSET)
                stats.put(type, getElo(uuid, type));
            }
            return stats;
        }, runnable -> Bukkit.getScheduler().runTaskAsynchronously(ProjectM.getInstance(), runnable));
    }

    // ---------- Lifecycle ----------

    @Override
    public void close() {
        if (pool != null && !pool.isClosed()) {
            pool.close();
        }
    }
}