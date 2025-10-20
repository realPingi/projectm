package com.yalcinkaya.lobby.util;

import com.yalcinkaya.lobby.Lobby;
import com.yalcinkaya.lobby.hotbar.LobbyHotbarGUI;
import com.yalcinkaya.lobby.user.LobbyUser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.regex.Pattern;

public class LobbyUtil {

    public static final String LINEBREAK = "\n"; // or "\r\n";

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

    public static String getLobbyMessage(MessageType messageType, String... strings) {
        return getColoredString(messageType.getPrefix(), messageType.getFormat(), strings);
    }

    public static String getColoredString(String prefix, String defaultString, String... strings) {
        String coloredString = prefix + defaultString + "";
        for (String string : strings) {
            coloredString += string + defaultString;
        }
        return coloredString;
    }

    public static void broadcastMessageForAll(MessageType messageType, String... strings) {
        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(getLobbyMessage(messageType, strings)));
    }

    public static String wrap(String string, int lineLength) {
        StringBuilder b = new StringBuilder();
        for (String line : string.split(Pattern.quote(LINEBREAK))) {
            b.append(wrapLine(line, lineLength));
        }
        return b.toString();
    }

    private static String wrapLine(String line, int lineLength) {
        if (line.length() == 0) return LINEBREAK;
        if (line.length() <= lineLength) return line + LINEBREAK;
        String[] words = line.split(" ");
        StringBuilder allLines = new StringBuilder();
        StringBuilder trimmedLine = new StringBuilder();
        for (String word : words) {
            if (trimmedLine.length() + 1 + word.length() <= lineLength) {
                trimmedLine.append(word).append(" ");
            } else {
                allLines.append(trimmedLine).append(LINEBREAK);
                trimmedLine = new StringBuilder();
                trimmedLine.append(word).append(" ");
            }
        }
        if (trimmedLine.length() > 0) {
            allLines.append(trimmedLine);
        }
        allLines.append(LINEBREAK);
        return allLines.toString();
    }

    public static int slotFromRowCol(int row, int col) {
        return row * 9 + col;
    }
}
