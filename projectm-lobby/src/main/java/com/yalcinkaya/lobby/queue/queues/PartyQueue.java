package com.yalcinkaya.lobby.queue.queues;

import com.yalcinkaya.core.util.CoreUtil;
import com.yalcinkaya.core.util.MessageType;
import com.yalcinkaya.lobby.Lobby;
import com.yalcinkaya.lobby.net.MatchStarter;
import com.yalcinkaya.lobby.party.Party;
import com.yalcinkaya.lobby.queue.Queue;
import com.yalcinkaya.lobby.user.LobbyUser;
import com.yalcinkaya.lobby.util.Place;
import org.bukkit.Location;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class PartyQueue extends Queue<Party> {

    @Override
    public Set<Party> findMatch() {
        if (getQueued() >= 2) {
            return queue.stream().limit(2).collect(Collectors.toSet());
        }
        return null;
    }

    @Override
    public void onMatchFound(Set<Party> matches) {
        super.onMatchFound(matches);
        Iterator<Party> iterator = matches.iterator();
        List<UUID> blue = iterator.next().getMembers().stream().toList();
        List<UUID> red = iterator.next().getMembers().stream().toList();
        MatchStarter matchStarter = Lobby.getInstance().getMatchStarter();
        matchStarter.startMatch(blue, red, matchStarter.selectRandomMap());
    }

    @Override
    public String getName() {
        return "Party Queue";
    }

    @Override
    public Party queuebalize(LobbyUser user) {
        Party party = user.getParty();
        if (party == null) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "You must create a party first."));
            return null;
        }
        if (party.size() != getPartySize()) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Invalid party size. You need ", "" + getPartySize(), " members."));
            return null;
        }
        return party;
    }

    @Override
    public Location getPhysicalEntry() {
        return Place.QUEUE_PARTY_UNRANKED.getLocation();
    }

    public int getPartySize() {
        return 5;
    }
}
