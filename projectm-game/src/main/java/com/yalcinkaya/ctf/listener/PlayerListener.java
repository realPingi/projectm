package com.yalcinkaya.ctf.listener;

import com.yalcinkaya.core.util.CoreUtil;
import com.yalcinkaya.core.util.MessageType;
import com.yalcinkaya.ctf.CTF;
import com.yalcinkaya.ctf.CTFKit;
import com.yalcinkaya.ctf.stages.CaptureStage;
import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.user.CTFUserManager;
import com.yalcinkaya.ctf.util.CTFUtil;
import com.yalcinkaya.ctf.util.TempBlock;
import fr.mrmicky.fastboard.FastBoard;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRecipeDiscoverEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class PlayerListener implements Listener {

    private final CTFUserManager userManager;
    private final List<TempBlock> tempBlocks = new ArrayList<>();
    private final Inventory kitInventory = Bukkit.createInventory(null, 27, "Kit selection");

    public PlayerListener(CTFUserManager userManager) {
        this.userManager = userManager;
        kitInventory.addItem(CTFKit.getIcons());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!userManager.isRegistered(uuid)) {
            userManager.addUser(uuid);
            CTFUser user = userManager.getUser(uuid);
            user.setSpectating(true);
            CTFUtil.setupSpectator(user);
        }
        CTFUser user = userManager.getUser(uuid);
        user.setScoreboard(new FastBoard(player));
        Bukkit.getScheduler().runTaskLater(CTF.getInstance(), () -> CTFUtil.updateNametag(user), 2L);
    }

    @EventHandler
    public void onKitSelect(InventoryClickEvent event) {
        if (event.getClickedInventory() != null && event.getClickedInventory().equals(CTF.getInstance().getPlayerListener().getKitInventory())) {
            CTFKit kit = CTFKit.getFromIcon(event.getCurrentItem());
            if (kit != null) {
                event.setCancelled(true);
                Player player = (Player) event.getWhoClicked();
                CTFUser user = CTFUtil.getUser(player.getUniqueId());
                if (!CTFUtil.isSelected(user.getTeam(), kit)) {
                    if (CTF.getInstance().getCurrentStage() instanceof CaptureStage) {
                        CTFUtil.clearPlayer(player);
                        CTFKit.setKit(user, kit);
                        CTFUtil.equipPlayer(player);
                    } else {
                        CTFKit.setKit(user, kit);
                    }
                    user.sendMessage(CoreUtil.getMessage(MessageType.INFO, "You selected ", kit.getName(), "."));
                    event.getWhoClicked().closeInventory();
                } else {
                    user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "This kit is already taken."));
                }
            }
        }
    }

    @EventHandler
    public void onDamageByPlayer(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            if (getUserManager().getUser(player.getUniqueId()).isSpectating()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (getUserManager().getUser(player.getUniqueId()).isSpectating()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (getUserManager().getUser(event.getPlayer().getUniqueId()).isSpectating()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (getUserManager().getUser(event.getPlayer().getUniqueId()).isSpectating() || (isTempBlock(event.getBlock()) && getFromBlock(event.getBlock()).isUnbreakable())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onStarve(FoodLevelChangeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onRecipe(PlayerRecipeDiscoverEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (event.getRecipe().getResult().getType() != Material.OAK_PLANKS) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSpectate(PlayerTeleportEvent event) {
        if (event.getCause() == TeleportCause.SPECTATE) {
            event.setCancelled(true);
            event.getPlayer().setSpectatorTarget(null);
        }
    }

    public boolean isTempBlock(Block block) {
        return tempBlocks.stream().anyMatch(tempBlock -> tempBlock.getBlock().equals(block));
    }

    public TempBlock getFromBlock(Block block) {
        return tempBlocks.stream().filter(tempBlock -> tempBlock.getBlock().equals(block)).findAny().orElse(null);
    }
}
