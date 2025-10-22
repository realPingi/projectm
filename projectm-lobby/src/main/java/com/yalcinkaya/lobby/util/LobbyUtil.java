package com.yalcinkaya.lobby.util;

import com.yalcinkaya.lobby.Lobby;
import com.yalcinkaya.lobby.hotbar.LobbyHotbarGUI;
import com.yalcinkaya.lobby.user.LobbyUser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class LobbyUtil {

    public static LobbyUser getUser(Player player) {
        return getUser(player.getUniqueId());
    }

    public static LobbyUser getUser(UUID uuid) {
        return Lobby.getInstance().getUserManager().getUser(uuid);
    }

    public static Player getPlayer(LobbyUser user) {
        return Bukkit.getPlayer(user.getUuid());
    }

    public static Player getPlayer(UUID uuid) {
        return Bukkit.getPlayer(uuid);
    }

    public static void giveLobbyItems(Player player) {
        LobbyHotbarGUI queueHotbarGUI = Lobby.getInstance().getHotbarManager().getLobbyHotbarGUI();
        queueHotbarGUI.supply(player);
    }

}
