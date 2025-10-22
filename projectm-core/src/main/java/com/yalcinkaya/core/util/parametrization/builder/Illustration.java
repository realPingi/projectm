package com.yalcinkaya.core.util.parametrization.builder;

import com.yalcinkaya.core.util.SmartRunnable;
import com.yalcinkaya.core.util.parametrization.Parametrizable;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.function.Function;

public class Illustration extends SmartRunnable {

    private final Iterator<Vector> image;
    private final int sampleSize;
    private final int samplesPerTick;
    private final World world;
    private final MultiColorParticle effect;
    private Function<Vector, Boolean> task;

    public Illustration(Parametrizable parametrizable, int sampleSize, int samplesPerTick,
                        MultiColorParticle effect, World world) {
        this.sampleSize = sampleSize;
        this.samplesPerTick = samplesPerTick;
        this.world = world;
        this.effect = effect;
        image = parametrizable.image(sampleSize);
    }

    @Override
    public void cycle() {
        for (int i = 0; i < samplesPerTick; i++) {
            if (!image.hasNext()) {
                cancel();
                return;
            }

            Vector next = image.next();
            spawnEffect(next.toLocation(world));
            if (task != null) {
                if (task.apply(next)) {
                    cancel();
                    return;
                }
                ;
            }
        }
    }

    public Illustration supplyTask(Function<Vector, Boolean> task) {
        this.task = task;
        return this;
    }

    public void start() {
        if (samplesPerTick < sampleSize) {
            createTimer(1, -1);
        } else {
            while (image.hasNext()) {
                Vector next = image.next();
                spawnEffect(next.toLocation(world));
                if (task != null) {
                    if (task.apply(next)) {
                        return;
                    }
                    ;
                }
            }
        }
    }

    private void spawnEffect(Location location) {
        world.spawnParticle(Particle.DUST, location, effect.getCount(), effect.getOffset(), effect.getOffset(), effect.getOffset(), new Particle.DustOptions(effect.pop(), 1.0F));
    }

}
