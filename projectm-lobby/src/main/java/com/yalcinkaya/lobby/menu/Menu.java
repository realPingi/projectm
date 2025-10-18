package com.yalcinkaya.lobby.menu;

import com.yalcinkaya.lobby.Lobby;
import com.yalcinkaya.lobby.user.LobbyUser;
import com.yalcinkaya.lobby.util.LobbyUtil;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.function.BiConsumer;

@Data
public abstract class Menu {

    private Inventory inventory;
    private HashMap<Integer, InteractiveItem> interactiveItems = new HashMap<>();

    public Menu(int size, String name) {
        this.inventory = Bukkit.createInventory(null, size, name);
    }

    public void setItem(int pos, ItemStack itemStack, BiConsumer<LobbyUser, ClickType> action) {
        interactiveItems.put(pos, new InteractiveItem(itemStack, action));
        inventory.setItem(pos, itemStack);
    }

    public boolean isInteractive(int slot) {
        return interactiveItems.containsKey(slot);
    }

    public void open(LobbyUser user) {
        Player player = LobbyUtil.getPlayer(user);
        if (player != null && player.isOnline()) {
            refresh();
            player.openInventory(inventory);
            user.setMenu(this);
        }
    }

    public void closeAndOpen(LobbyUser user) {
        Player player = LobbyUtil.getPlayer(user);
        if (player != null && player.isOnline()) {
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(Lobby.getInstance(), () -> open(user), 2);

        }
    }

    public abstract void refresh();

}
