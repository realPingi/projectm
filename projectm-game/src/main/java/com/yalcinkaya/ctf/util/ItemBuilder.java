package com.yalcinkaya.ctf.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class ItemBuilder {

    private ItemStack itemStack;

    public ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
    }

    public ItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public ItemBuilder name(String string) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(string);
        itemStack.setItemMeta(itemMeta);
        return this;
    }

    public ItemBuilder amount(int i) {
        itemStack.setAmount(i);
        return this;
    }

    public ItemBuilder lore(List<String> lore, ChatColor color) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        for (int i = 0; i < lore.size(); i++) {
            lore.set(i, color + lore.get(i));
        }
        itemMeta.setLore(lore);
        itemStack.setItemMeta(itemMeta);
        return this;
    }

    public ItemBuilder lore(String string, ChatColor color) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        List<String> lore = Arrays.asList(CTFUtil.wrap(string, 40).split(CTFUtil.LINEBREAK));
        for (int i = 0; i < lore.size(); i++) {
            lore.set(i, color + lore.get(i));
        }
        itemMeta.setLore(lore);
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
