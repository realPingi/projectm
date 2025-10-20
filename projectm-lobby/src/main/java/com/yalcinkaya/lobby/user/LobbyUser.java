package com.yalcinkaya.lobby.user;

import com.yalcinkaya.lobby.party.Party;
import com.yalcinkaya.lobby.queue.Queueable;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
public class LobbyUser implements Queueable {

    private UUID uuid;
    private Party party;

    public LobbyUser(UUID uuid) {
        this.uuid = uuid;
    }

    public void sendMessage(String message) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            player.sendMessage(message);
        }
    }

    @Override
    public Set<UUID> getUUIDs() {
        return new HashSet<>(Collections.singleton(uuid));
    }
}
