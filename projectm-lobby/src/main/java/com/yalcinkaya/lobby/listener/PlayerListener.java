package com.yalcinkaya.lobby.listener;

import com.yalcinkaya.lobby.Lobby;
import com.yalcinkaya.lobby.net.MatchLookupService;
import com.yalcinkaya.lobby.queue.Queue;
import com.yalcinkaya.lobby.queue.QueueManager;
import com.yalcinkaya.lobby.util.LobbyUtil;
import com.yalcinkaya.lobby.util.MessageType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.*;

public class PlayerListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(Lobby.getInstance(), () -> {
            Lobby.getInstance().getUserManager().addUser(player.getUniqueId());
            player.teleport(new Location(player.getWorld(), 0, 0, 0));
            LobbyUtil.giveLobbyItems(player);
        }, 1);
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        event.setQuitMessage(null);
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
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        event.setCancelled(true);
    }
}
