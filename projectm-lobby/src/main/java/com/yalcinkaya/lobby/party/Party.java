package com.yalcinkaya.lobby.party;

import com.yalcinkaya.lobby.queue.Queueable;
import com.yalcinkaya.lobby.user.LobbyUser;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
public class Party implements Queueable {

    private UUID partyId;
    private Set<UUID> members;

    public Party() {
        partyId = UUID.randomUUID();
    }

    public void addMember(LobbyUser user) {
        addMember(user.getUuid());
    }

    public void addMember(UUID uuid) {
        members.add(uuid);
    }

    public void removeMember(LobbyUser user) {
        removeMember(user.getUuid());
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    @Override
    public Set<UUID> getUUIDs() {
        return members;
    }
}
