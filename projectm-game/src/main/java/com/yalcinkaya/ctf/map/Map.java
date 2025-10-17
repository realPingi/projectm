package com.yalcinkaya.ctf.map;

import com.yalcinkaya.ctf.CTF;
import com.yalcinkaya.ctf.flag.Flag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;

public interface Map {

    String getName();

    List<Flag> getFlags();

    Location getBlueSpawn();

    Location getRedSpawn();

    default World getWorld() {
        return Bukkit.getWorld(CTF.ctfWorld);
    }

}
