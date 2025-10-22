package com.yalcinkaya.core.util.parametrization.types;

import com.yalcinkaya.core.util.parametrization.Tuple2D;
import lombok.Getter;
import org.bukkit.util.Vector;

import java.util.function.Function;

@Getter
public class SurfaceTypes {

    public static Function<Tuple2D, Vector> SPHERE_1 = angles -> new Vector(
            Math.cos(angles.getX()) * Math.sin(angles.getY()),
            Math.cos(angles.getY()),
            Math.sin(angles.getX()) * Math.sin(angles.getY()));


    public static Function<Tuple2D, Vector> SPHERE_2 = angles -> new Vector(
            Math.cos(angles.getY()) * Math.sin(angles.getX()),
            Math.cos(angles.getX()),
            Math.sin(angles.getY()) * Math.sin(angles.getX()));

    public static Function<Tuple2D, Vector> CYLINDER_1 = parameters -> new
            Vector(Math.cos(parameters.getX()), parameters.getY(),
            Math.sin(parameters.getX()));

    public static Function<Tuple2D, Vector> CYLINDER_2 = parameters -> new
            Vector(Math.cos(parameters.getY()), parameters.getX(),
            Math.sin(parameters.getY()));

    public static Function<Tuple2D, Vector> FLAT = parameters -> new Vector(parameters.getX(), 0,
            parameters.getY());

    public static Function<Tuple2D, Vector> torus(double outerRadius, double innerRadius) {
        return angles -> new Vector(
                (outerRadius + innerRadius * Math.cos(angles.getX())) * Math.cos(angles.getY()),
                (outerRadius + innerRadius * Math.cos(angles.getX())) * Math.sin(angles.getY()),
                innerRadius * Math.sin(angles.getX()));
    }
}
