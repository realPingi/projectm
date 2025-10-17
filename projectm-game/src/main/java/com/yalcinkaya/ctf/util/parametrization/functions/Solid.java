/*
 * Copyright (c) 2024 PvPDojo Network - All Rights Reserved
 */

package com.yalcinkaya.ctf.util.parametrization.functions;

import com.yalcinkaya.ctf.util.parametrization.Parametrizable;
import com.yalcinkaya.ctf.util.parametrization.Tuple3D;
import com.yalcinkaya.ctf.util.parametrization.domains.Box;
import org.bukkit.util.Vector;

import java.util.function.Function;

public class Solid implements Parametrizable<Tuple3D, Box> {

    private Box box;
    private Function<Tuple3D, Vector> function;

    public Solid(Box box, Function<Tuple3D, Vector> function) {
        this.box = box;
        this.function = function;
    }

    @Override
    public Box getDomain() {
        return box;
    }

    @Override
    public void setDomain(Box box) {
        this.box = box;
    }

    @Override
    public Function<Tuple3D, Vector> getFunction() {
        return function;
    }

    @Override
    public void setFunction(Function<Tuple3D, Vector> function) {
        this.function = function;
    }

    @Override
    public Solid copy() {
        return new Solid(box, function);
    }
}
