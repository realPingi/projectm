package com.yalcinkaya.ctf.commands;

import com.yalcinkaya.ctf.CTF;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;

public class AssignMatchCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("Usage: /assignmatch <matchId> <mapId> <teamsB64>");
            return true;
        }

        if (!(sender instanceof RemoteConsoleCommandSender)) {
            sender.sendMessage(ChatColor.RED + "This command is reserved for orchestrator (RCON only).");
            return true;
        }

        String matchId = args[0];
        String mapId   = args[1];
        String teamsB64 = args[2];

        try {
            CTF ctf = CTF.getInstance();
            ctf.setMatchId(matchId);
            ctf.setMapId(mapId);
            ctf.setTeamJson(teamsB64);
            ctf.start();

            sender.sendMessage("[Orchestrator] Match " + matchId + " assigned on map " + mapId);
        } catch (Exception e) {
            sender.sendMessage("[Orchestrator] Failed to assign match: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }
}

