package com.yalcinkaya.core.redis;

import com.yalcinkaya.core.ProjectM;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class RedisDataService {

    private static final String REDIS_HOST = "redis";
    private static final int REDIS_PORT = 6379;

    private static final String KEY_LEADERBOARD_ELO_BASE = "leaderboard:elo:";
    private static final String KEY_PLAYER_HASH_PREFIX = "player:";

    private static final String FIELD_ELO_BASE = "elo:";
    private static final String FIELD_NAME = "name";

    private static final String FIELD_RANK_NAME = "rank_name";

    private static final String FIELD_WINS = "wins";

    private static final double STARTING_ELO = 1000.0;

    private static final String DEFAULT_RANK_NAME = "DEFAULT";

    private Jedis jedis;

    public RedisDataService() {
        try {
            this.jedis = new Jedis(REDIS_HOST, REDIS_PORT);
            this.jedis.ping();
            System.out.println("[Redis] Connection successful to " + REDIS_HOST);
        } catch (Exception e) {
            System.err.println("[Redis] Connection failed: " + e.getMessage());
            this.jedis = null;
        }
    }

    private String getLeaderboardKey(QueueType queueType) {
        return KEY_LEADERBOARD_ELO_BASE + queueType.getRedisKey();
    }

    private String getEloField(QueueType queueType) {
        return FIELD_ELO_BASE + queueType.getRedisKey();
    }

    private String getPlayerKey(String uuid) {
        return KEY_PLAYER_HASH_PREFIX + uuid.replace("-", "").toLowerCase(Locale.ROOT);
    }

    public void updateElo(String uuid, String playerName, QueueType queueType, double newElo) {
        if (jedis == null) return;
        String playerKey = getPlayerKey(uuid);
        String leaderboardKey = getLeaderboardKey(queueType);
        String eloField = getEloField(queueType);

        try {
            Transaction transaction = jedis.multi();

            transaction.hset(playerKey, eloField, String.valueOf(newElo));
            transaction.hset(playerKey, FIELD_NAME, playerName);

            transaction.zadd(leaderboardKey, newElo, playerKey);

            transaction.exec();
        } catch (Exception e) {
            System.err.println("[Redis] Failed to update ELO for " + uuid + " (" + queueType.getRedisKey() + "): " + e.getMessage());
        }
    }

    public void updateRank(String uuid, String rankName) {
        if (jedis == null) return;
        String playerKey = getPlayerKey(uuid);

        try {
            jedis.hset(playerKey, FIELD_RANK_NAME, rankName);
        } catch (Exception e) {
            System.err.println("[Redis] Failed to update rank for " + uuid + ": " + e.getMessage());
        }
    }

    /**
     * Ruft den Rang-Enum-Eintrag eines Spielers ab.
     * @param uuid Die UUID des Spielers.
     * @return Der Rang-Enum-Eintrag oder Rank.DEFAULT, wenn kein Eintrag gefunden wird.
     */
    public Rank getRank(String uuid) {
        String rankName = getRankName(uuid);

        try {
            return Rank.valueOf(rankName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Rank.DEFAULT;
        }
    }

    public String getRankName(String uuid) {
        if (jedis == null) return DEFAULT_RANK_NAME;
        String playerKey = getPlayerKey(uuid);

        try {
            String rank = jedis.hget(playerKey, FIELD_RANK_NAME);
            // KORREKTUR: Wenn der Wert null ist, geben wir den Standardrang zurück.
            return rank != null ? rank : DEFAULT_RANK_NAME;
        } catch (Exception e) {
            System.err.println("[Redis] Failed to get rank name for " + uuid + ": " + e.getMessage());
            return DEFAULT_RANK_NAME;
        }
    }

    public CompletableFuture<String> resolvePlayerAndSetRankAsync(String targetName, String rankName) {
        return CompletableFuture.supplyAsync(() -> {
            Player target = Bukkit.getPlayerExact(targetName);
            String uuid;
            String actualName;

            if (target != null) {
                uuid = target.getUniqueId().toString();
                actualName = target.getName();
            } else {
                return null;
            }

            updateRank(uuid, rankName);
            return actualName;
        }, runnable -> Bukkit.getScheduler().runTaskAsynchronously(ProjectM.getInstance(), runnable));
    }

    public double getElo(String uuid, QueueType queueType) {
        if (jedis == null) return STARTING_ELO;
        String playerKey = getPlayerKey(uuid);
        String eloField = getEloField(queueType);

        try {
            String eloStr = jedis.hget(playerKey, eloField);
            return eloStr != null ? Double.parseDouble(eloStr) : STARTING_ELO;
        } catch (Exception e) {
            System.err.println("[Redis] Failed to get ELO for " + uuid + " (" + queueType.getRedisKey() + "): " + e.getMessage());
            return STARTING_ELO;
        }
    }

    public CompletableFuture<Map<QueueType, Double>> getEloStatsAsync(String uuidString) {
        // Starte den asynchronen Task
        return CompletableFuture.supplyAsync(() -> {
            Map<QueueType, Double> stats = new HashMap<>();

            // Iteriere über alle definierten QueueTypes
            for (QueueType type : QueueType.values()) {
                double elo = getElo(uuidString, type);
                // Nur anzeigen, wenn das ELO vom STARTING_ELO abweicht
                if (elo != RedisDataService.STARTING_ELO) {
                    stats.put(type, elo);
                }
            }
            return stats;
        }, runnable -> Bukkit.getScheduler().runTaskAsynchronously(ProjectM.getInstance(), runnable));
    }

    public CompletableFuture<String> resolvePlayerAndSetEloAsync(String targetName, QueueType type, double newElo) {
        return CompletableFuture.supplyAsync(() -> {
            Player target = Bukkit.getPlayerExact(targetName);
            String uuid;

            if (target != null) {
                uuid = target.getUniqueId().toString();
            } else {
                // Hier müsste die Auflösung für Offline-Spieler erfolgen
                return null;
            }

            // Hole den Spielernamen für das Speichern im Hash und die Ausgabe
            String actualName = target.getName();

            // Speicherung asynchron durchführen
            updateElo(uuid, actualName, type, newElo);
            return actualName;
        }, runnable -> Bukkit.getScheduler().runTaskAsynchronously(ProjectM.getInstance(), runnable));
    }

    public CompletableFuture<String> resolvePlayerAndAddEloAsync(String targetName, QueueType type, double amount) {
        return CompletableFuture.supplyAsync(() -> {
            Player target = Bukkit.getPlayerExact(targetName);
            String uuid;

            if (target != null) {
                uuid = target.getUniqueId().toString();
            } else {
                // Hier müsste die Auflösung für Offline-Spieler erfolgen
                return null;
            }

            // 1. Aktuelles ELO abrufen
            double currentElo = getElo(uuid, type);
            // 2. Neues ELO berechnen
            double newElo = currentElo + amount;

            // 3. ELO speichern (updateElo speichert den Namen und das neue ELO)
            String actualName = target.getName();
            updateElo(uuid, actualName, type, newElo);

            return actualName;
        }, runnable -> Bukkit.getScheduler().runTaskAsynchronously(ProjectM.getInstance(), runnable));
    }

    public String getPlayerName(String uuid) {
        if (jedis == null) return null;
        String playerKey = getPlayerKey(uuid);

        try {
            return jedis.hget(playerKey, FIELD_NAME);
        } catch (Exception e) {
            System.err.println("[Redis] Failed to get player name for " + uuid + ": " + e.getMessage());
            return ChatColor.GRAY + "Unknown player";
        }
    }

    public Map<String, Double> getTopRanks(QueueType queueType, int topN) {
        if (jedis == null) return new HashMap<>();
        String leaderboardKey = getLeaderboardKey(queueType);

        List<String> members = jedis.zrevrange(leaderboardKey, 0, topN - 1);
        Map<String, Double> ranks = new HashMap<>();

        for (String member : members) {
            try {
                Double score = jedis.zscore(leaderboardKey, member);
                if (score != null) {
                    String uuidWithoutPrefix = member.substring(KEY_PLAYER_HASH_PREFIX.length());
                    ranks.put(uuidWithoutPrefix, score);
                }
            } catch (Exception e) {
            }
        }
        return ranks;
    }

    public void incrementStat(String uuid, String field, long increment) {
        if (jedis == null) return;
        String playerKey = getPlayerKey(uuid);

        try {
            jedis.hincrBy(playerKey, field, increment);
        } catch (Exception e) {
            System.err.println("[Redis] Failed to increment stat " + field + " for " + uuid + ": " + e.getMessage());
        }
    }
}

