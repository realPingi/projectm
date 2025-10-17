package com.yalcinkaya.ctf.kits.clickItems;

import com.yalcinkaya.ctf.kit.ClickItem;
import com.yalcinkaya.ctf.kit.EnergyConsumer;
import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Knife extends ClickItem implements EnergyConsumer {

    private final ItemStack item = CTFUtil.createIcon("Kunai", Material.ARROW);
    private Arrow arrow;

    @Override
    public boolean tryClick(CTFUser activator) {
        if (arrow != null) {
            arrow.remove();
        }
        if (!tryConsume(activator)) {
            return false;
        }

        Player player = CTFUtil.getPlayer(activator);
        arrow = player.launchProjectile(Arrow.class, player.getEyeLocation().getDirection().normalize().multiply(1.75));
        return true;
    }

    public Arrow getArrow() {
        return arrow;
    }

    @Override
    public String getName() {
        return "Kunai";
    }

    @Override
    public ItemStack getItem() {
        return item;
    }

    @Override
    public int getCooldownSeconds() {
        return 30;
    }

    @Override
    public Sound getSound() {
        return Sound.ENTITY_ENDER_PEARL_THROW;
    }

    @Override
    public double getEnergy() {
        return 40;
    }

}
