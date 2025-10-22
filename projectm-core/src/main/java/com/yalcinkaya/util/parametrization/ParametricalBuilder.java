package com.yalcinkaya.util.parametrization;

import org.bukkit.util.Vector;
import org.joml.Quaterniond;

import java.util.function.Function;

public abstract class ParametricalBuilder<E, D extends Domain<E>, P extends Parametrizable<E, D>> {

    protected D domain;
    protected Function<E, Vector> function;

    public ParametricalBuilder(D domain, Function<E, Vector> function) {
        this.domain = domain;
        this.function = function;
    }

    public ParametricalBuilder<E, D, P> translate(Vector translation) {
        transform(vector -> vector.add(translation));
        return this;
    }

    public ParametricalBuilder<E, D, P> scale(double scale) {
        scaleXYZ(scale, scale, scale);
        return this;
    }

    public ParametricalBuilder<E, D, P> scaleX(double scale) {
        scaleXYZ(scale, 1, 1);
        return this;
    }

    public ParametricalBuilder<E, D, P> scaleY(double scale) {
        scaleXYZ(1, scale, 1);
        return this;
    }

    public ParametricalBuilder<E, D, P> scaleZ(double scale) {
        scaleXYZ(1, 1, scale);
        return this;
    }

    public ParametricalBuilder<E, D, P> scaleXYZ(double xMod, double yMod, double zMod) {
        transform(vector -> vector.multiply(new Vector(xMod, yMod, zMod)));
        return this;
    }


    public ParametricalBuilder<E, D, P> rotate(Vector axis, double angle) {
        transform(vector -> VectorUtil.rotateAboutAxis(vector, axis, angle));
        return this;
    }

    public ParametricalBuilder<E, D, P> rotate(Vector from, Vector to) {
        transform(VectorUtil.getRotation(from, to));
        return this;
    }

    public ParametricalBuilder<E, D, P> rotate(Quaterniond quaterniond) {
        transform(vector -> VectorUtil.rotateVector(vector, quaterniond));
        return this;
    }

    public ParametricalBuilder<E, D, P> transform(Function<Vector, Vector> trafo) {
        this.function = function.andThen(trafo);
        return this;
    }

    public abstract P build();
}
