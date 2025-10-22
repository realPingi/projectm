package com.yalcinkaya.ctf.kits.clickItems;

import com.yalcinkaya.core.util.CoreUtil;
import com.yalcinkaya.core.util.PotentialObject;
import com.yalcinkaya.core.util.parametrization.VectorUtil;
import com.yalcinkaya.core.util.parametrization.builder.Illustrations;
import com.yalcinkaya.core.util.parametrization.builder.MultiColorParticle;
import com.yalcinkaya.core.util.parametrization.builder.SurfaceBuilder;
import com.yalcinkaya.core.util.parametrization.domains.Area;
import com.yalcinkaya.core.util.parametrization.domains.Interval;
import com.yalcinkaya.core.util.parametrization.functions.Surface;
import com.yalcinkaya.core.util.parametrization.types.SurfaceTypes;
import com.yalcinkaya.ctf.kit.ClickItem;
import com.yalcinkaya.ctf.kit.EnergyConsumer;
import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Set;

public class Blaze extends ClickItem implements EnergyConsumer {

    private static final Set<PotentialObject<Color>> colors = CoreUtil.mixColors(Color.fromRGB(255, 189, 66), Color.fromRGB(186, 122, 2), Color.fromRGB(242, 203, 131), 70, 50, 20);
    private final ItemStack item = CTFUtil.createIcon("Blaze", Material.CAMPFIRE);
    private final int duration = 12;
    private final int radius = 5;
    private final int effectDuration = 15;

    @Override
    public boolean tryClick(CTFUser activator) {
        Player player = CTFUtil.getPlayer(activator);
        Vector direction = player.getEyeLocation().getDirection().normalize().setY(0);
        Location center = player.getLocation().add(0, radius + 2, 0);
        World world = player.getWorld();
        boolean shoot = player.isSneaking();
        Surface sphere = new SurfaceBuilder(new Area(new Interval(0, Math.PI), new Interval(0, 2 * Math.PI)), SurfaceTypes.SPHERE_1).scale(radius).translate(center.toVector()).build();
        Illustrations.animateWithTask(sphere, 512, duration * 20, new MultiColorParticle(colors), world, 0.05,
                vector -> {
                    CTFUtil.getNearbyMates(activator, vector.toLocation(world), radius).stream().map(CTFUtil::getPlayer).forEach(mate -> {
                        mate.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, effectDuration * 20, 3));
                        mate.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, effectDuration * 20, 0));
                    });
                    return false;
                }, t -> (vector -> VectorUtil.rotateAboutAxis(vector, new Vector(1, 1, 1), center.toVector(), t).add(shoot ? direction.clone().multiply(10 * t) : new Vector(0, 0, 0))));
        return true;
    }

    @Override
    public String getName() {
        return "Blaze";
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
        return Sound.ENTITY_DRAGON_FIREBALL_EXPLODE;
    }

    @Override
    public double getEnergy() {
        return 40;
    }
}
