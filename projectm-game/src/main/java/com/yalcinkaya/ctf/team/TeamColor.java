package com.yalcinkaya.ctf.team;

import org.bukkit.Color;

public enum TeamColor {

    BLUE(Color.BLUE),
    RED(Color.RED);

    Color color;

    TeamColor(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }
}
