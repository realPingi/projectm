package com.yalcinkaya.ctf.util.parametrization.builder;

import com.yalcinkaya.ctf.util.parametrization.ParametricalBuilder;
import com.yalcinkaya.ctf.util.parametrization.domains.Interval;
import com.yalcinkaya.ctf.util.parametrization.functions.Curve;
import org.bukkit.util.Vector;

import java.util.function.Function;

public class CurveBuilder extends ParametricalBuilder<Double, Interval, Curve> {

    public CurveBuilder(Interval domain, Function<Double, Vector> function) {
        super(domain, function);
    }

    @Override
    public Curve build() {
        return new Curve(domain, function);
    }
}
