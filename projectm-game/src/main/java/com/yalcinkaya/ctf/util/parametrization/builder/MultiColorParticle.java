package com.yalcinkaya.ctf.util.parametrization.builder;

import com.yalcinkaya.ctf.util.CTFUtil;
import com.yalcinkaya.ctf.util.PotentialObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Color;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Getter
@AllArgsConstructor
public class MultiColorParticle {

    private Set<PotentialObject<Color>> colors;
    private int count;
    private double offset;

    public MultiColorParticle(Color color) {
        this.colors = new HashSet<>(Arrays.asList(new PotentialObject<>(color, 100)));
        this.count = 1;
        this.offset = 0;
    }

    public MultiColorParticle(Set<PotentialObject<Color>> colors) {
        this.colors = colors;
        this.count = 1;
        this.offset = 0;
    }

    public MultiColorParticle(Color color, int count, double offset) {
        this.colors = new HashSet<>(Arrays.asList(new PotentialObject<>(color, 100)));
        this.count = count;
        this.offset = offset;
    }

    public Color pop() {
        return CTFUtil.getRandom(colors);
    }

}
