package com.yalcinkaya.util.parametrization.types;

import lombok.Getter;
import org.bukkit.util.Vector;

import java.util.function.Function;

@Getter
public class CurveTypes {

    public static Function<Double, Vector> CIRCLE = t -> new Vector(Math.cos(t), 0, Math.sin(t));
    public static Function<Double, Vector> SPIRAL = t -> new Vector(Math.cos(t), t, Math.sin(t));
    public static Function<Double, Vector> LINE = t -> new Vector(0, t, 0);

}
