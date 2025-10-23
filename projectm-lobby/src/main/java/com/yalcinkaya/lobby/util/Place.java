package com.yalcinkaya.lobby.util;

import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

@AllArgsConstructor
public enum Place {

    SPAWN(-1.5,88,-0.5, -90, 0),
    QUEUES(49, 84, -0.5, -90, 0),
    LEADERBOARDS(90.5, 84, -0.5, 90,0),
    QUEUE_SOLO_UNRANKED(56.5, 85, -4.5, 90, 0),
    QUEUE_PARTY_UNRANKED(56.5, 85, 3.5, 90, 0),
    LEADERBOARD_SOLO(84.5, 88, 4.5, -90, 0),
    LEADERBOARD_PARTY(84.5, 88, -5.5, -90, 0);

    private final double x;
    private final double y;
    private final double z;
    private final long yaw;
    private final long pitch;

    public Location getLocation() {
        World world = Bukkit.getWorld("world");
        return new Location(world, x, y, z, yaw, pitch);
    }
}
