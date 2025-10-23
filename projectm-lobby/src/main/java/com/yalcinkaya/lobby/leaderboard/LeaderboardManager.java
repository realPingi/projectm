package com.yalcinkaya.lobby.leaderboard;

import com.yalcinkaya.core.redis.QueueType;
import com.yalcinkaya.lobby.Lobby;
import com.yalcinkaya.lobby.util.Place;
import org.bukkit.Bukkit;

import java.util.Set;

public class LeaderboardManager {

    private final Leaderboard soloLeaderboard = new Leaderboard(QueueType.SOLO, Place.LEADERBOARD_SOLO);
    private final Leaderboard partyLeaderboard = new Leaderboard(QueueType.PARTY, Place.LEADERBOARD_PARTY);
    private final Set<Leaderboard> leaderboards = Set.of(soloLeaderboard, partyLeaderboard);

    public void init() {
        loadLeaderboards();
        Bukkit.getScheduler().runTaskTimer(Lobby.getInstance(), this::updateLeaderboards, 0, 20 * 60 * 5);
    }

    public void loadLeaderboards() {
        leaderboards.forEach(Leaderboard::load);
    }

    public void updateLeaderboards() {
        leaderboards.forEach(Leaderboard::update);
    }
}
