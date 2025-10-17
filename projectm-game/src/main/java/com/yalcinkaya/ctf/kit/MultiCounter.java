package com.yalcinkaya.ctf.kit;

import java.util.Arrays;

public interface MultiCounter {

    Counter[] getCounters();

    default Counter getCounter(String name) {
        return Arrays.stream(getCounters()).filter(counter -> counter.getName().equals(name)).findFirst().orElse(null);
    }
}
