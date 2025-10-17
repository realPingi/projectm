package com.yalcinkaya.ctf.kits.clickItems;

import com.yalcinkaya.ctf.CTF;
import com.yalcinkaya.ctf.kit.ClickItem;
import com.yalcinkaya.ctf.kit.EnergyConsumer;
import com.yalcinkaya.ctf.kits.Create;
import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import com.yalcinkaya.ctf.util.TempBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

public class Tunnel extends ClickItem implements EnergyConsumer {

    private static final int length = 2;
    private static final int width = 2;
    private static final int depth = 15;
    private static final int duration = 3;
    private final ItemStack item = CTFUtil.createIcon("Tunnel", Material.LARGE_AMETHYST_BUD);

    @Override
    public boolean tryClick(CTFUser activator) {
        Player player = CTFUtil.getPlayer(activator);
        Vector direction = player.getEyeLocation().getDirection().normalize();
        Location start = player.getLocation().clone().add(0, 1, 0).add(direction.clone().multiply(3));

        BlockIterator depthIterator = new BlockIterator(player.getWorld(), start.toVector(), direction, 0, depth);
        int delay = 0;
        while (depthIterator.hasNext()) {
            Block node = depthIterator.next();
            int d = delay;
            CTFUtil.hollowRing(node.getLocation(), direction, length, width).forEach(block -> {

                new BukkitRunnable() {

                    @Override
                    public void run() {
                        new TempBlock(block, CTFUtil.getRandom(Create.materials), true, duration);
                    }

                }.runTaskLater(CTF.getInstance(), d);
            });
            delay += 5;
        }

        return true;
    }

    @Override
    public String getName() {
        return "Tunnel";
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
