package com.yalcinkaya.core.util.parametrization.functions;

import com.yalcinkaya.core.util.parametrization.Parametrizable;
import com.yalcinkaya.core.util.parametrization.domains.Interval;
import org.bukkit.util.Vector;

import java.util.function.Function;

public class Curve implements Parametrizable<Double, Interval> {

    private Interval interval;
    private Function<Double, Vector> function;

    public Curve(Interval interval, Function<Double, Vector> function) {
        this.interval = interval;
        this.function = function;
    }

    @Override
    public Interval getDomain() {
        return interval;
    }

    @Override
    public void setDomain(Interval interval) {
        this.interval = interval;
    }

    @Override
    public Function<Double, Vector> getFunction() {
        return function;
    }

    @Override
    public void setFunction(Function<Double, Vector> function) {
        this.function = function;
    }

    @Override
    public Curve copy() {
        return new Curve(interval, function);
    }

}
