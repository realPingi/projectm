package com.yalcinkaya.ctf.kits.clickItems;

import com.yalcinkaya.core.util.CoreUtil;
import com.yalcinkaya.core.util.MessageType;
import com.yalcinkaya.core.util.SmoothTravel;
import com.yalcinkaya.ctf.kit.ClickItem;
import com.yalcinkaya.ctf.kit.EnergyConsumer;
import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

public class Rewind extends ClickItem implements EnergyConsumer {

    private final ItemStack item = CTFUtil.createIcon("Rewind", Material.CRYING_OBSIDIAN);
    @Getter
    private final int rewindSpan = 4;

    @Setter
    private Location someTimeAgo;
    private SmoothTravel smoothTravel;

    @Override
    public boolean tryClick(CTFUser activator) {

        if (!isSafeTravel()) {
            activator.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Warp destination was too dangerous."));
            return false;
        }

        if (!isCompleted()) {
            activator.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Can't cancel the warp process."));
            return false;
        }

        smoothTravel = new SmoothTravel(CTFUtil.getPlayer(activator), someTimeAgo, 1, true, false);
        smoothTravel.start();
        return true;
    }

    private boolean isSafeTravel() {

        if (someTimeAgo == null) {
            return false;
        }

        BlockIterator iterator = new BlockIterator(someTimeAgo.getWorld(), someTimeAgo.toVector(), new Vector(0, -1, 0), 0, 10);
        Block block = iterator.next();
        while (iterator.hasNext() && !block.getType().isSolid()) {
            block = iterator.next();
        }

        return block.getType().isSolid();
    }

    @Override
    public String getName() {
        return "Rewind";
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
        return Sound.BLOCK_END_PORTAL_FRAME_FILL;
    }

    @Override
    public double getEnergy() {
        return 30;
    }

    public boolean isCompleted() {
        return smoothTravel == null || smoothTravel.isCompleted();
    }

}
