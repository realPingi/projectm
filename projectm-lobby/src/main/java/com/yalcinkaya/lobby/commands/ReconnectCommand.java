package com.yalcinkaya.lobby.commands;

import com.yalcinkaya.core.util.CoreUtil;
import com.yalcinkaya.core.util.MessageType;
import com.yalcinkaya.lobby.Lobby;
import com.yalcinkaya.lobby.net.MatchLookupService;
import com.yalcinkaya.lobby.user.LobbyUser;
import com.yalcinkaya.lobby.util.LobbyUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Optional;

public class ReconnectCommand implements CommandExecutor {
    private final MatchLookupService service;

    public ReconnectCommand() {
        this.service = new MatchLookupService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }
        Player p = (Player) sender;
        LobbyUser user = LobbyUtil.getUser(p);

        LobbyUtil.getUser(p).sendMessage(CoreUtil.getMessage(MessageType.INFO, "Looking for running match..."));
        Bukkit.getScheduler().runTaskAsynchronously(Lobby.getInstance(), () -> {
            Optional<MatchLookupService.MatchInfo> matchOpt = service.findMatchFor(p.getUniqueId());
            if (!matchOpt.isPresent()) {
                user.sendMessage(CoreUtil.getMessage(MessageType.INFO, "No running match."));
                return;
            }
            MatchLookupService.MatchInfo m = matchOpt.get();
            String serverName = m.toVelocityServerName();
            user.sendMessage(CoreUtil.getMessage(MessageType.INFO, "Reconnecting..."));
            connectViaProxy(p, serverName);
        });

        return true;
    }

    /* ---- proxy connect ---- */

    private void connectViaProxy(Player player, String serverName) {
        Bukkit.getScheduler().runTask(Lobby.getInstance(), () -> {
            try (ByteArrayOutputStream b = new ByteArrayOutputStream();
                 DataOutputStream out = new DataOutputStream(b)) {
                out.writeUTF("Connect");
                out.writeUTF(serverName);
                player.sendPluginMessage(Lobby.getInstance(), "BungeeCord", b.toByteArray());
            } catch (IOException e) {
                LobbyUtil.getUser(player).sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Failed reconnecting."));
            }
        });
    }
}
