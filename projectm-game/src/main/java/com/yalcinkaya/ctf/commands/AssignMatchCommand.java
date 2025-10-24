package com.yalcinkaya.ctf.commands;

import com.yalcinkaya.core.redis.QueueType;
import com.yalcinkaya.ctf.CTF;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;

import java.util.Locale;

public class AssignMatchCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("Usage: /assignmatch <matchId> <mapId> <teamsB64>");
            return true;
        }

        if (!(sender instanceof RemoteConsoleCommandSender)) {
            sender.sendMessage("<red>" + "This command is reserved for orchestrator (RCON only).");
            return true;
        }

        String matchId = args[0];
        String mapId   = args[1];
        String teamsB64 = args[2];
        String queueType = args[3];

        try {
            CTF ctf = CTF.getInstance();
            ctf.setMatchId(matchId);
            ctf.setMapId(mapId);
            ctf.setTeamJson(teamsB64);
            ctf.setQueueType(QueueType.valueOf(queueType.toUpperCase(Locale.ROOT)));
            ctf.start();

            sender.sendMessage("[Orchestrator] Match " + matchId + " assigned on map " + mapId);
        } catch (Exception e) {
            sender.sendMessage("[Orchestrator] Failed to assign match: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }
}

