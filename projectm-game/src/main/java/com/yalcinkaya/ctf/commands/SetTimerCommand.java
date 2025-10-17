package com.yalcinkaya.ctf.commands;

import com.yalcinkaya.ctf.CTF;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class SetTimerCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        int time = Integer.parseInt(args[0]);
        if (time < 0) {
            throw new IllegalArgumentException();
        }
        CTF.getInstance().getCurrentStage().setTimer(time);
        return true;
    }
}
