/*
 * Copyright (c) 2024 PvPDojo Network - All Rights Reserved
 */

package com.yalcinkaya.util.parametrization.builder;

import com.yalcinkaya.util.parametrization.ParametricalBuilder;
import com.yalcinkaya.util.parametrization.Tuple3D;
import com.yalcinkaya.util.parametrization.domains.Box;
import com.yalcinkaya.util.parametrization.functions.Solid;
import org.bukkit.util.Vector;

import java.util.function.Function;

public class SolidBuilder extends ParametricalBuilder<Tuple3D, Box, Solid> {

    public SolidBuilder(Box domain,
                        Function<Tuple3D, Vector> function) {
        super(domain, function);
    }

    @Override
    public Solid build() {
        return new Solid(domain, function);
    }

}
