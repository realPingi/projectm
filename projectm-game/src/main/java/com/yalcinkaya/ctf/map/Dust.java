package com.yalcinkaya.ctf.map;

import com.yalcinkaya.ctf.flag.Flag;
import com.yalcinkaya.ctf.flag.FlagLocation;
import com.yalcinkaya.ctf.team.TeamColor;
import org.bukkit.Location;

import java.util.Arrays;
import java.util.List;

public class Dust implements Map {

    private final List<Flag> flags = Arrays.asList(
            new Flag(FlagLocation.FRONT, new Location(getWorld(), 91.5, 45, 0.5, 0, 0), TeamColor.BLUE),
            new Flag(FlagLocation.LEFT, new Location(getWorld(), 140.5, 46, -100.5, 0, 0), TeamColor.BLUE),
            new Flag(FlagLocation.RIGHT, new Location(getWorld(), 140.5, 46, 101.5, 0, 0), TeamColor.BLUE),
            new Flag(FlagLocation.FRONT, new Location(getWorld(), -90.5, 45, 0.5, 0, 0), TeamColor.RED),
            new Flag(FlagLocation.LEFT, new Location(getWorld(), -139.5, 46, 101.5, 0, 0), TeamColor.RED),
            new Flag(FlagLocation.RIGHT, new Location(getWorld(), -139.5, 46, -100.5, 0, 0), TeamColor.RED)
    );

    @Override
    public String getName() {
        return "Dust";
    }

    @Override
    public List<Flag> getFlags() {
        return flags;
    }

    @Override
    public Location getBlueSpawn() {
        return new Location(getWorld(), 139, 39, 1, 90, 0);
    }

    @Override
    public Location getRedSpawn() {
        return new Location(getWorld(), -138, 39, 1, -90, 0);
    }
}
