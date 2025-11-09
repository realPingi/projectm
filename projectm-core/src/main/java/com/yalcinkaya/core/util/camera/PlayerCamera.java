package com.yalcinkaya.core.util.camera;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

@Getter
public class PlayerCamera extends Camera {

    private final UUID observedId;

    public PlayerCamera(UUID observedId) {
        this.observedId = observedId;
    }

    @Override
    public Location getObservedLocation() {
        Player player = Bukkit.getPlayer(observedId);
        if (player == null || !player.isOnline()) {
            return null;
        }
        return player.getLocation();
    }
}
