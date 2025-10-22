package com.yalcinkaya.core.util;

import lombok.Getter;
import lombok.Setter;

@Getter
public class PotentialObject<T> {

    private T content;
    private int probability;
    @Setter
    private int position;

    public PotentialObject(T content, int probability) {
        this.content = content;
        this.probability = probability;
    }

}
