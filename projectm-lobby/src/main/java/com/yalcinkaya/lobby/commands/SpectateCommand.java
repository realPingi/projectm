package com.yalcinkaya.lobby.commands;

import com.yalcinkaya.core.redis.Rank;
import com.yalcinkaya.core.util.CoreUtil;
import com.yalcinkaya.core.util.MessageType;
import com.yalcinkaya.lobby.Lobby;
import com.yalcinkaya.lobby.net.MatchLookupService;
import com.yalcinkaya.lobby.user.LobbyUser;
import com.yalcinkaya.lobby.util.LobbyUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SpectateCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            return true;
        }

        LobbyUser user = LobbyUtil.getUser(player);

        if (!Rank.hasPermissions(user, Rank.CONTENT)) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Insufficient permissions."));
            return true;
        }

        if (args.length < 1) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Usage: /spectate <game>"));
            return true;
        }

        String serverName = args[0];

        MatchLookupService lookup = new MatchLookupService();
        try {
            List<MatchLookupService.MatchInfo> matches = lookup.fetchMatches();
            for (MatchLookupService.MatchInfo matchInfo : matches) {
                if (matchInfo.toVelocityServerName().equals(serverName)) {
                    Lobby.getInstance().getMatchStarter().sendPlayerConnect(player, serverName);
                    return true;
                }
            }

            if (matches.isEmpty()) {
                user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "No running matches."));
                return true;
            }

            List<String> serverNames = matches.stream().map(MatchLookupService.MatchInfo::toVelocityServerName).toList();
            List<String> matchInfoMessage = new ArrayList<>();
            matchInfoMessage.add("Invalid server name. Running matches: ");
            serverNames.forEach(s -> {
                matchInfoMessage.add(s);
                matchInfoMessage.add(", ");
            });
            matchInfoMessage.removeLast();
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, matchInfoMessage.toArray(new String[0])));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return true;
    }
}
