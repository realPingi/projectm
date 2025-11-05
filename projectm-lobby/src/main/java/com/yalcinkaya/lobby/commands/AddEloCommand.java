package com.yalcinkaya.lobby.commands;

import com.yalcinkaya.core.ProjectM;
import com.yalcinkaya.core.redis.QueueType;
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

public class AddEloCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            return true;
        }

        LobbyUser user = LobbyUtil.getUser(player);

        if (!Rank.hasPermissions(user, Rank.MODERATOR)) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Insufficient permissions."));
            return true;
        }

        if (args.length < 3) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING,"Usage: /addelo <playerName> <queueType> <eloMod>"));
            return true;
        }

        String targetName = args[0];
        QueueType queueType;
        int amount;

        try {
            queueType = QueueType.valueOf(args[1].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING,"Invalid queue type: " + args[1]));
            return true;
        }

        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Elo modfifier must be a number."));
            return true;
        }

        ProjectM.getInstance().getRedisDataService().resolvePlayerAndAddEloAsync(targetName, queueType, amount)
                .thenAccept(result -> {
                    if (result == null) {
                        user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Can't find player."));
                    } else {
                        user.sendMessage(CoreUtil.getMessage(MessageType.SUCCESS, "Modified the elo of ", result, " by ", "" + amount, " for ", CoreUtil.camelizeString(queueType.name()) + " Queue", "."));
                    }
                });

        user.sendMessage(CoreUtil.getMessage(MessageType.INFO, "Trying to modify elo..."));
        return true;
    }
}
