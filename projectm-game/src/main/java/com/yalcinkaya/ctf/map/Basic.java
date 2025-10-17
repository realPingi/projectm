package com.yalcinkaya.ctf.map;

import com.yalcinkaya.ctf.flag.Flag;
import com.yalcinkaya.ctf.flag.FlagLocation;
import com.yalcinkaya.ctf.team.TeamColor;
import org.bukkit.Location;

import java.util.Arrays;
import java.util.List;

public class Basic implements Map {

    private final List<Flag> flags = Arrays.asList(
            new Flag(FlagLocation.LEFT, new Location(getWorld(), -179.5, 35, 60.5, 0, 0), TeamColor.BLUE),
            new Flag(FlagLocation.RIGHT, new Location(getWorld(), -179.5, 35, -59.5, 0, 0), TeamColor.BLUE),
            new Flag(FlagLocation.LEFT, new Location(getWorld(), 180.5, 35, -59.5, 0, 0), TeamColor.RED),
            new Flag(FlagLocation.RIGHT, new Location(getWorld(), 180.5, 35, 60.5, 0, 0), TeamColor.RED));

    @Override
    public String getName() {
        return "Basic";
    }

    @Override
    public List<Flag> getFlags() {
        return flags;
    }

    @Override
    public Location getBlueSpawn() {
        return new Location(getWorld(), -117.5, 29, 0.5, -90, 0);
    }

    @Override
    public Location getRedSpawn() {
        return new Location(getWorld(), 117.5, 29, 0.5, 90, 0);
    }
}
