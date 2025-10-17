/*
 * Copyright (c) 2024 PvPDojo Network - All Rights Reserved
 */

package com.yalcinkaya.ctf.util.parametrization.domains;

import com.google.common.collect.Lists;
import com.yalcinkaya.ctf.util.parametrization.Domain;
import com.yalcinkaya.ctf.util.parametrization.Tuple3D;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Box implements Domain<Tuple3D> {

    private final Interval xInterval;
    private final Interval yInterval;
    private final Interval zInterval;

    public Box(Interval xInterval, Interval yInterval, Interval zInterval) {
        this.xInterval = xInterval;
        this.yInterval = yInterval;
        this.zInterval = zInterval;
    }

    public Box(Vector start, Vector end) {
        this.xInterval = new Interval(start.getX(), end.getX());
        this.yInterval = new Interval(start.getY(), end.getY());
        this.zInterval = new Interval(start.getZ(), end.getZ());
    }

    @Override
    public boolean contains(Tuple3D element) {
        return xInterval.contains(element.getX()) && yInterval.contains(element.getY())
                && zInterval.contains(element.getZ());
    }

    @Override
    public Iterator<Tuple3D> iterator(int sampleSize) {
        List<Tuple3D> samples = new ArrayList<>();
        final int samplesPerInterval = (int) Math.pow(sampleSize, 1.0 / 3);
        List<Double> xSamples = Lists.newArrayList(xInterval.iterator(samplesPerInterval));
        List<Double> ySamples = Lists.newArrayList(yInterval.iterator(samplesPerInterval));
        List<Double> zSamples = Lists.newArrayList(zInterval.iterator(samplesPerInterval));

        for (Double x : xSamples) {
            for (Double y : ySamples) {
                for (Double z : zSamples) {
                    samples.add(new Tuple3D(x, y, z));
                }
            }
        }

        return samples.iterator();
    }
}
