package com.yalcinkaya.ctf.listener;

import com.yalcinkaya.core.util.CoreUtil;
import com.yalcinkaya.core.util.MessageType;
import com.yalcinkaya.ctf.kit.ClickItem;
import com.yalcinkaya.ctf.kit.ClickKit;
import com.yalcinkaya.ctf.kit.InteractiveClickItem;
import com.yalcinkaya.ctf.kit.Kit;
import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Arrays;

public class KitListener implements Listener {

    @EventHandler
    public void onClickItem(PlayerInteractEvent event) {

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Player player = event.getPlayer();
            CTFUser user = CTFUtil.getUser(player.getUniqueId());
            if (user.getKit() instanceof ClickKit clickKit) {
                ItemStack item = event.getItem();
                if (item != null) {
                    for (ClickItem clickItem : clickKit.getClickItems()) {
                        if (clickItem instanceof InteractiveClickItem) {
                            continue;
                        }
                        if (clickItem.getItem().getType() == item.getType()) {
                            event.setCancelled(true);
                            if (clickItem.isCooldown()) {
                                clickItem.sendWarning(player);
                                return;
                            }
                            if (!clickItem.checkEnergy(user)) {
                                return;
                            }
                            if (user.isFrozen()) {
                                user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "You are frozen."));
                                return;
                            }
                            if (clickItem.tryClick(user)) {
                                clickItem.useCooldown();
                                clickItem.consume(user);
                                user.playSoundForWorld(clickItem.getSound());
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInteractiveClickItem(PlayerInteractEntityEvent event) {

        if (event.getRightClicked() instanceof Player rightClicked) {
            Player player = event.getPlayer();
            CTFUser user = CTFUtil.getUser(player.getUniqueId());
            if (user.getKit() instanceof ClickKit clickKit) {
                ItemStack item = player.getItemInHand();
                for (ClickItem clickItem : clickKit.getClickItems()) {

                    if (!(clickItem instanceof InteractiveClickItem interactiveClickItem)) {
                        continue;
                    }

                    if (interactiveClickItem.getItem().getType() == item.getType()) {
                        event.setCancelled(true);
                        if (interactiveClickItem.isCooldown()) {
                            interactiveClickItem.sendWarning(player);
                            return;
                        }
                        if (user.isFrozen()) {
                            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "You are frozen."));
                            return;
                        }
                        interactiveClickItem.setClicked(CTFUtil.getUser(rightClicked));
                        if (interactiveClickItem.tryClick(user)) {
                            interactiveClickItem.useCooldown();
                            user.playSoundForWorld(interactiveClickItem.getSound());
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {

        CTFUser user = CTFUtil.getUser(event.getPlayer().getUniqueId());
        Kit kit = user.getKit();
        if (kit != null) {
            kit.onInteract(event);
        }
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {

        Player player = event.getPlayer();
        CTFUser user = CTFUtil.getUser(player.getUniqueId());
        Kit kit = user.getKit();
        if (kit != null) {
            ItemStack item = event.getItemDrop().getItemStack();
            if (kit.getStartItems() != null && Arrays.asList(kit.getStartItems()).contains(item)) {
                event.setCancelled(true);
            }
            if (kit instanceof ClickKit clickKit && Arrays.stream(clickKit.getClickItems()).map(ClickItem::getItem).anyMatch(i -> i.equals(item))) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        if (event.getWhoClicked() instanceof Player player) {
            CTFUser user = CTFUtil.getUser(player.getUniqueId());
            Kit kit = user.getKit();
            if (kit != null) {
                ItemStack clickedItem = event.getCurrentItem();
                int slot = event.getClick() == ClickType.NUMBER_KEY ? event.getHotbarButton() : 0;
                ItemStack hotkeyedItem = player.getInventory().getItem(slot);

                if (kit.getStartItems() != null && (Arrays.asList(kit.getStartItems()).contains(clickedItem) || (event.getClick() == ClickType.NUMBER_KEY && Arrays.asList(kit.getStartItems()).contains(hotkeyedItem)))) {
                    event.setCancelled(true);
                }

                if (kit instanceof ClickKit clickKit && Arrays.stream(clickKit.getClickItems()).map(ClickItem::getItem).anyMatch(i -> i.equals(clickedItem) || (event.getClick() == ClickType.NUMBER_KEY && i.equals(hotkeyedItem)))) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {

        Player player = event.getPlayer();
        CTFUser user = CTFUtil.getUser(player.getUniqueId());
        if (user.getKit() != null && user.getKit() instanceof ClickKit clickKit) {
            ItemStack item = player.getItemInHand();
            if (Arrays.stream(clickKit.getClickItems()).map(ClickItem::getItem).anyMatch(i -> i.equals(item))) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        CTFUser user = CTFUtil.getUser(event.getPlayer().getUniqueId());
        Kit kit = user.getKit();
        if (kit != null) {
            kit.onToggleSneak(event);
        }
    }

    @EventHandler
    public void onProjectileHitPlayer(ProjectileHitEvent event) {

        Projectile projectile = event.getEntity();
        ProjectileSource source = projectile.getShooter();
        Entity victim = event.getHitEntity();

        if (source instanceof Player && victim instanceof Player) {
            CTFUser user = CTFUtil.getUser(((Player) source).getUniqueId());
            Kit kit = user.getKit();
            if (kit != null) {
                kit.onProjectileHitPlayer(event);
            }
        }
    }

    @EventHandler
    public void onProjectileHitBlock(ProjectileHitEvent event) {

        Projectile projectile = event.getEntity();
        ProjectileSource source = projectile.getShooter();
        Block hit = event.getHitBlock();

        if (source instanceof Player && hit != null) {
            CTFUser user = CTFUtil.getUser(((Player) source).getUniqueId());
            Kit kit = user.getKit();
            if (kit != null) {
                kit.onProjectileHitBlock(event);
            }
        }
    }

    @EventHandler
    public void onPlayerDamageByPlayer(EntityDamageByEntityEvent event) {

        if (event.getEntity() instanceof Player damaged && event.getDamager() instanceof Player damager) {

            CTFUser damagedUser = CTFUtil.getUser(damaged.getUniqueId());
            CTFUser damagerUser = CTFUtil.getUser(damager.getUniqueId());

            Kit damagedKit = damagedUser.getKit();
            Kit damagerKit = damagerUser.getKit();

            if (damagedKit != null) {
                damagedKit.onDamagedByPlayer(event);
            }

            if (damagerKit != null) {
                damagerKit.onDamagePlayer(event);
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        CTFUser user = CTFUtil.getUser(event.getPlayer().getUniqueId());
        Kit kit = user.getKit();
        if (kit != null) {
            kit.onMove(event);
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        CTFUser user = CTFUtil.getUser(event.getPlayer().getUniqueId());
        Kit kit = user.getKit();
        if (kit != null) {
            kit.onLeave(event);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        CTFUser user = CTFUtil.getUser(event.getEntity().getUniqueId());
        Kit kit = user.getKit();
        if (kit != null) {
            kit.onDeath(event);
        }
    }

    @EventHandler
    public void onPickUpItem(PlayerPickupItemEvent event) {
        CTFUser user = CTFUtil.getUser(event.getPlayer().getUniqueId());
        Kit kit = user.getKit();
        if (kit != null) {
            kit.onPickUpItem(event);
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {

        if (!(event.getEntity().getShooter() instanceof Player shooter)) {
            return;
        }

        CTFUser user = CTFUtil.getUser(shooter.getUniqueId());
        Kit kit = user.getKit();
        if (kit != null) {
            kit.onProjectileLaunch(event);
        }
    }
}
