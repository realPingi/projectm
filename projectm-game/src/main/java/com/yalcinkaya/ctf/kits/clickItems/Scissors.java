package com.yalcinkaya.ctf.kits.clickItems;

import com.yalcinkaya.ctf.kit.ClickItem;
import com.yalcinkaya.ctf.kit.Counter;
import com.yalcinkaya.ctf.kit.EnergyConsumer;
import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import com.yalcinkaya.ctf.util.SmartRunnable;
import com.yalcinkaya.ctf.util.parametrization.VectorUtil;
import com.yalcinkaya.ctf.util.parametrization.builder.Illustrations;
import com.yalcinkaya.ctf.util.parametrization.builder.MultiColorParticle;
import com.yalcinkaya.ctf.util.parametrization.builder.SurfaceBuilder;
import com.yalcinkaya.ctf.util.parametrization.domains.Area;
import com.yalcinkaya.ctf.util.parametrization.functions.Surface;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.joml.Quaterniond;

import java.util.HashSet;
import java.util.Set;

public class Scissors extends ClickItem implements EnergyConsumer {

    private final ItemStack item = CTFUtil.createIcon("Scissors", Material.TUBE_CORAL_FAN);
    private final Set<Player> cutPlayers = new HashSet<>();

    private final int stacksNeeded = 5;
    @Getter
    private final Counter stacks = new Counter("Stacks", ChatColor.BLUE, stacksNeeded * 8);

    @Override
    public boolean tryClick(CTFUser activator) {
        int count = (int) Math.floor((double) stacks.getCount() / stacksNeeded);

        if (count == 0) {
            stacks.sendWarning(activator);
            return false;
        }

        stacks.add(-count * stacksNeeded);
        new SmartRunnable() {
            @Override
            public void cycle() {
                cut(activator);
            }
        }.createTimer(20, count);
        return true;
    }

    private void cut(CTFUser activator) {
        Player player = CTFUtil.getPlayer(activator);
        Vector start = player.getLocation().add(0, 1, 0).toVector();
        Vector direction = player.getEyeLocation().getDirection().normalize().setY(0);
        Quaterniond rotation = VectorUtil.getQuaternion(VectorUtil.getXAxis(), direction);
        Surface surface = new SurfaceBuilder(new Area(0, 6, -0.25 * Math.PI, 0.25 * Math.PI), p -> new Vector(p.getX() * Math.cos(p.getY()), 0, p.getX() * Math.sin(p.getY()))).rotate(rotation).translate(start).build();
        Illustrations.drawFastWithTask(surface, 256, new MultiColorParticle(CTFUtil.getColorMix(activator), 1, 0), player.getWorld(),
                vector -> {
                    CTFUtil.getNearbyOpponents(activator, vector.toLocation(player.getWorld()), 3).forEach(user -> {
                        Player cutPlayer = CTFUtil.getPlayer(user);
                        if (!cutPlayer.getUniqueId().equals(player.getUniqueId()) && !cutPlayers.contains(cutPlayer)) {
                            cutPlayer.damage(4);
                            cutPlayers.add(cutPlayer);
                        }
                    });
                    return false;
                });
        activator.playSoundForWorld(getSound());
        cutPlayers.clear();
    }

    @Override
    public String getName() {
        return "Scissors";
    }

    @Override
    public ItemStack getItem() {
        return item;
    }

    @Override
    public int getCooldownSeconds() {
        return 40;
    }

    @Override
    public Sound getSound() {
        return Sound.BLOCK_LANTERN_BREAK;
    }

    @Override
    public double getEnergy() {
        return 20;
    }
}
