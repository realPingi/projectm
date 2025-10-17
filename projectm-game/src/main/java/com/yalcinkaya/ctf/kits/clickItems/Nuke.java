package com.yalcinkaya.ctf.kits.clickItems;

import com.yalcinkaya.ctf.CTF;
import com.yalcinkaya.ctf.kit.ClickItem;
import com.yalcinkaya.ctf.kit.EnergyConsumer;
import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import com.yalcinkaya.ctf.util.MessageType;
import com.yalcinkaya.ctf.util.PotentialObject;
import com.yalcinkaya.ctf.util.parametrization.builder.CurveBuilder;
import com.yalcinkaya.ctf.util.parametrization.builder.Illustrations;
import com.yalcinkaya.ctf.util.parametrization.builder.MultiColorParticle;
import com.yalcinkaya.ctf.util.parametrization.domains.Interval;
import com.yalcinkaya.ctf.util.parametrization.functions.Curve;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Set;

public class Nuke extends ClickItem implements EnergyConsumer {

    private final ItemStack item = CTFUtil.createIcon("Nuke", Material.TNT);

    @Getter
    private int stage = 0;
    private Zombie fakePlayer;
    private ItemStack[] armor;

    @Override
    public boolean tryClick(CTFUser activator) {

        if (stage == 0) {

            stage++;
            CTFUtil.playSoundForAll(Sound.ENTITY_SHULKER_TELEPORT);
            setGhost(activator);
            new BukkitRunnable() {

                @Override
                public void run() {
                    if (stage == 1) {
                        stage = 0;
                        setNormal(activator);
                        useCooldown();
                        consume(activator);
                    }
                }

            }.runTaskLater(CTF.getInstance(), 20 * 8);
            return false;
        }

        Player player = CTFUtil.getPlayer(activator);
        World world = player.getWorld();
        Location location = player.getLocation();

        Block endBlock = world.getHighestBlockAt(location.getBlockX(), location.getBlockZ());
        Location start = endBlock.getLocation().clone().add(0, 50, 0);

        if (endBlock == null || !endBlock.getType().isSolid()) {
            return false;
        }

        double distance = start.distance(endBlock.getLocation());
        Curve curve = new CurveBuilder(new Interval(0, distance), t -> new Vector(0, -t, 0)).translate(start.toVector()).build();
        Set<PotentialObject<Color>> colors = CTFUtil.getColorMix(activator);
        MultiColorParticle effect = new MultiColorParticle(colors, 10, 0);

        Illustrations.drawSlowlyWithTask(curve, (int) (distance * 10), 2 * 20, effect, world,

                vector -> {
                    if (vector.toLocation(world).getBlock().getType().isSolid()) {
                        CTFUtil.createExplosion(vector.toLocation(world).clone().add(0, 1, 0), 10, 8, colors.stream().map(PotentialObject::getContent).toArray(Color[]::new));
                        return true;
                    }
                    return false;
                });

        CTFUtil.broadcastMessageForAll(MessageType.BROADCAST, CTFUtil.getColoredName(activator), " is going to nuke this place up!");

        stage = 0;
        setNormal(activator);

        return true;
    }

    private void setGhost(CTFUser user) {
        fakePlayer = CTFUtil.spawnAlibiPlayer(user);
        Player player = CTFUtil.getPlayer(user);
        armor = player.getEquipment().getArmorContents();
        player.getEquipment().setArmorContents(new ItemStack[1]);
        player.setInvisible(true);
        player.teleport(player.getLocation().clone().add(0, 1, 0));
        CTFUtil.setFly(player, true);
        player.setFlySpeed(1);
        user.setPreventCapture(true);
    }

    private void setNormal(CTFUser user) {
        Player player = CTFUtil.getPlayer(user);
        player.teleport(fakePlayer.getLocation());
        fakePlayer.remove();
        CTFUtil.setFly(player, false);
        player.getEquipment().setArmorContents(armor);
        player.setInvisible(false);
        user.setPreventCapture(false);
    }

    @Override
    public String getName() {
        return "Nuke";
    }

    @Override
    public ItemStack getItem() {
        return item;
    }

    @Override
    public int getCooldownSeconds() {
        return 70;
    }

    @Override
    public Sound getSound() {
        return Sound.BLOCK_BONE_BLOCK_PLACE;
    }

    @Override
    public double getEnergy() {
        return 100;
    }
}
