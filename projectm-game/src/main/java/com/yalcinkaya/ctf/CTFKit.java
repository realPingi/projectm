package com.yalcinkaya.ctf;

import com.yalcinkaya.ctf.kit.Kit;
import com.yalcinkaya.ctf.kits.*;
import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.function.Supplier;

@Getter
public enum CTFKit {

    AGENTZERO("Agent-0", AgentZero::new, CTFUtil.createIcon("Agent-0", Material.CRYING_OBSIDIAN, "Click " + emph("Rewind") + " to teleport to the location you've been 4 seconds ago.")),
    DRYEET("Dr. Yeet", DrYeet::new, CTFUtil.createIcon("Dr. Yeet", Material.DIAMOND, "You gain 1 charge per melee hit taken. If you have 15 charges, click " + emph("Reflect") + " to activate a barrier that will reflect the next incoming melee hit.")),
    GWEN("Gwen", Gwen::new, CTFUtil.createIcon("Gwen", Material.TUBE_CORAL_FAN, "You gain 1 stack per melee hit dealt. Click " + emph("Scissors") + " to perform cuts. You will perform 1 cut for every 5 stacks. A cut will deal 2 hearts of damage to everyone in its range.")),
    KRE8("K:Re/8", Create::new, CTFUtil.createIcon("K:Re/8", Material.LARGE_AMETHYST_BUD, "Click " + emph("Wall") + " to place a wall. Click " + emph("Tunnel") + " to create a tunnel.")),
    KUNAI("Kunai", Kunai::new, CTFUtil.createIcon("Kunai", Material.ARROW, "Click " + emph("Kunai") + " to shoot an arrow. Crouch to teleport to its location midair.")),
    SILVY("Silvy", Silvy::new, CTFUtil.createIcon("Silvy", Material.PINK_GLAZED_TERRACOTTA, "Click " + emph("Extract") + " to shoot a beam. On contact, it will set the player's maximum health to 15 hearts and restore your own health.")),
    SOLAR("Solar", Solar::new, CTFUtil.createIcon("Solar", Material.CAMPFIRE, "Click " + emph("Blaze") + " to send out a sun that will give regeneration and resistance to nearby allies. Crouch to shoot.")),
    TOUKA("Touka", Touka::new, CTFUtil.createIcon("Touka", Material.CRIMSON_ROOTS, "Click " + emph("Ascent") + " to gain levitation for 4 seconds.")),
    TYPHON("Typhon", Typhon::new, CTFUtil.createIcon("Typhon", Material.GRAY_GLAZED_TERRACOTTA, "Shoot an arrow that will create an area of levitation upon landing."));

    private String name;
    private Supplier<Kit> supplier;
    private ItemStack icon;

    CTFKit(String name, Supplier<Kit> supplier, ItemStack icon) {
        this.name = name;
        this.supplier = supplier;
        this.icon = icon;
    }

    public static ItemStack[] getIcons() {
        return Arrays.stream(values()).map(CTFKit::getIcon).toArray(ItemStack[]::new);
    }

    public static CTFKit getFromString(String name) {
        return Arrays.stream(values()).filter(ctfKit -> ctfKit.name().toLowerCase().equals(name.toLowerCase())).findFirst().orElse(null);
    }

    public static CTFKit getFromIcon(ItemStack icon) {
        return Arrays.stream(values()).filter(ctfKit -> ctfKit.getIcon().equals(icon)).findFirst().orElse(null);
    }

    public static CTFKit getFromKit(Kit kit) {
        return Arrays.stream(values()).filter(ctfKit -> ctfKit.getSupplier().get().getClass().equals(kit.getClass())).findFirst().orElse(null);
    }

    public static void setKit(CTFUser user, CTFKit kit) {
        user.setKit(kit.getKit());
    }

    private static String emph(String string) {
        return "<light_purple>" + string + "</light_purple>";
    }

    public Kit getKit() {
        return getSupplier().get();
    }

}
