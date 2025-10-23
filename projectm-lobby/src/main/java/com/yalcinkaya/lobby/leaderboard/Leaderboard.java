package com.yalcinkaya.lobby.leaderboard;

import com.yalcinkaya.core.ProjectM;
import com.yalcinkaya.core.redis.QueueType;
import com.yalcinkaya.core.redis.RedisDataService;
import com.yalcinkaya.lobby.util.Place;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Leaderboard {

    private Hologram hologram;
    private final QueueType queueType;
    private final Place place;

    private static final int MAX_RANKS = 10;
    private static final DecimalFormat ELO_FORMAT = new DecimalFormat("#,##0.0");

    private String holoId() {
        return "lb_" + queueType.getRedisKey().toLowerCase(Locale.ROOT);
    }

    public Leaderboard(QueueType queueType, Place place) {
        this.queueType = queueType;
        this.place = place;
    }

    public void load() {
        Location location = place.getLocation();
        Bukkit.getScheduler().runTask(ProjectM.getInstance(), () -> {
            if (location.getWorld() == null) {
                ProjectM.getInstance().getLogger().warning("[Leaderboard] World ist null für " + queueType);
                return;
            }

            // Stelle sicher, dass der Chunk geladen ist
            var chunk = location.getChunk();
            if (!chunk.isLoaded()) {
                chunk.load(true);
            }

            List<String> lines = new ArrayList<>();
            lines.add(ChatColor.GOLD + queueType.getRedisKey().toUpperCase(Locale.ROOT) + " LEADERBOARD");
            for (int i = 1; i <= MAX_RANKS; i++) {
                lines.add(ChatColor.YELLOW + "#" + i + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + "Lade...");
            }

            hologram = DHAPI.createHologram(holoId(), location, lines);

            Bukkit.getScheduler().runTaskLater(ProjectM.getInstance(), this::update, 2L);
        });
    }

    public void update() {
        ProjectM.getInstance().getRedisDataService()
                .getTopRanksWithNamesAsync(queueType, MAX_RANKS)
                .thenAccept(rows -> Bukkit.getScheduler().runTask(ProjectM.getInstance(), () -> updateHologramLines(rows)));
    }

    private void updateHologramLines(List<RedisDataService.LeaderboardEntry> rows) {
        if (hologram == null) {
            ProjectM.getInstance().getLogger().warning("[Leaderboard] updateHologramLines aufgerufen, aber hologram == null");
            return;
        }

        int rank = 1;
        for (RedisDataService.LeaderboardEntry row : rows) {
            if (rank > MAX_RANKS) break;

            String line = ChatColor.YELLOW + "#" + rank + ChatColor.DARK_GRAY + " | " +
                    ChatColor.WHITE + (row.name != null ? row.name : "Unknown") + ChatColor.DARK_GRAY + " (" +
                    ChatColor.GOLD + ELO_FORMAT.format(row.elo) + ChatColor.DARK_GRAY + ")";

            // 0 ist die Titelzeile – Rangzeilen beginnen ab Index 1
            DHAPI.setHologramLine(hologram, rank, line);
            rank++;
        }

        // restliche Plätze auffüllen
        while (rank <= MAX_RANKS) {
            DHAPI.setHologramLine(
                    hologram,
                    rank,
                    ChatColor.YELLOW + "#" + rank + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + "N/A"
            );
            rank++;
        }
    }
}

