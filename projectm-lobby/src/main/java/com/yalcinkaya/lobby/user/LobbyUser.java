package com.yalcinkaya.lobby.user;

import com.yalcinkaya.lobby.menu.Menu;
import com.yalcinkaya.lobby.party.Party;
import com.yalcinkaya.lobby.queue.queueables.SingleQueueable;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

@Data
public class LobbyUser implements SingleQueueable {

    private UUID uuid;
    private Party party;
    private Menu menu;

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
    public UUID getUUID() {
        return uuid;
    }
}
