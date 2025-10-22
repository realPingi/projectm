/*
 * Copyright (c) 2024 PvPDojo Network - All Rights Reserved
 */

package com.yalcinkaya.util.parametrization;

import lombok.Data;

@Data

public class Tuple3D {

    double x;
    double y;
    double z;

    public Tuple3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

}
