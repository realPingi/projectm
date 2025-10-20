package com.yalcinkaya.lobby.listener;

import com.yalcinkaya.lobby.Lobby;
import com.yalcinkaya.lobby.hotbar.HotbarManager;
import com.yalcinkaya.lobby.util.LobbyUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.units.qual.A;

import java.awt.*;
import java.util.Set;

public class HotbarListener implements Listener {

    Set<Action> acceptedActions = Set.of(Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK, Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK);

    @EventHandler
    public void onHotbarInteract(PlayerInteractEvent event) {
        HotbarManager hotbarManager = Lobby.getInstance().getHotbarManager();
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        Action action = event.getAction();
        if (item != null && acceptedActions.contains(action) && hotbarManager.isHotbarGUIItem(item)) {
            hotbarManager.accept(LobbyUtil.getUser(event.getPlayer()), fromAction(action), item);
            event.setCancelled(true);
        }
    }

    private ClickType fromAction(Action action) {
        if (action.isLeftClick()) {
            return ClickType.LEFT;
        } else if (action.isRightClick()) {
            return ClickType.RIGHT;
        } else {
            return null;
        }
    }

}
