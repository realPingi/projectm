package com.yalcinkaya.lobby.user;

import com.yalcinkaya.core.user.User;
import com.yalcinkaya.lobby.Lobby;
import com.yalcinkaya.lobby.party.Party;
import com.yalcinkaya.lobby.queue.Queueable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class LobbyUser extends User implements Queueable {

    public LobbyUser(UUID uuid) {
        super(uuid);
    }

    @Override
    public Set<UUID> getUUIDs() {
        return new HashSet<>(Collections.singleton(getUuid()));
    }

    public Party getParty() {
        return Lobby.getInstance().getPartyManager().getParty(this);
    }
}
