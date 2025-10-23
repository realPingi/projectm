package com.yalcinkaya.lobby.listener;

import com.yalcinkaya.core.ProjectM;
import com.yalcinkaya.lobby.Lobby;
import com.yalcinkaya.lobby.util.LobbyUtil;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.*;
import org.bukkit.util.Vector;

public class PlayerListener implements Listener {

    private final static Vector spawnVector = new Vector(-1.5,88,-0.5);

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);
        Player player = event.getPlayer();
        if (player.isOp()) {
            ProjectM.getInstance().getNametagManager().setPlayerNametag(player, "op", NamedTextColor.DARK_RED);
        } else {
            ProjectM.getInstance().getNametagManager().setPlayerNametag(player, "default", NamedTextColor.GRAY);
        }
        Bukkit.getScheduler().runTaskLater(Lobby.getInstance(), () -> {
            Lobby.getInstance().getUserManager().addUser(player.getUniqueId());
            player.teleport(getSpawnLocation());
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
        if (event.getEntity() instanceof Player player) {
            if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
                player.teleport(getSpawnLocation());
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

    public static Location getSpawnLocation() {
        World world = Bukkit.getWorld("world");
        Location spawnLocation = spawnVector.toLocation(world);
        spawnLocation.setYaw(-90);
        return spawnLocation;
    }

}
