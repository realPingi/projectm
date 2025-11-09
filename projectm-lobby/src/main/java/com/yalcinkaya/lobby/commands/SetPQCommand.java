package com.yalcinkaya.lobby.commands;

import com.yalcinkaya.core.redis.Rank;
import com.yalcinkaya.core.util.CoreUtil;
import com.yalcinkaya.core.util.MessageType;
import com.yalcinkaya.lobby.Lobby;
import com.yalcinkaya.lobby.user.LobbyUser;
import com.yalcinkaya.lobby.util.LobbyUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetPQCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            return true;
        }

        LobbyUser user = LobbyUtil.getUser(player);

        if (!Rank.hasPermissions(user, Rank.ADMIN)) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Insufficient permissions."));
            return true;
        }

        if (args.length < 1) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Usage: /setpq <partySize>"));
            return true;
        }

        int partySize = Integer.parseInt(args[0]);

        Lobby.getInstance().getQueueManager().getPartyQueue().setPartySize(partySize);

        user.sendMessage(CoreUtil.getMessage(MessageType.SUCCESS, "Set Party Queue team size to ", "" + partySize));

        return true;
    }
}