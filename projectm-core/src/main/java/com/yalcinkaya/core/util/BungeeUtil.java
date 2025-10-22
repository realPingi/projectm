package com.yalcinkaya.core.util;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class BungeeUtil {

    private static final String PROXY_CHANNEL = "BungeeCord";
    private static final String LOBBY_SERVER_NAME = "lobby";

    public static void sendPlayerToLobby(JavaPlugin plugin, Player player) {
        try (ByteArrayOutputStream b = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(b)) {

            out.writeUTF("Connect");

            out.writeUTF(LOBBY_SERVER_NAME);

            player.sendPluginMessage(plugin, PROXY_CHANNEL, b.toByteArray());

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to send BungeeCord connect message: " + player.getName());
        }
    }
}
