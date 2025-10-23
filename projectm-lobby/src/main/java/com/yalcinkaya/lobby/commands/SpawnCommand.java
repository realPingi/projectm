package com.yalcinkaya.lobby.commands;

import com.yalcinkaya.lobby.util.Place;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        player.teleport(Place.SPAWN.getLocation());
        return true;
    }
}
