package com.yalcinkaya.util.parametrization.builder;

import com.yalcinkaya.util.parametrization.ParametricalBuilder;
import com.yalcinkaya.util.parametrization.Tuple2D;
import com.yalcinkaya.util.parametrization.domains.Area;
import com.yalcinkaya.util.parametrization.functions.Surface;
import org.bukkit.util.Vector;

import java.util.function.Function;

public class SurfaceBuilder extends ParametricalBuilder<Tuple2D, Area, Surface> {

    public SurfaceBuilder(Area domain, Function<Tuple2D, Vector> function) {
        super(domain, function);
    }

    @Override
    public Surface build() {
        return new Surface(domain, function);
    }
}
