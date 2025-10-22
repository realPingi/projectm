/*
 * Copyright (c) 2024 PvPDojo Network - All Rights Reserved
 */

package com.yalcinkaya.util.parametrization.types;

import com.yalcinkaya.util.parametrization.Tuple3D;
import org.bukkit.util.Vector;

import java.util.function.Function;

public class SolidTypes {

    public static Function<Tuple3D, Vector> CUBOID = params ->
            new Vector(params.getY(),
                    params.getX(),
                    params.getZ());

}
