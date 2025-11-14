package com.yalcinkaya.lobby.commands;

import com.yalcinkaya.core.redis.QueueType;
import com.yalcinkaya.core.redis.Rank;
import com.yalcinkaya.core.util.CoreUtil;
import com.yalcinkaya.core.util.MessageType;
import com.yalcinkaya.lobby.Lobby;
import com.yalcinkaya.lobby.net.MatchStarter;
import com.yalcinkaya.lobby.party.Party;
import com.yalcinkaya.lobby.party.PartyManager;
import com.yalcinkaya.lobby.queue.QueueManager;
import com.yalcinkaya.lobby.user.LobbyUser;
import com.yalcinkaya.lobby.util.LobbyUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PartyMatchCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            return true;
        }

        LobbyUser user = LobbyUtil.getUser(player);

        if (!Rank.hasPermissions(user, Rank.OWNER)) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Insufficient permissions."));
            return true;
        }

        if (args.length < 2) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Usage: /partymatch <leaderBlue> <leaderRed>"));
            return true;
        }

        Player leaderBlue = Bukkit.getOnlinePlayers().stream().filter(p -> p.getName().equalsIgnoreCase(args[0])).findFirst().orElse(null);
        Player leaderRed = Bukkit.getOnlinePlayers().stream().filter(p -> p.getName().equalsIgnoreCase(args[1])).findFirst().orElse(null);

        if (leaderBlue == null || leaderRed == null) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Invalid player names."));
            return true;
        }

        PartyManager partyManager = Lobby.getInstance().getPartyManager();
        UUID blueId = leaderBlue.getUniqueId();
        UUID redId = leaderRed.getUniqueId();
        if (!partyManager.hasParty(blueId) || !partyManager.hasParty(redId)) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Party registration incomplete."));
            return true;
        }

        Party blueParty = partyManager.getParty(blueId);
        Party redParty = partyManager.getParty(redId);

        QueueManager queueManager = Lobby.getInstance().getQueueManager();
        blueParty.getMembers().forEach(uuid -> queueManager.unqueue(LobbyUtil.getUser(uuid)));
        redParty.getMembers().forEach(uuid -> queueManager.unqueue(LobbyUtil.getUser(uuid)));

        MatchStarter matchStarter = Lobby.getInstance().getMatchStarter();
        matchStarter.startMatch(blueParty.getMembers().stream().toList(), redParty.getMembers().stream().toList(), matchStarter.selectRandomMap(), QueueType.CUSTOM);

        Bukkit.getOnlinePlayers().forEach(p -> {
            LobbyUser u = LobbyUtil.getUser(p);
            if (u != null) {
                u.sendMessage(CoreUtil.getMessage(MessageType.BROADCAST, "Match between the parties of ", leaderBlue.getName(), " and ", leaderRed.getName(), " is starting! Watch live at: ", "<underlined><click:open_url:http://twitch.tv/projectmetade>twitch.tv/projectmetade</click></underlined>"));
            }
        });

        return true;
    }
}
