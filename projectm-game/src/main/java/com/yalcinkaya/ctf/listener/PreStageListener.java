package com.yalcinkaya.ctf.listener;

import com.yalcinkaya.ctf.CTF;
import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import com.yalcinkaya.util.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

public class PreStageListener extends StageListener {

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        event.setQuitMessage(null);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        setupPlayer(event.getPlayer());
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
    public void onInventoryClick(InventoryClickEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        event.setCancelled(true);
        ItemStack item = event.getItem();
        if (item != null) {
            if (item.getType() == Material.ENDER_CHEST) {
                event.getPlayer().openInventory(CTF.getInstance().getPlayerListener().getKitInventory());
            }
        }
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

    @Override
    public void setupPlayer(Player player) {
        CTFUtil.teleportPlayerMidMap(CTFUtil.getUser(player.getUniqueId()));
        player.getInventory().setItem(4, ItemBuilder.of(Material.ENDER_CHEST).name("<italic:false><gold>Select your kit<gold>").build());
    }

    @Override
    public String getJoinMessage(CTFUser user) {
        return null;
    }

}
