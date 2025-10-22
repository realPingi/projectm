package com.yalcinkaya.core.util.parametrization.builder;

import com.yalcinkaya.core.util.parametrization.ParametricalBuilder;
import com.yalcinkaya.core.util.parametrization.Tuple2D;
import com.yalcinkaya.core.util.parametrization.domains.Area;
import com.yalcinkaya.core.util.parametrization.functions.Surface;
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
