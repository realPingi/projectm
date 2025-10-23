package com.yalcinkaya.lobby.leaderboard;

import com.yalcinkaya.core.ProjectM;
import com.yalcinkaya.core.redis.QueueType;
import com.yalcinkaya.lobby.Lobby;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class Leaderboard {

    private Hologram hologram;
    private final QueueType queueType;
    private final Location location;

    private static final int MAX_RANKS = 10;
    private static final DecimalFormat ELO_FORMAT = new DecimalFormat("#,##0.0");

    public Leaderboard(QueueType queueType, Location location) {
        this.queueType = queueType;
        this.location = location;
    }

    public void load() {
        List<String> lines = new ArrayList<>();
        lines.add(queueType.getRedisKey().toUpperCase(Locale.ROOT) + " LEADERBOARD");

        for (int i = 1; i <= MAX_RANKS; i++) {
            lines.add("#" + i + ": Lade...");
        }

        hologram = DHAPI.createHologram(queueType.getRedisKey(), location, lines);

        update();
    }

    public void update() {
        CompletableFuture.supplyAsync(() -> ProjectM.getInstance().getRedisDataService().getTopRanks(queueType, MAX_RANKS))
                .thenAccept(ranks -> Bukkit.getScheduler().runTask(Lobby.getInstance(), () -> updateHologramLines(ranks)));
    }

    private void updateHologramLines(Map<String, Double> ranks) {

        List<Map.Entry<String, Double>> sortedRanks = new ArrayList<>(ranks.entrySet());

        int rank = 1;
        for (Map.Entry<String, Double> entry : sortedRanks) {
            if (rank > MAX_RANKS) break;

            String uuidStr = entry.getKey();
            double elo = entry.getValue();

            String name = ProjectM.getInstance().getRedisDataService().getPlayerName(uuidStr);

            String line = ChatColor.YELLOW + "#" + rank + ChatColor.DARK_GRAY + " | " +
                    ChatColor.WHITE + name + ChatColor.DARK_GRAY + " (" +
                    ChatColor.GOLD + ELO_FORMAT.format(elo) + ChatColor.DARK_GRAY + ")";

            DHAPI.setHologramLine(hologram, rank, line);
            rank++;
        }

        while (rank <= MAX_RANKS) {
            DHAPI.setHologramLine(hologram, rank, ChatColor.YELLOW + "#" + rank + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + "N/A");
            rank++;
        }
    }
}
