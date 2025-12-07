package com.yalcinkaya.ctf.kits.clickItems;

import com.yalcinkaya.core.util.PotentialObject;
import com.yalcinkaya.core.util.parametrization.VectorUtil;
import com.yalcinkaya.core.util.parametrization.builder.CurveBuilder;
import com.yalcinkaya.core.util.parametrization.builder.Illustrations;
import com.yalcinkaya.core.util.parametrization.builder.MultiColorParticle;
import com.yalcinkaya.core.util.parametrization.domains.Interval;
import com.yalcinkaya.core.util.parametrization.functions.Curve;
import com.yalcinkaya.core.util.parametrization.types.CurveTypes;
import com.yalcinkaya.ctf.CTF;
import com.yalcinkaya.ctf.kit.ClickItem;
import com.yalcinkaya.ctf.kit.EnergyConsumer;
import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.joml.Quaterniond;

import java.util.Set;

public class Ascent extends ClickItem implements EnergyConsumer {

    private final ItemStack item = CTFUtil.createIcon("Ascent", Material.CRIMSON_ROOTS);

    private final double length = 0.6;
    private final double width = 2.1;
    private final int duration = 4;
    private Wings wings;

    @Override
    public boolean tryClick(CTFUser activator) {

        Player player = CTFUtil.getPlayer(activator);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration * 20, 3));
        wings = new Wings(player);
        wings.start();
        activator.playSoundForWorld(Sound.ENTITY_ENDER_DRAGON_GROWL);

        new BukkitRunnable() {

            @Override
            public void run() {
                activator.setCancelNextFallDamage(true);
                wings.cancel();
            }

        }.runTaskLater(CTF.getInstance(), duration * 20);

        return true;
    }

    @Override
    public String getName() {
        return "Ascent";
    }

    @Override
    public ItemStack getItem() {
        return item;
    }

    @Override
    public int getCooldownSeconds() {
        return 20;
    }

    @Override
    public Sound getSound() {
        return Sound.ENTITY_ENDER_DRAGON_SHOOT;
    }

    @Override
    public double getEnergy() {
        return 30;
    }

    public void cancel() {
        wings.cancel();
    }

    public boolean isActive() {
        return wings != null && !wings.isCancelled();
    }

    private class Wings extends BukkitRunnable {

        private final Player holder;
        private final Set<PotentialObject<Color>> colors;
        private final int samples = 80;

        public Wings(Player holder) {
            this.holder = holder;
            colors = CTFUtil.getColorMix(CTFUtil.getUser(holder));
        }

        @Override
        public void run() {
            Vector direction = holder.getEyeLocation().getDirection().setY(0).normalize();
            Vector start = holder.getLocation().clone().add(0, 1, 0).toVector().add(direction.clone().multiply(-1));
            Quaterniond rotation = VectorUtil.getQuaternion(VectorUtil.getYAxis(), direction);
            Vector ortho1 = VectorUtil.rotateVector(VectorUtil.getXAxis(), rotation);
            double rawAngle = VectorUtil.getYAxis().angle(ortho1);
            double angle = direction.getZ() > 0 ? rawAngle : -rawAngle;
            Curve c1 = new CurveBuilder(new Interval(0, 2 * Math.PI), CurveTypes.CIRCLE).scaleX(width).scaleZ(length).rotate(rotation).rotate(direction, angle + Math.PI / 3).translate(start).build();//.scaleX(length).scaleZ(width).rotate(rotation).rotate(direction, angle + Math.PI / 3).translate(start).build();
            Curve c2 = new CurveBuilder(new Interval(0, 2 * Math.PI), CurveTypes.CIRCLE).scaleX(width).scaleZ(length).rotate(rotation).rotate(direction, angle - Math.PI / 3).translate(start).build();//.scaleX(length).scaleZ(width).rotate(rotation).rotate(direction, angle - Math.PI / 3).translate(start).build();
            Illustrations.drawFast(c1, samples, new MultiColorParticle(colors), holder.getWorld());
            Illustrations.drawFast(c2, samples, new MultiColorParticle(colors), holder.getWorld());
        }

        public void start() {
            runTaskTimer(CTF.getInstance(), 0, 1);
        }

    }

}
