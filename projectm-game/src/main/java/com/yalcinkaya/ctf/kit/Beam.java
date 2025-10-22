package com.yalcinkaya.ctf.kit;

import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import com.yalcinkaya.util.PotentialObject;
import com.yalcinkaya.util.parametrization.VectorUtil;
import com.yalcinkaya.util.parametrization.builder.CurveBuilder;
import com.yalcinkaya.util.parametrization.builder.Illustrations;
import com.yalcinkaya.util.parametrization.builder.MultiColorParticle;
import com.yalcinkaya.util.parametrization.domains.Interval;
import com.yalcinkaya.util.parametrization.functions.Curve;
import com.yalcinkaya.util.parametrization.types.CurveTypes;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public abstract class Beam extends ClickItem implements EnergyConsumer {

    @Override
    public boolean tryClick(CTFUser activator) {
        Player player = CTFUtil.getPlayer(activator);
        shootBeam(activator, player.getLocation().clone().add(0, 0.625, 0), player.getEyeLocation().getDirection().normalize());
        return true;
    }

    public void shootBeam(CTFUser activator, Location start, Vector direction) {
        Curve curve = new CurveBuilder(new Interval(0, getLength()), CurveTypes.LINE).rotate(VectorUtil.getYAxis(), direction).translate(start.toVector()).build();

        final int samples = getLength() * 10;
        final int time = getLength() * 20 / 50;
        MultiColorParticle effect = new MultiColorParticle(getColors(), (int) (16 * getRadius()), 0.125 * getRadius());
        World world = start.getWorld();

        Illustrations.drawSlowlyWithTask(curve, samples, time, effect, world,

                vector -> {
                    Location location = vector.toLocation(world);
                    Set<CTFUser> targets = new HashSet<>();
                    switch (getAffectType()) {
                        case FRIENDLY -> targets = CTFUtil.getNearbyMates(activator, location, getRadius());
                        case HOSTILE -> targets = CTFUtil.getNearbyOpponents(activator, location, getRadius());
                        case NEUTRAL -> targets = CTFUtil.getNearbyUsers(location, getRadius());
                    }
                    targets.remove(activator);
                    CTFUser nearest = targets.stream().min(Comparator.comparingDouble(a -> CTFUtil.getPlayer(a).getLocation().distance(location))).orElse(null);
                    if (nearest != null) {
                        return tryAffect(activator, nearest);
                    }
                    return false;
                });
    }

    public abstract boolean tryAffect(CTFUser shooter, CTFUser hit);

    public abstract Set<PotentialObject<Color>> getColors();

    public abstract int getLength();

    public abstract AffectType getAffectType();

    public abstract double getRadius();

}
