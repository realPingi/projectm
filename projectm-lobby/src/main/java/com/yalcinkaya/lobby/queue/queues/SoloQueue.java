package com.yalcinkaya.lobby.queue.queues;

import com.yalcinkaya.core.redis.QueueType;
import com.yalcinkaya.lobby.Lobby;
import com.yalcinkaya.lobby.net.MatchStarter;
import com.yalcinkaya.lobby.queue.Queue;
import com.yalcinkaya.lobby.user.LobbyUser;
import com.yalcinkaya.lobby.util.Place;
import lombok.Data;
import org.bukkit.Location;

import java.util.*;
import java.util.stream.Collectors;

@Data
public class SoloQueue extends Queue<LobbyUser> {

    private int min = 4;

    @Override
    public Set<LobbyUser> findMatch() {
        int neededPlayers = 2 * min;
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
        matchStarter.startMatch(blue, red, matchStarter.selectRandomMap(), QueueType.SOLO);
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
        return Place.QUEUE_SOLO_UNRANKED.getLocation();
    }

}
