package com.yalcinkaya.lobby.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ItemBuilder {

    private ItemStack itemStack;

    public ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
    }

    public ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public ItemBuilder name(String string) {
        Bukkit.broadcastMessage(String.valueOf(itemStack.hasItemMeta()));
        Bukkit.broadcastMessage(String.valueOf(itemStack.getItemMeta() != null));
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(string);
        itemStack.setItemMeta(itemMeta);
        return this;
    }

    public ItemBuilder amount(int i) {
        itemStack.setAmount(i);
        return this;
    }

    public ItemBuilder lore(List<String> lore) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setLore(lore);
        itemStack.setItemMeta(itemMeta);
        return this;
    }


    public ItemBuilder lore(String string, ChatColor color) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        List<String> lore = Arrays.asList(LobbyUtil.wrap(string, 40).split(LobbyUtil.LINEBREAK));
        for (int i = 0; i < lore.size(); i++) {
            lore.set(i, color + lore.get(i));
        }
        List<Component> components = lore.stream().map(Component::text).collect(Collectors.toList());
        itemMeta.lore(components);
        itemStack.setItemMeta(itemMeta);
        return this;
    }

    public ItemBuilder enchant(Enchantment enchantment, int level) {
        itemStack.addEnchantment(enchantment, level);
        return this;
    }

    public ItemBuilder unbreakable() {
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setUnbreakable(true);
        itemStack.setItemMeta(itemMeta);
        return this;
    }

    public ItemStack build() {
        return itemStack;
    }

}
