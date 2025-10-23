package com.yalcinkaya.lobby.commands;

import com.yalcinkaya.core.util.CoreUtil;
import com.yalcinkaya.core.util.MessageType;
import com.yalcinkaya.lobby.Lobby;
import com.yalcinkaya.lobby.user.LobbyUser;
import com.yalcinkaya.lobby.util.LobbyUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetSQCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.isOp() && !(sender instanceof Player)) {
            return true;
        }

        LobbyUser user = LobbyUtil.getUser((Player) sender);

        if (args.length < 1) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING,"Usage: /setsq <min>"));
            return true;
        }

        int min = Integer.parseInt(args[0]);

        Lobby.getInstance().getQueueManager().getSoloQueue().setMin(min);

        user.sendMessage(CoreUtil.getMessage(MessageType.SUCCESS, "Set Solo Queue team size to ", "" + min));

        return true;
    }
}
