package com.yalcinkaya.core.redis;

import com.google.gson.Gson;
import com.yalcinkaya.core.ProjectM;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import redis.clients.jedis.*;
import redis.clients.jedis.params.ZAddParams;
import redis.clients.jedis.resps.Tuple;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class RedisDataService implements AutoCloseable {

    // --- Verbindungskonfiguration ---
    private static final String REDIS_HOST = "redis";
    private static final int REDIS_PORT = 6379;

    // --- Defaults ---
    public static final int STARTING_ELO = 1000;
    private static final String DEFAULT_RANK_NAME = "DEFAULT";

    // --- Redis Keys ---
    private static final String KEY_LEADERBOARD_ELO_BASE = "leaderboard:elo:";   // + <queueKey>
    private static final String KEY_PLAYER_HASH_PREFIX   = "player:";            // + <normalizedUuid>

    // NEW: Name/UUID-Indexe (für Offline-Lookups)
    private static final String KEY_IDX_NAME_TO_UUID = "player_index:name_to_uuid"; // HSET <lower_name> <uuid>
    private static final String KEY_IDX_UUID_TO_NAME = "player_index:uuid_to_name"; // HSET <uuid> <last_seen_name>

    // --- Hash-Felder ---
    private static final String FIELD_ELO_BASE   = "elo:"; // + <queueKey>
    private static final String FIELD_NAME       = "name";
    private static final String FIELD_RANK_NAME  = "rank_name";
    private static final String FIELD_WINS       = "wins";

    private static final Pattern UUID_REGEX =
            Pattern.compile("^[0-9a-fA-F]{8}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{12}$");

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

    private String normalizeUuid(String uuid) {
        return uuid.replace("-", "").toLowerCase(Locale.ROOT);
    }

    private String getPlayerKey(String uuid) {
        return KEY_PLAYER_HASH_PREFIX + normalizeUuid(uuid);
    }

    // ---------- Index Helpers (Name <-> UUID) ----------

    /** Speichert/aktualisiert bidirektionalen Index. */
    private void indexPlayer(Jedis jedis, String uuid, String playerName) {
        if (uuid == null || playerName == null || playerName.isEmpty()) return;
        String u = uuid.toLowerCase(Locale.ROOT);
        String n = playerName.toLowerCase(Locale.ROOT);
        jedis.hset(KEY_IDX_NAME_TO_UUID, n, u);
        jedis.hset(KEY_IDX_UUID_TO_NAME, u, playerName); // letzter bekannter Name
    }

    /** Versucht zuerst Redis, dann Bukkit-Offline, um aus Name -> UUID zu kommen. */
    public Optional<String> getUuidByName(String targetName) {
        if (targetName == null || targetName.isEmpty()) return Optional.empty();
        String lower = targetName.toLowerCase(Locale.ROOT);

        try (Jedis jedis = pool.getResource()) {
            String uuid = jedis.hget(KEY_IDX_NAME_TO_UUID, lower);
            if (uuid != null && !uuid.isEmpty()) return Optional.of(uuid);

            // Fallback: Bukkit Offline (aufgrund deiner Async-Wrapper okay, kann blockieren)
            OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
            if (op != null && (op.isOnline() || op.hasPlayedBefore())) {
                String u = op.getUniqueId().toString();
                // Index auffüllen
                indexPlayer(jedis, u, Optional.ofNullable(op.getName()).orElse(targetName));
                return Optional.of(u);
            }
        } catch (Exception e) {
            System.err.println("[Redis] getUuidByName failed: " + e.getMessage());
        }
        return Optional.empty();
    }

    /** Name für UUID – zuerst Redis, dann Bukkit-Offline. */
    public Optional<String> getNameByUuid(String uuid) {
        if (uuid == null || uuid.isEmpty()) return Optional.empty();
        String u = uuid.toLowerCase(Locale.ROOT);
        try (Jedis jedis = pool.getResource()) {
            String name = jedis.hget(KEY_IDX_UUID_TO_NAME, u);
            if (name != null && !name.isEmpty()) return Optional.of(name);
            try {
                OfflinePlayer op = Bukkit.getOfflinePlayer(java.util.UUID.fromString(uuid));
                if (op != null) {
                    String resolved = op.getName();
                    if (resolved != null && !resolved.isEmpty()) {
                        indexPlayer(jedis, uuid, resolved);
                        return Optional.of(resolved);
                    }
                }
            } catch (IllegalArgumentException ignored) {
                // uuid war kein valides UUID-Format
            }
        } catch (Exception e) {
            System.err.println("[Redis] getNameByUuid failed: " + e.getMessage());
        }
        return Optional.empty();
    }

    // ---------- Bootstrap / Ensure Defaults ----------

    /**
     * Initialisiert (falls nicht vorhanden) ELO und Name im Spieler-Hash
     * und den passenden Leaderboard-Eintrag. Komplett idempotent.
     */
    private void ensureDefaultForQueue(Jedis jedis, String uuid, String playerName, QueueType queueType) {
        final String playerKey      = getPlayerKey(uuid);
        final String eloField       = getEloField(queueType);
        final String leaderboardKey = getLeaderboardKey(queueType);

        // Name nur setzen, wenn nicht vorhanden
        if (playerName != null && !playerName.isEmpty()) {
            jedis.hsetnx(playerKey, FIELD_NAME, playerName);
            indexPlayer(jedis, uuid, playerName); // NEW: Index pflegen
        }

        // ELO-Hashfeld: wenn fehlt -> auf 1000 setzen
        Long created = jedis.hsetnx(playerKey, eloField, String.valueOf(STARTING_ELO));

        // Leaderboard-Eintrag nur anlegen, wenn neu (NX)
        try {
            ZAddParams params = ZAddParams.zAddParams().nx();
            jedis.zadd(leaderboardKey, STARTING_ELO, playerKey, params);
        } catch (NoSuchMethodError | Exception ignored) {
            if (created != null && created == 1L) {
                jedis.zadd(leaderboardKey, STARTING_ELO, playerKey);
            }
        }
    }

    /** Initialisiert einen Spieler für ALLE Queues (praktisch beim ersten Join). */
    public void bootstrapAllQueues(String uuid, String playerName) {
        try (Jedis jedis = pool.getResource()) {
            if (playerName != null) indexPlayer(jedis, uuid, playerName); // NEW
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
            ensureDefaultForQueue(jedis, uuid, playerName, queueType);

            Transaction t = jedis.multi();
            t.hset(playerKey, eloField, String.valueOf(newElo));
            if (playerName != null && !playerName.isEmpty()) {
                t.hset(playerKey, FIELD_NAME, playerName);
                t.hset(KEY_IDX_UUID_TO_NAME, uuid.toLowerCase(Locale.ROOT), playerName); // NEW
                t.hset(KEY_IDX_NAME_TO_UUID, playerName.toLowerCase(Locale.ROOT), uuid.toLowerCase(Locale.ROOT)); // NEW
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
            if (!jedis.hexists(playerKey, eloField)) {
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
            if (!jedis.hexists(playerKey, eloField)) {
                String name = jedis.hget(playerKey, FIELD_NAME);
                ensureDefaultForQueue(jedis, uuid, name, queueType);
            }

            String currentEloString = jedis.hget(playerKey, eloField);
            int currentElo = (currentEloString != null) ? Integer.parseInt(currentEloString) : STARTING_ELO;
            newElo = currentElo + delta;

            Transaction t = jedis.multi();
            Response<Double> newScore = t.zincrby(leaderboardKey, delta, playerKey);
            Response<Long>   hsetRes  = t.hset(playerKey, eloField, String.valueOf(newElo));
            List<Object> exec = t.exec();

            if (exec == null) {
                System.err.printf("[Redis] addElo EXEC returned null for %s (%s)%n", uuid, queueType.name());
            } else {
                System.out.printf("[Redis] addElo OK %s (%s): newScore=%s, hsetRes=%s, newElo=%d%n",
                        uuid, queueType.name(), newScore.get(), hsetRes.get(), newElo);
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

    /** Setzt/aktualisiert den (zuletzt bekannten) Spieler-Namen im Hash + Index. */
    public void updatePlayerName(String uuid, String playerName) {
        if (playerName == null) return;
        final String playerKey = getPlayerKey(uuid);
        try (Jedis jedis = pool.getResource()) {
            jedis.hset(playerKey, FIELD_NAME, playerName);
            indexPlayer(jedis, uuid, playerName); // NEW
        } catch (Exception e) {
            System.err.println("[Redis] Failed to update player name for " + uuid + ": " + e.getMessage());
        }
    }

    /** Kann null zurückgeben, wenn kein Name im Hash liegt. */
    public String getPlayerName(String uuid) {
        final String playerKey = getPlayerKey(uuid);
        try (Jedis jedis = pool.getResource()) {
            String name = jedis.hget(playerKey, FIELD_NAME);
            if (name != null) return name;
        } catch (Exception e) {
            System.err.println("[Redis] Failed to get player name for " + uuid + ": " + e.getMessage());
        }
        // Fallback: Index/Bukkit
        return getNameByUuid(uuid).orElse(null);
    }

    // ---------- Public: Leaderboard / Stats ----------

    public static final class LeaderboardEntry {
        public final String uuid; // normalized (ohne Bindestriche)
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
                    String memberKey = t.getElement();         // "player:<normalized-uuid>"
                    nameResp.put(memberKey, p.hget(memberKey, FIELD_NAME));
                }
                p.sync();

                for (Tuple t : set) {
                    String memberKey = t.getElement();
                    String uuidNoPrefix = memberKey.substring(KEY_PLAYER_HASH_PREFIX.length()); // normalized uuid
                    String name = Optional.ofNullable(nameResp.get(memberKey))
                            .map(Response::get).orElse(null);

                    if (name == null || name.isEmpty()) {
                        // Versuch, über Index einen Namen zu finden
                        String dashed = uuidNoPrefix.replaceFirst(
                                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})",
                                "$1-$2-$3-$4-$5");
                        name = getNameByUuid(dashed).orElse("Unknown");
                    }
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

    // ---------- Async Admin Helpers (OFFLINE-FÄHIG) ----------

    /**
     * Löst einen beliebigen Target-String in (uuid, name) auf.
     * - UUID (mit/ohne Bindestriche) wird direkt akzeptiert
     * - Sonst: Name -> UUID via Redis-Index, dann Bukkit-Offline Fallback
     */
    private Optional<AbstractMap.SimpleEntry<String, String>> resolveTarget(String target) {
        if (target == null || target.isEmpty()) return Optional.empty();

        // 1) Wenn es bereits wie eine UUID aussieht
        if (UUID_REGEX.matcher(target).matches()) {
            String dashed = target.contains("-") ? target : insertUuidDashes(target.toLowerCase(Locale.ROOT));
            String name = getNameByUuid(dashed).orElse(null);
            return Optional.of(new AbstractMap.SimpleEntry<>(dashed, name != null ? name : dashed));
        }

        // 2) Online first (schnell)
        Player p = Bukkit.getPlayerExact(target);
        if (p != null) {
            String u = p.getUniqueId().toString();
            String n = p.getName();
            try (Jedis jedis = pool.getResource()) { indexPlayer(jedis, u, n); }
            return Optional.of(new AbstractMap.SimpleEntry<>(u, n));
        }

        // 3) Redis Index
        Optional<String> byIdx = getUuidByName(target);
        if (byIdx.isPresent()) {
            String u = byIdx.get();
            String n = getNameByUuid(u).orElse(target);
            return Optional.of(new AbstractMap.SimpleEntry<>(u, n));
        }

        // 4) Bukkit Offline Fallback
        OfflinePlayer op = Bukkit.getOfflinePlayer(target);
        if (op != null && (op.isOnline() || op.hasPlayedBefore())) {
            String u = op.getUniqueId().toString();
            String n = Optional.ofNullable(op.getName()).orElse(target);
            try (Jedis jedis = pool.getResource()) { indexPlayer(jedis, u, n); }
            return Optional.of(new AbstractMap.SimpleEntry<>(u, n));
        }

        return Optional.empty();
    }

    /** Fügt Bindestriche in eine 32er-UUID ein. */
    private String insertUuidDashes(String normalized) {
        if (normalized == null || normalized.length() != 32) return normalized;
        return normalized.substring(0,8) + "-" +
                normalized.substring(8,12) + "-" +
                normalized.substring(12,16) + "-" +
                normalized.substring(16,20) + "-" +
                normalized.substring(20);
    }

    public CompletableFuture<String> resolvePlayerAndAddEloAsync(String targetNameOrUuid, QueueType type, int amount) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<AbstractMap.SimpleEntry<String, String>> resolved = resolveTarget(targetNameOrUuid);
            if (resolved.isEmpty()) return null;

            String uuid = resolved.get().getKey();
            String actualName = resolved.get().getValue();

            updatePlayerName(uuid, actualName); // pflegt auch Index
            addElo(uuid, type, amount);
            return actualName;
        }, runnable -> Bukkit.getScheduler().runTaskAsynchronously(ProjectM.getInstance(), runnable));
    }

    public CompletableFuture<String> resolvePlayerAndSetEloAsync(String targetNameOrUuid, QueueType type, int newElo) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<AbstractMap.SimpleEntry<String, String>> resolved = resolveTarget(targetNameOrUuid);
            if (resolved.isEmpty()) return null;

            String uuid = resolved.get().getKey();
            String actualName = resolved.get().getValue();

            updatePlayerName(uuid, actualName);
            updateElo(uuid, actualName, type, newElo);
            return actualName;
        }, runnable -> Bukkit.getScheduler().runTaskAsynchronously(ProjectM.getInstance(), runnable));
    }

    public CompletableFuture<String> resolvePlayerAndSetRankAsync(String targetNameOrUuid, String rankName) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<AbstractMap.SimpleEntry<String, String>> resolved = resolveTarget(targetNameOrUuid);
            if (resolved.isEmpty()) return null;

            String uuid = resolved.get().getKey();
            String actualName = resolved.get().getValue();

            updateRank(uuid, rankName);
            if (getPlayerName(uuid) == null) {
                updatePlayerName(uuid, actualName);
            }
            return actualName;
        }, runnable -> Bukkit.getScheduler().runTaskAsynchronously(ProjectM.getInstance(), runnable));
    }

    public CompletableFuture<Map<QueueType, Integer>> getEloStatsAsync(String uuidOrName) {
        return CompletableFuture.supplyAsync(() -> {
            // Akzeptiere UUID oder Name
            String uuid = UUID_REGEX.matcher(uuidOrName).matches()
                    ? (uuidOrName.contains("-") ? uuidOrName : insertUuidDashes(uuidOrName.toLowerCase(Locale.ROOT)))
                    : getUuidByName(uuidOrName).orElse(null);

            if (uuid == null) return Collections.emptyMap();

            Map<QueueType, Integer> stats = new EnumMap<>(QueueType.class);
            for (QueueType type : QueueType.values()) {
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
