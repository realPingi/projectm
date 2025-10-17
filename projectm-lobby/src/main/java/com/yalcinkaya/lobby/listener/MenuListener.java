package com.yalcinkaya.lobby.listener;

import com.yalcinkaya.lobby.Lobby;
import com.yalcinkaya.lobby.menu.Menu;
import com.yalcinkaya.lobby.menu.MenuManager;
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

public class MenuListener implements Listener {

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        MenuManager menuManager = Lobby.getInstance().getMenuManager();
        if (event.getWhoClicked() instanceof Player player) {
            Inventory inventory = event.getClickedInventory();
            if (menuManager.isMenu(inventory)) {
                ClickType clickType = event.getClick();
                int slot = event.getSlot();
                Menu menu = menuManager.getMenu(inventory);
                if (menu.isInteractive(slot)) {
                    menu.getInteractiveItems().get(slot).getAction().accept(LobbyUtil.getUser(player), clickType);
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onMenuOpen(PlayerInteractEvent event) {
        MenuManager menuManager = Lobby.getInstance().getMenuManager();
        ItemStack item = event.getItem();
        Action action = event.getAction();
        if (item != null && (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) && menuManager.isKey(item)) {
            event.getPlayer().openInventory(menuManager.getMenu(item).getInventory());
            event.setCancelled(true);
        }
    }

}
