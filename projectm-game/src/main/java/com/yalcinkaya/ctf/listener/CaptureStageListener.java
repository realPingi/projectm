package com.yalcinkaya.ctf.listener;

import com.yalcinkaya.core.util.CoreUtil;
import com.yalcinkaya.core.util.IntTuple;
import com.yalcinkaya.core.util.MessageType;
import com.yalcinkaya.ctf.CTF;
import com.yalcinkaya.ctf.flag.CaptureStatus;
import com.yalcinkaya.ctf.flag.Flag;
import com.yalcinkaya.ctf.map.Map;
import com.yalcinkaya.ctf.team.TeamColor;
import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;

@Getter
public class CaptureStageListener extends StageListener {

    @EventHandler
    public void onWaterFlow(BlockFromToEvent event) {
        if (event.getBlock().isLiquid()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        final int buildLimit = 3;
        Block placed = event.getBlockPlaced();
        Block against = event.getBlockAgainst();
        Player player = event.getPlayer();

        if (CTFUtil.isInFlagRegion(against.getLocation()) || CTFUtil.isInFlagRegion(placed.getLocation()) || CTFUtil.isInSpawnRegion(placed.getLocation())) {
            event.setCancelled(true);
            CTFUtil.sendMessage(player, MessageType.WARNING, "Protected region.");
            return;
        }

        if (CTF.getInstance().getPlayerListener().isTempBlock(against)) {
            event.setCancelled(true);
            CTFUtil.sendMessage(player, MessageType.WARNING, "Can't build on temporary blocks.");
            return;
        }

        HashMap<IntTuple, Integer> heightMap = CTF.getInstance().getHeightMap();
        IntTuple placedTuple = IntTuple.tuple(placed);
        IntTuple againstTuple = IntTuple.tuple(against);
        Material material = placed.getType();
        placed.setType(Material.AIR); // trickserei
        int y;
        if (heightMap.containsKey(placedTuple)) {
            y = heightMap.get(placedTuple);
        } else {
            y = heightMap.containsKey(againstTuple) ? heightMap.get(againstTuple) : CoreUtil.getHighestBlock(against.getX(), against.getZ(), against.getWorld()).getY() + buildLimit;
            heightMap.put(placedTuple, y);
        }
        placed.setType(material);
        if (placed.getY() > y) {
            event.setCancelled(true);
            CTFUtil.sendMessage(player, MessageType.WARNING, "Build limit reached.");
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Location location = event.getBlock().getLocation();
        if (CTFUtil.isInFlagRegion(location) || CTFUtil.isInSpawnRegion(location)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {

        event.quitMessage(null);

        Player player = event.getPlayer();
        CTFUser user = CTFUtil.getUser(player.getUniqueId());

        if (user.isSpectating()) {
            return;
        }

        if (user.isCapturing()) {
            CTFUtil.restoreFlag(user);
        }

        if (!player.isDead()) {
            player.setHealth(0);
        }

        user.setEnergy(0);
        event.quitMessage(MiniMessage.miniMessage().deserialize(CoreUtil.getMessage(MessageType.BROADCAST, "", CTFUtil.getColoredName(user), " left the match.")));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {

        Player player = event.getEntity();
        CTFUser user = CTFUtil.getUser(player.getUniqueId());
        String userName = CTFUtil.getColoredName(user);
        event.getDrops().clear();
        user.setEnergy(0);

        EntityDamageEvent entityDamageEvent = player.getLastDamageCause();
        if (entityDamageEvent == null) {
            event.deathMessage(null);
            return;
        }

        if (user.isSpectating()) {
            event.deathMessage(null);
            return;
        }

        UUID lastDamager = user.getLastDamager();
        DamageCause damageCause = entityDamageEvent.getCause();
        final String deathMessage;
        final String lastDamagerName;

        if (lastDamager == null) {
            switch (damageCause) {
                case VOID -> deathMessage = " fell down the abyss.";
                case FALL -> deathMessage = " shattered on the ground.";
                default -> deathMessage = " died a mysterious death.";
            }
        } else {
            CTFUser lastDamagerUser = CTFUtil.getUser(lastDamager);
            lastDamagerName = CTFUtil.getColoredName(lastDamagerUser);
            if (!lastDamagerUser.getTeam().equals(user.getTeam())) {
                CTFUtil.rewardKill(lastDamager);
            }
            switch (damageCause) {
                case VOID -> deathMessage = " has been knocked into the abyss by " + lastDamagerName + ".";
                case FALL -> deathMessage = " couldn't escape " + lastDamagerName + ".";
                case PROJECTILE -> deathMessage = " has been sniped by " + lastDamagerName + ".";
                case ENTITY_ATTACK -> deathMessage = " has been killed by " + lastDamagerName + ".";
                default -> deathMessage = " has been killed by " + lastDamagerName + " in a mysterious way.";
            }
        }

        event.deathMessage(MiniMessage.miniMessage().deserialize(CoreUtil.getMessage(MessageType.BROADCAST, "", userName, deathMessage)));
        user.setLastDamager(null);

        if (user.isCapturing()) {
            CTFUtil.restoreFlag(user);
        }

        Bukkit.getScheduler().runTaskLater(CTF.getInstance(), () -> {
            if (player.isDead()) {
                player.spigot().respawn();
                Map map = CTF.getInstance().getMap();
                Location respawnLoc = user.getTeam().getColor() == TeamColor.BLUE ? map.getBlueSpawn() : map.getRedSpawn();
                player.teleport(respawnLoc);
            }
        }, 1L);
    }

    @EventHandler
    public void onPlayerDamageByPlayer(EntityDamageByEntityEvent event) {

        Entity damagedEntity = event.getEntity();
        Entity damagerEntity = event.getDamager();

        if (damagerEntity instanceof Player damager) {

            if (damagedEntity instanceof Player damaged) {

                CTFUser damagerUser = CTFUtil.getUser(damager);

                if (damager.getItemInHand().getType() == Material.IRON_AXE) {
                    damagerUser.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Axes don't deal damage."));
                    event.setCancelled(true);
                }

                CTFUser damagedUser = CTFUtil.getUser(damaged);
                damagedUser.setLastDamager(damager.getUniqueId());
                event.setDamage(event.getDamage() * 0.75);

                if (!damagedUser.getTeam().equals(damagerUser.getTeam())) {
                    if (!event.isCancelled()) {
                        CTFUtil.modifyEnergy(damagerUser, event.getDamage());
                    }
                } else {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            CTFUser user = CTFUtil.getUser(player);

            if (user.isFrozen()) {
                event.setCancelled(true);
            }

            if (event.getCause() == DamageCause.FALL && user.isCancelNextFallDamage()) {
                event.setCancelled(true);
                user.setCancelNextFallDamage(false);
            }

            if (event.getCause() == DamageCause.VOID) {
                event.setDamage(1000);
            }
        }
    }

    @EventHandler
    public void onGap(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() == Material.GOLDEN_APPLE) {
            Player player = event.getPlayer();
            player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 8));
        }
    }

    @EventHandler
    public void onRegainHealth(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player player) {
            CTFUser user = CTFUtil.getUser(player);
            if (System.currentTimeMillis() - user.getLastHeal() < 4 * 1000) {
                event.setCancelled(true);
            } else {
                user.setLastHeal(System.currentTimeMillis());
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getHitEntity() instanceof Player player) {
            CTFUser user = CTFUtil.getUser(player);
            if (user.isFrozen()) {
                event.setCancelled(true);
            }
        }
        if (event.getHitBlock() != null) {
            event.getEntity().remove();
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        new BukkitRunnable() {

            @Override
            public void run() {
                if (projectile.getLocation().getY() < 0) {
                    projectile.remove();
                    cancel();
                }
            }

        }.runTaskTimer(CTF.getInstance(), 0, 5);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        CTFUser user = CTFUtil.getUser(event.getPlayer());

        if (user.isSpectating()) {
            return;
        }

        if (user.isFrozen() && !event.getTo().toVector().equals(event.getFrom().toVector())) {
            event.setCancelled(true);
        }
        if (user.isCancelNextFallDamage() && event.getPlayer().getFallDistance() < 3 && event.getPlayer().isOnGround()) {
            user.setCancelNextFallDamage(false);
        }
        if (CTFUtil.isInFlagRegion(user.getTeam().getColor().toString(), event.getTo())) {
            event.setCancelled(true);
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "You can't enter your own flag region."));
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        CTFUser user = CTFUtil.getUser(event.getPlayer());

        if (user.isSpectating()) {
            return;
        }

        if (CTFUtil.isInFlagRegion(user.getTeam().getColor().toString(), event.getTo())) {
            event.setCancelled(true);
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "You can't enter your own flag region."));
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (event.getRecipe().getResult().getType() == Material.FISHING_ROD) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(ItemSpawnEvent event) {
        Item item = event.getEntity();
        if (item.getItemStack().getType() != Material.OAK_PLANKS && item.getThrower() == null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCapture(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        CTFUser user = CTFUtil.getUser(player);

        if (user.isSpectating()) {
            return;
        }

        if (event.isSneaking()) {
            Flag nearbyFlag = CTF.getInstance().getMap().getFlags().stream().filter(flag -> !flag.getTeam().equals(user.getTeam().getColor())).filter(flag -> CTFUtil.isInFlagRange(flag, player)).findFirst().orElse(null);
            if (nearbyFlag != null) {

                if (nearbyFlag.getStatus() == CaptureStatus.CAPTURED) {
                    user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "This flag is already captured."));
                    return;
                }

                if (nearbyFlag.getStatus() == CaptureStatus.DANGER) {
                    user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "This flag is being captured right now."));
                    return;
                }

                if (user.isCapturing()) {
                    user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "You are already capturing a flag."));
                    return;
                }

                if (user.getEnergy() < 100) {
                    user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Your energy resources are insufficient."));
                    return;
                }

                if (user.isPreventCapture()) {
                    user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "You can't capture in this state."));
                    return;
                }

                new BukkitRunnable() {

                    final int windup = 4;
                    int timer;

                    @Override
                    public void run() {
                        timer++;

                        if (!player.isSneaking() || !CTFUtil.isInFlagRange(nearbyFlag, player)) {
                            player.setLevel(0);
                            player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BREAK, 1F, 0);
                            cancel();
                            return;
                        }

                        if (timer % 20 == 0 && timer < windup * 20) {
                            player.setLevel(timer / 20);
                            player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1F, 0);
                        }
                        if (timer >= windup * 20) {
                            CTFUtil.pickUpFlag(user, nearbyFlag);
                            player.setLevel(0);
                            cancel();
                        }
                    }

                }.runTaskTimer(CTF.getInstance(), 1, 1);
            }
        }
    }

    @Override
    public void setupPlayer(Player player) {
        CTF ctf = CTF.getInstance();
        CTFUser user = CTFUtil.getUser(player);
        CTFUtil.clearPlayer(player);

        Location spawn = user.getTeam().equals(ctf.getBlue()) ? ctf.getMap().getBlueSpawn() : ctf.getMap().getRedSpawn();
        player.teleport(spawn);
        user.setFrozen(true);
        CTFUtil.equipPlayer(player);

        if (user.getKit() == null) {
            player.openInventory(CTF.getInstance().getPlayerListener().getKitInventory());
        }

        new BukkitRunnable() {

            @Override
            public void run() {
                user.setFrozen(false);
            }

        }.runTaskLater(ctf, 4 * 20);
    }

    @Override
    public Component getJoinMessage(CTFUser user) {
        return MiniMessage.miniMessage().deserialize(CoreUtil.getMessage(MessageType.BROADCAST, "", CTFUtil.getColoredName(user), " rejoined the match."));
    }

}
