package com.yalcinkaya.lobby.commands;

import com.yalcinkaya.core.ProjectM;
import com.yalcinkaya.core.redis.Rank;
import com.yalcinkaya.core.util.CoreUtil;
import com.yalcinkaya.core.util.MessageType;
import com.yalcinkaya.lobby.user.LobbyUser;
import com.yalcinkaya.lobby.util.LobbyUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public class SetRankCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.isOp() && !(sender instanceof Player)) {
            return true;
        }

        LobbyUser user = LobbyUtil.getUser((Player) sender);

        if (args.length < 2) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING,"Usage: /setrank <playerName> <rank>"));
            return true;
        }

        String targetName = args[0];
        String rankName = args[1].toUpperCase(Locale.ROOT);

        try {
            Rank.valueOf(rankName);
        } catch (IllegalArgumentException e) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "No such rank."));
            return true;
        }

        ProjectM.getInstance().getRedisDataService().resolvePlayerAndSetRankAsync(targetName, rankName)
                .thenAccept(result -> {
                    if (result == null) {
                        user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Can't find player."));
                    } else {
                        user.sendMessage(CoreUtil.getMessage(MessageType.SUCCESS, "Set the rank of ", result, " to ", rankName, "."));
                    }
                });

        user.sendMessage(CoreUtil.getMessage(MessageType.INFO, "Trying to set rank..."));
        return true;
    }

}
