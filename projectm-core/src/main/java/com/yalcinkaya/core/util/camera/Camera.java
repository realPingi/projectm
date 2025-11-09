package com.yalcinkaya.core.util.camera;

import com.yalcinkaya.core.ProjectM;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public abstract class Camera {

    private final Set<UUID> viewers = new HashSet<>();

    public void record() {
        Bukkit.getScheduler().runTaskTimer(ProjectM.getInstance(), () -> {
            viewers.forEach(viewer -> {
                Player player = Bukkit.getPlayer(viewer);
                if (player != null && player.isOnline() && getObservedLocation() != null) {
                    Location viewLocation = getObservedLocation().clone().add(0, 5, 0);
                    Vector direction = getObservedLocation().toVector().subtract(viewLocation.toVector());
                    viewLocation.setDirection(direction);
                    player.teleport(viewLocation);
                }
            });
        }, 0, 1);
    }

    public void attach(Player player) {
        viewers.add(player.getUniqueId());
    }

    public void detach(Player player) {
        viewers.remove(player.getUniqueId());
    }

    public boolean isAttached(Player player) {
        return viewers.contains(player.getUniqueId());
    }

    public abstract Location getObservedLocation();
}
