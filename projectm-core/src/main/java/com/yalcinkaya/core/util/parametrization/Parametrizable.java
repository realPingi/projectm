package com.yalcinkaya.core.util.parametrization;

import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public interface Parametrizable<P, D extends Domain<P>> {

    D getDomain();

    void setDomain(D domain);

    Function<P, Vector> getFunction();

    void setFunction(Function<P, Vector> function);

    Parametrizable<P, D> copy();

    default Vector eval(P parameter) {
        if (!getDomain().contains(parameter)) {
            throw new IllegalArgumentException();
        }
        return getFunction().apply(parameter);
    }

    default Iterator<Vector> image(int sampleSize) {
        List<Vector> image = new ArrayList<>();
        getDomain().iterator(sampleSize).forEachRemaining(p -> image.add(eval(p)));
        return image.iterator();
    }

}
