package com.yalcinkaya.lobby.menu;

import com.yalcinkaya.lobby.Lobby;
import com.yalcinkaya.lobby.menu.menus.QueueMenu;
import com.yalcinkaya.lobby.util.ItemBuilder;
import com.yalcinkaya.lobby.util.LobbyUtil;
import com.yalcinkaya.lobby.util.QueueUtil;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class MenuManager {

    private final QueueMenu queueMenu = new QueueMenu(27, "Queues");
    @Getter
    private final ItemStack queueMenuKey = ItemBuilder.of(Material.COMPASS).name("<italic:false><light_purple>Queue<light_purple>").build();
    private final HashMap<ItemStack, Menu> menus = new HashMap<>();

    public void loadMenus() {
        menus.put(queueMenuKey, queueMenu);
    }

    public boolean isMenu(Inventory inventory) {
        return menus.values().stream().anyMatch(menu -> menu.getInventory().equals(inventory));
    }

    public Menu getMenu(Inventory inventory) {
        return menus.values().stream().filter(menu -> menu.getInventory().equals(inventory)).findFirst().orElse(null);
    }

    public boolean isKey(ItemStack itemStack) {
        return menus.keySet().stream().anyMatch(i -> i.isSimilar(itemStack));
    }

    public Menu getMenu(ItemStack key) {
        return menus.get(key);
    }
}
