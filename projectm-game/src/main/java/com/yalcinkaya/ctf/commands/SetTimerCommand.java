package com.yalcinkaya.ctf.commands;

import com.yalcinkaya.core.redis.Rank;
import com.yalcinkaya.core.util.CoreUtil;
import com.yalcinkaya.core.util.MessageType;
import com.yalcinkaya.ctf.CTF;
import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SetTimerCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            return true;
        }

        CTFUser user = CTFUtil.getUser(player.getUniqueId());

        if (!Rank.hasPermissions(user, Rank.MODERATOR)) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Insufficient permissions."));
            return true;
        }

        int time = Integer.parseInt(args[0]);
        if (time < 0) {
            throw new IllegalArgumentException();
        }
        CTF.getInstance().getCurrentStage().setTimer(time * 20);
        return true;
    }
}
