package com.yalcinkaya.ctf.kits.clickItems;

import com.yalcinkaya.core.util.CoreUtil;
import com.yalcinkaya.ctf.kit.ClickItem;
import com.yalcinkaya.ctf.kit.EnergyConsumer;
import com.yalcinkaya.ctf.kits.Create;
import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import com.yalcinkaya.ctf.util.TempBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class Wall extends ClickItem implements EnergyConsumer {

    private static final int length = 5;
    private static final int width = 10;
    private static final int duration = 3;
    private final ItemStack item = CTFUtil.createIcon("Wall", Material.SCULK_VEIN);

    @Override
    public boolean tryClick(CTFUser activator) {
        Player player = CTFUtil.getPlayer(activator);
        Vector direction = player.getEyeLocation().getDirection().normalize();
        Vector simplified = CoreUtil.simplifyDirection(direction);
        Location start = player.getLocation().clone().add(0, 1, 0).add(simplified.clone().multiply(3));
        CTFUtil.centeredPlane(start, simplified, length, width).forEach(block -> new TempBlock(block, CoreUtil.getRandom(Create.materials), true, duration));
        return true;
    }

    @Override
    public String getName() {
        return "Wall";
    }

    @Override
    public ItemStack getItem() {
        return item;
    }

    @Override
    public int getCooldownSeconds() {
        return 60;
    }

    @Override
    public Sound getSound() {
        return Sound.BLOCK_WOOD_PLACE;
    }

    @Override
    public double getEnergy() {
        return 50;
    }
}
