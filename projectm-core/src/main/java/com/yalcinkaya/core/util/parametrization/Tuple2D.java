package com.yalcinkaya.core.util.parametrization;

import lombok.Data;

@Data
public class Tuple2D {

    double x;
    double y;

    public Tuple2D(double x, double y) {
        this.x = x;
        this.y = y;
    }
}
