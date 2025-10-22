package com.yalcinkaya.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionType;

import java.util.*;
import java.util.function.Consumer;

public class ItemBuilder {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final ItemStack stack;

    /* ------------ Konstruktoren / Fabrik ------------ */

    private ItemBuilder(Material material) {
        this.stack = new ItemStack(material);
    }

    private ItemBuilder(ItemStack base) {
        this.stack = base.clone();
    }

    public static ItemBuilder of(Material material) {
        return new ItemBuilder(material);
    }

    public static ItemBuilder of(ItemStack base) {
        return new ItemBuilder(base);
    }

    /* ------------ Grundlegendes ------------ */

    public ItemBuilder amount(int amount) {
        stack.setAmount(Math.max(1, Math.min(99, amount)));
        return this;
    }

    public ItemBuilder meta(Consumer<ItemMeta> editor) {
        ItemMeta meta = stack.getItemMeta();
        editor.accept(meta);
        stack.setItemMeta(meta);
        return this;
    }

    public ItemBuilder name(String miniMessage) {
        return meta(m -> m.displayName(MM.deserialize(miniMessage)));
    }

    public ItemBuilder name(Component component) {
        return meta(m -> m.displayName(component));
    }

    public ItemBuilder lore(String... linesMiniMessage) {
        return lore(Arrays.asList(linesMiniMessage));
    }

    public ItemBuilder lore(List<String> linesMiniMessage) {
        List<Component> comps = linesMiniMessage.stream()
                .map(MM::deserialize)
                .collect(Collectors.toList());
        return meta(m -> m.lore(comps));
    }

    public ItemBuilder appendLore(String... linesMiniMessage) {
        return meta(m -> {
            List<Component> now = Optional.ofNullable(m.lore()).map(ArrayList::new).orElseGet(ArrayList::new);
            for (String s : linesMiniMessage) now.add(MM.deserialize(s));
            m.lore(now);
        });
    }

    /* ------------ Enchants / Flags / Unbreakable / Model ------------ */

    public ItemBuilder enchant(Enchantment ench, int level, boolean ignoreLevelRestriction) {
        return meta(m -> m.addEnchant(ench, Math.max(level, 1), ignoreLevelRestriction));
    }

    public ItemBuilder clearEnchants() {
        return meta(m -> m.getEnchants().keySet().forEach(m::removeEnchant));
    }

    public ItemBuilder flags(ItemFlag... flags) {
        return meta(m -> m.addItemFlags(flags));
    }

    public ItemBuilder unbreakable(boolean value) {
        return meta(m -> m.setUnbreakable(value));
    }

    public ItemBuilder model(int customModelData) {
        return meta(m -> m.setCustomModelData(customModelData));
    }

    /* ------------ Schaden / Lederfarbe ------------ */

    public ItemBuilder damage(int value) {
        return meta(m -> {
            if (m instanceof Damageable dmg) dmg.setDamage(Math.max(0, value));
        });
    }

    public ItemBuilder leatherColor(Color color) {
        return meta(m -> {
            if (m instanceof LeatherArmorMeta lam) lam.setColor(color);
        });
    }

    /* ------------ Potions ------------ */

    public ItemBuilder potion(PotionType type) {
        return meta(m -> {
            if (m instanceof PotionMeta pm) pm.setBasePotionType(type);
        });
    }

    public ItemBuilder potionColor(Color color) {
        return meta(m -> {
            if (m instanceof PotionMeta pm) pm.setColor(color);
        });
    }

    /* ------------ PDC Utilities ------------ */

    public <Z, T> ItemBuilder pdc(Plugin plugin, String key, PersistentDataType<T, Z> type, Z value) {
        return meta(m -> {
            PersistentDataContainer pdc = m.getPersistentDataContainer();
            pdc.set(new NamespacedKey(plugin, key), type, value);
        });
    }

    public <Z, T> Optional<Z> getPdc(Plugin plugin, String key, PersistentDataType<T, Z> type) {
        ItemMeta m = stack.getItemMeta();
        if (m == null) return Optional.empty();
        PersistentDataContainer pdc = m.getPersistentDataContainer();
        NamespacedKey nk = new NamespacedKey(plugin, key);
        if (!pdc.has(nk, type)) return Optional.empty();
        return Optional.ofNullable(pdc.get(nk, type));
    }

    /* ------------ Build ------------ */

    public ItemStack build() {
        return stack.clone();
    }
}
