package com.yalcinkaya.lobby.leaderboard;

import com.yalcinkaya.core.ProjectM;
import com.yalcinkaya.core.redis.QueueType;
import com.yalcinkaya.core.redis.Rank;
import com.yalcinkaya.core.redis.RedisDataService;
import com.yalcinkaya.core.util.CoreUtil;
import com.yalcinkaya.lobby.util.Place;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class Leaderboard {

    private static final int MAX_RANKS = 10;
    private final QueueType queueType;
    private final Place place;
    private Hologram hologram;

    public Leaderboard(QueueType queueType, Place place) {
        this.queueType = queueType;
        this.place = place;
    }

    private String holoId() {
        return "lb_" + queueType.getRedisKey().toLowerCase(Locale.ROOT);
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
            lines.add(ChatColor.GOLD + "--- " + CoreUtil.camelizeString(queueType.getRedisKey()) + " Leaderboard ---");
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

            // Rangnamen sicher in Enum mappen
            Rank r;
            try {
                r = Rank.valueOf(Optional.ofNullable(row.rankName)
                        .orElse("default").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                r = Rank.DEFAULT;
            }

            ChatColor prefix = r == Rank.DEFAULT ? ChatColor.WHITE : r.getLegacyColor();

            // Beispiel: Farbe des Rangs vor den Spielernamen setzen
            String coloredName = prefix + (row.name != null ? row.name : "Unknown") + ChatColor.RESET;

            String line = ChatColor.YELLOW + "#" + rank + ChatColor.DARK_GRAY + " | "
                    + coloredName + ChatColor.DARK_GRAY + " ("
                    + ChatColor.GOLD + row.elo + ChatColor.DARK_GRAY + ")";

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

