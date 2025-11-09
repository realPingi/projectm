package com.yalcinkaya.lobby.listener;

import com.yalcinkaya.core.ProjectM;
import com.yalcinkaya.core.redis.Rank;
import com.yalcinkaya.core.redis.RedisDataService;
import com.yalcinkaya.lobby.Lobby;
import com.yalcinkaya.lobby.user.LobbyUserManager;
import com.yalcinkaya.lobby.util.LobbyUtil;
import com.yalcinkaya.lobby.util.Place;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.*;

public class PlayerListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.joinMessage(null);
        Player player = event.getPlayer();
        LobbyUserManager userManager = Lobby.getInstance().getUserManager();
        if (!userManager.isRegistered(player.getUniqueId())) {
            userManager.addUser(player.getUniqueId());
        }
        player.teleport(Place.SPAWN.getLocation());
        LobbyUtil.giveLobbyItems(player);
        Bukkit.getScheduler().runTaskAsynchronously(Lobby.getInstance(), () -> {
            RedisDataService redisDataService = ProjectM.getInstance().getRedisDataService();
            Rank rank = redisDataService.getRank(player.getUniqueId().toString());
            ProjectM.getInstance().getNametagManager().setPlayerNametag(player, rank.name(), rank.getColor());
            redisDataService.bootstrapAllQueues(player.getUniqueId().toString(), player.getName());
        });
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        event.quitMessage(null);
        Lobby.getInstance().getQueueManager().unqueue(LobbyUtil.getUser(event.getPlayer()));
    }

    @EventHandler
    public void onRecipeDiscover(PlayerRecipeDiscoverEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
                player.teleport(Place.SPAWN.getLocation());
            }
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!event.getPlayer().isOp()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!event.getPlayer().isOp()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        event.setCancelled(true);
    }

}
