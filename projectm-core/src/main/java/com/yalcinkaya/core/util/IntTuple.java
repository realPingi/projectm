package com.yalcinkaya.core.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.block.Block;

import java.util.Objects;

@Getter
@AllArgsConstructor
public class IntTuple {

    private int x;
    private int z;

    public static IntTuple tuple(Block block) {
        return new IntTuple(block.getX(), block.getZ());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof IntTuple tuple && tuple.getX() == x && tuple.getZ() == z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }
}
