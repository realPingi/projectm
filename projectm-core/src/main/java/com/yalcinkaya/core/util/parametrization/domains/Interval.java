package com.yalcinkaya.core.util.parametrization.domains;

import com.yalcinkaya.core.util.parametrization.Domain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public record Interval(double a, double b) implements Domain<Double> {

    public Interval {

        if (b <= a) {
            throw new IllegalArgumentException();
        }

    }

    public double length() {
        return b - a;
    }

    @Override
    public boolean contains(Double element) {
        return a <= element && element <= b;
    }

    @Override
    public Iterator<Double> iterator(int sampleSize) {
        List<Double> samples = new ArrayList<>();
        final double step = length() / sampleSize;
        double x = a;
        while (contains(x)) {
            samples.add(x);
            x += step;
        }
        return samples.iterator();
    }
}
