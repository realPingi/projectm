package com.yalcinkaya.lobby.commands;

import com.yalcinkaya.core.ProjectM;
import com.yalcinkaya.core.redis.QueueType;
import com.yalcinkaya.core.redis.RedisDataService;
import com.yalcinkaya.core.util.CoreUtil;
import com.yalcinkaya.core.util.MessageType;
import com.yalcinkaya.lobby.user.LobbyUser;
import com.yalcinkaya.lobby.util.LobbyUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;

public class EloCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }

        LobbyUser user = LobbyUtil.getUser(player);
        RedisDataService redisService = ProjectM.getInstance().getRedisDataService();

        redisService.getEloStatsAsync(player.getUniqueId().toString())
                .thenAccept(eloStats -> Bukkit.getScheduler().runTask(ProjectM.getInstance(), () -> {
                    if (eloStats == null || eloStats.isEmpty()) {
                        user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "No stats found."));
                        return;
                    }

                    for (Map.Entry<QueueType, Integer> entry : eloStats.entrySet()) {
                        QueueType type = entry.getKey();
                        int elo = entry.getValue();

                        String queueName = type.name().toUpperCase(Locale.ROOT);

                        user.sendMessage(CoreUtil.getMessage(MessageType.INFO, "<yellow>" + queueName + ": <gray>" + elo));
                    }
                }))
                .exceptionally(ex -> {
                    Bukkit.getScheduler().runTask(ProjectM.getInstance(), () ->
                            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Failed loading elo data."))
                    );
                    Bukkit.getLogger().severe("Async ELO lookup failed: " + ex.getMessage());
                    return null;
                });

        user.sendMessage(CoreUtil.getMessage(MessageType.INFO, "Loading elo data..."));
        return true;
    }
}
