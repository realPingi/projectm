package com.yalcinkaya.lobby.party;

import com.yalcinkaya.lobby.queue.Queueable;
import com.yalcinkaya.lobby.user.LobbyUser;
import lombok.Data;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
public class Party implements Queueable {

    private UUID id;
    private Set<UUID> members = new HashSet<>();

    public Party(UUID... players) {
        id = UUID.randomUUID();
        members.addAll(Arrays.stream(players).toList());
    }
    public Party() {
        id = UUID.randomUUID();
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

    public int size() {
        return members.size();
    }

}

