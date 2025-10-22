package com.yalcinkaya.core.util.parametrization.functions;

import com.yalcinkaya.core.util.parametrization.Parametrizable;
import com.yalcinkaya.core.util.parametrization.Tuple2D;
import com.yalcinkaya.core.util.parametrization.domains.Area;
import org.bukkit.util.Vector;

import java.util.function.Function;

public class Surface implements Parametrizable<Tuple2D, Area> {

    private Area area;
    private Function<Tuple2D, Vector> function;

    public Surface(Area area, Function<Tuple2D, Vector> function) {
        this.area = area;
        this.function = function;
    }

    @Override
    public Area getDomain() {
        return area;
    }

    @Override
    public void setDomain(Area area) {
        this.area = area;
    }

    @Override
    public Function<Tuple2D, Vector> getFunction() {
        return function;
    }

    @Override
    public void setFunction(Function<Tuple2D, Vector> function) {
        this.function = function;
    }

    @Override
    public Surface copy() {
        return new Surface(area, function);
    }

}
