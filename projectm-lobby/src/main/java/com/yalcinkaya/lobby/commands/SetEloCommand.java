package com.yalcinkaya.lobby.commands;

import com.yalcinkaya.core.ProjectM;
import com.yalcinkaya.core.redis.QueueType;
import com.yalcinkaya.core.util.CoreUtil;
import com.yalcinkaya.core.util.MessageType;
import com.yalcinkaya.lobby.user.LobbyUser;
import com.yalcinkaya.lobby.util.LobbyUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;


public class SetEloCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.isOp() && !(sender instanceof Player)) {
            return true;
        }

        LobbyUser user = LobbyUtil.getUser((Player) sender);

        if (args.length < 3) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING,"Usage: /setelo <playerName> <queueType> <newElo>"));
            return true;
        }

        String targetName = args[0];
        QueueType queueType;
        int newElo;

        try {
            queueType = QueueType.valueOf(args[1].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING,"Invalid queue type: " + args[1]));
            return true;
        }

        try {
            newElo = Integer.parseInt(args[2]);
            if (newElo < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Elo value must be positive."));
            return true;
        }

        // Führe die Logik asynchron aus
        ProjectM.getInstance().getRedisDataService().resolvePlayerAndSetEloAsync(targetName, queueType, newElo)
                .thenAccept(result -> {
                    if (result == null) {
                        user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Can't find player."));
                    } else {
                        user.sendMessage(CoreUtil.getMessage(MessageType.SUCCESS, "Set the elo of ", result, " to ", "" + newElo, " for ", CoreUtil.camelizeString(queueType.name()) + " Queue", "."));
                    }
                });

        user.sendMessage(CoreUtil.getMessage(MessageType.INFO, "Trying to set elo..."));
        return true;
    }

}
