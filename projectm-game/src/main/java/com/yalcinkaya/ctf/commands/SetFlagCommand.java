package com.yalcinkaya.ctf.commands;

import com.yalcinkaya.core.redis.Rank;
import com.yalcinkaya.core.util.CoreUtil;
import com.yalcinkaya.core.util.MessageType;
import com.yalcinkaya.ctf.CTF;
import com.yalcinkaya.ctf.flag.CaptureStatus;
import com.yalcinkaya.ctf.flag.FlagLocation;
import com.yalcinkaya.ctf.team.Team;
import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SetFlagCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            return true;
        }

        CTFUser user = CTFUtil.getUser(player.getUniqueId());

        if (!Rank.hasPermissions(user, Rank.ADMIN)) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Insufficient permissions."));
            return true;
        }

        Team team = args[0].equalsIgnoreCase("blue") ? CTF.getInstance().getBlue() : CTF.getInstance().getRed();
        FlagLocation flagLocation = FlagLocation.valueOf(args[1]);
        CaptureStatus captureStatus = CaptureStatus.valueOf(args[2]);
        CTF.getInstance().getMap().getFlags().stream()
                .filter(flag -> flag.getTeam() == team.getColor())
                .filter(flag -> flag.getLocation() == flagLocation)
                .forEach(flag -> CTFUtil.setCaptureStatus(flag, captureStatus));
        return true;
    }
}
