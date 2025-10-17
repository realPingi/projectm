package com.yalcinkaya.ctf.util.parametrization.builder;

import com.google.common.util.concurrent.AtomicDouble;
import com.yalcinkaya.ctf.util.SmartRunnable;
import com.yalcinkaya.ctf.util.parametrization.Parametrizable;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.function.Function;

public class Illustrations {

    public static void drawFast(Parametrizable parametrizable, int sampleSize, MultiColorParticle effect,
                                World world) {
        new Illustration(parametrizable, sampleSize, sampleSize, effect, world).start();
    }

    public static void drawFastWithTask(Parametrizable parametrizable, int sampleSize,
                                        MultiColorParticle effect, World world, Function<Vector, Boolean> task) {
        new Illustration(parametrizable, sampleSize, sampleSize, effect, world).supplyTask(task)
                .start();
    }

    public static void drawSlowly(Parametrizable parametrizable, int sampleSize, int timeTicks,
                                  MultiColorParticle effect, World world) {
        new Illustration(parametrizable, sampleSize, Math.max(sampleSize / timeTicks, 1), effect,
                world).start();
    }

    public static void drawSlowlyWithTask(Parametrizable parametrizable, int sampleSize,
                                          int timeTicks, MultiColorParticle effect, World world, Function<Vector, Boolean> task) {
        new Illustration(parametrizable, sampleSize, Math.max(sampleSize / timeTicks, 1), effect,
                world).supplyTask(task).start();
    }

    public static int display(Parametrizable parametrizable, int sampleSize, int timeTicks,
                              MultiColorParticle effect, World world) {
        return new SmartRunnable() {
            @Override
            public void cycle() {
                drawFast(parametrizable, sampleSize, effect, world);
            }
        }.createTimer(1, timeTicks);
    }

    public static int displayWithTask(Parametrizable parametrizable, int sampleSize, int timeTicks,
                                      MultiColorParticle effect, World world, Function<Vector, Boolean> task) {
        return new SmartRunnable() {
            @Override
            public void cycle() {
                drawFastWithTask(parametrizable, sampleSize, effect, world, task);
            }
        }.createTimer(1, timeTicks);
    }

    public static int animate(Parametrizable parametrizable, int sampleSize, int timeTicks,
                              MultiColorParticle effect, World world, double speed, Function<Double, Function<Vector, Vector>>... animations) {
        AtomicDouble t = new AtomicDouble();
        return new SmartRunnable() {
            @Override
            public void cycle() {
                Parametrizable cloned = parametrizable.copy();
                for (Function<Double, Function<Vector, Vector>> animation : animations) {
                    cloned.setFunction(cloned.getFunction().andThen(animation.apply(t.get())));
                }
                drawFast(cloned, sampleSize, effect, world);
                t.getAndAdd(speed);
            }
        }.createTimer(1, timeTicks);
    }

    public static int animateWithTask(Parametrizable parametrizable, int sampleSize, int timeTicks,
                                      MultiColorParticle effect, World world, double speed, Function<Vector, Boolean> task, Function<Double, Function<Vector, Vector>>... animations) {
        AtomicDouble t = new AtomicDouble();
        return new SmartRunnable() {
            @Override
            public void cycle() {
                Parametrizable cloned = parametrizable.copy();
                for (Function<Double, Function<Vector, Vector>> animation : animations) {
                    cloned.setFunction(cloned.getFunction().andThen(animation.apply(t.get())));
                }
                drawFastWithTask(cloned, sampleSize, effect, world, task);
                t.getAndAdd(speed);
            }
        }.createTimer(1, timeTicks);
    }

}
