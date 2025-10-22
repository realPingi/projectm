package com.yalcinkaya.lobby.queue.queues;

import com.yalcinkaya.lobby.Lobby;
import com.yalcinkaya.lobby.net.MatchStarter;
import com.yalcinkaya.lobby.queue.Queue;
import com.yalcinkaya.lobby.user.LobbyUser;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.*;
import java.util.stream.Collectors;

public class SoloQueue extends Queue<LobbyUser> {
    private static final int MIN = 1;

    @Override
    public Set<LobbyUser> findMatch() {
        int neededPlayers = 2 * MIN;
        if (getQueued() >= neededPlayers) {
            return queue.stream().limit(neededPlayers).collect(Collectors.toSet());
        }
        return null;
    }

    @Override
    public void onMatchFound(Set<LobbyUser> matches) {
        super.onMatchFound(matches);
        List<UUID> blue = new ArrayList<>();
        List<UUID> red = new ArrayList<>();

        List<LobbyUser> shuffled = new ArrayList<>(matches);
        Collections.shuffle(shuffled);
        for (int i = 0; i < shuffled.size(); i++) {
            UUID match = shuffled.get(i).getUuid();
            if (i % 2 == 0) {
                blue.add(match);
            } else {
                red.add(match);
            }
        }

        MatchStarter matchStarter = Lobby.getInstance().getMatchStarter();
        matchStarter.startMatch(blue, red, matchStarter.selectRandomMap());
    }

    @Override
    public String getName() {
        return "Solo Queue";
    }

    @Override
    public LobbyUser queuebalize(LobbyUser user) {
        return user;
    }

    @Override
    public Location getPhysicalEntry() {
        return new Location(Bukkit.getWorld("world"), 2.5, -2, 12.5, -180, 0);
    }

}
