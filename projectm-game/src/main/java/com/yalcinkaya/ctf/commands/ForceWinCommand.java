package com.yalcinkaya.ctf.commands;

import com.yalcinkaya.ctf.CTF;
import com.yalcinkaya.ctf.flag.CaptureStatus;
import com.yalcinkaya.ctf.team.Team;
import com.yalcinkaya.ctf.util.CTFUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ForceWinCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Team team = args[0].equalsIgnoreCase("blue") ? CTF.getInstance().getRed() : CTF.getInstance().getBlue();
        CTF.getInstance().getMap().getFlags().stream()
                .filter(flag -> flag.getTeam() == team.getColor())
                .forEach(flag -> CTFUtil.setCaptureStatus(flag, CaptureStatus.CAPTURED));
        return true;
    }
}
