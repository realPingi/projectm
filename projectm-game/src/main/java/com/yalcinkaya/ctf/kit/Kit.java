package com.yalcinkaya.ctf.kit;

import com.yalcinkaya.ctf.CTF;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

public abstract class Kit implements Listener {

    public Kit() {
        CTF.getInstance().getServer().getPluginManager().registerEvents(this, CTF.getInstance());
    }

    public abstract String getName();

    public ItemStack[] getStartItems() {
        return null;
    }

    public void onInteract(PlayerInteractEvent event) {
    }

    public void onInteractEntity(PlayerInteractEntityEvent event) {
    }

    public void onItemDrop(PlayerDropItemEvent event) {
    }

    public void onDeath(PlayerDeathEvent event) {
    }

    public void onDamagedByPlayer(EntityDamageByEntityEvent event) {
    }

    public void onDamagePlayer(EntityDamageByEntityEvent event) {
    }

    public void onMove(PlayerMoveEvent event) {
    }

    public void onToggleSneak(PlayerToggleSneakEvent event) {
    }

    public void onProjectileLaunch(ProjectileLaunchEvent event) {
    }

    public void onProjectileHitPlayer(ProjectileHitEvent event) {
    }

    public void onProjectileHitBlock(ProjectileHitEvent event) {
    }

    public void onLeave(PlayerQuitEvent event) {
    }

    public void onPickUpItem(PlayerPickupItemEvent event) {
    }

}
