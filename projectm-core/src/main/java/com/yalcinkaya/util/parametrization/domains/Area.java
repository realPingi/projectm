package com.yalcinkaya.util.parametrization.domains;

import com.google.common.collect.Lists;
import com.yalcinkaya.util.parametrization.Domain;
import com.yalcinkaya.util.parametrization.Tuple2D;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Area implements Domain<Tuple2D> {

    private final Interval horizontal;
    private final Interval vertical;

    public Area(Interval horizontal, Interval vertical) {
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    public Area(double x1, double x2, double y1, double y2) {
        this.horizontal = new Interval(x1, x2);
        this.vertical = new Interval(y1, y2);
    }

    @Override
    public boolean contains(Tuple2D element) {
        return horizontal.contains(element.getX()) && vertical.contains(element.getY());
    }

    @Override
    public Iterator<Tuple2D> iterator(int sampleSize) {
        List<Tuple2D> samples = new ArrayList<>();
        final int samplesPerInterval = (int) Math.sqrt(sampleSize);
        List<Double> xSamples = Lists.newArrayList(horizontal.iterator(samplesPerInterval));
        List<Double> ySamples = Lists.newArrayList(vertical.iterator(samplesPerInterval));
        for (Double x : xSamples) {
            for (Double y : ySamples) {
                samples.add(new Tuple2D(x, y));
            }
        }
        return samples.iterator();
    }
}
