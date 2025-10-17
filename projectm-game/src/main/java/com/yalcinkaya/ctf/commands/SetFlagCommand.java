package com.yalcinkaya.ctf.commands;

import com.yalcinkaya.ctf.CTF;
import com.yalcinkaya.ctf.flag.CaptureStatus;
import com.yalcinkaya.ctf.flag.FlagLocation;
import com.yalcinkaya.ctf.team.Team;
import com.yalcinkaya.ctf.util.CTFUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class SetFlagCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
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
