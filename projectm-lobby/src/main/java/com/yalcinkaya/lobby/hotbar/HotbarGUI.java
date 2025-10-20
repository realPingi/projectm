package com.yalcinkaya.lobby.hotbar;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.units.qual.A;

import java.util.Arrays;

public interface HotbarGUI {

    InteractiveItem[] getItems();

    default void supply(Player player) {
        Inventory inventory = player.getInventory();
        ItemStack empty = new ItemStack(Material.AIR);
        for (int slot = 0; slot < 9; slot++) {

            if (slot >= getItems().length) {
                inventory.setItem(slot, empty);
                continue;
            }

            InteractiveItem interactiveItem = getItems()[slot];
            inventory.setItem(slot, interactiveItem == null ? empty : interactiveItem.getItemStack());
        }
    }

    default boolean contains(ItemStack itemStack) {
        return getInteractiveItem(itemStack) != null;
    }

    default InteractiveItem getInteractiveItem(ItemStack itemStack) {
        return Arrays.stream(getItems()).filter(interactiveItem -> interactiveItem != null && interactiveItem.getItemStack().equals(itemStack)).findFirst().orElse(null);
    }
}
