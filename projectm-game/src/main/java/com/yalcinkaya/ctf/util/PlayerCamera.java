package com.yalcinkaya.ctf.util;

import com.yalcinkaya.core.util.CoreUtil;
import com.yalcinkaya.core.util.MessageType;
import com.yalcinkaya.core.util.SmoothTravel;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.UUID;

@Getter
public class PlayerCamera {

    private final UUID observedId;

    private final HashMap<UUID, SmoothTravel> viewers = new HashMap<>();

    public PlayerCamera(UUID observedId) {
        this.observedId = observedId;
    }

    public void attach(Player player) {

        Player observed = Bukkit.getPlayer(observedId);

        if (observed == null || !observed.isOnline()) {
            return;
        }

        if (isAttached(player)) {
            CTFUtil.getUser(player).sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Already attached."));
            return;
        }

        SmoothTravel travel = new SmoothTravel(player, observed, new Vector(0, 3, 0));
        travel.start();
        viewers.put(player.getUniqueId(), travel);
    }

    public void detach(Player player) {

        if (!isAttached(player)) {
            CTFUtil.getUser(player).sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Not attached."));
            return;
        }

        viewers.get(player.getUniqueId()).cancel();
        viewers.remove(player.getUniqueId());
    }

    public boolean isAttached(Player player) {
        return viewers.containsKey(player.getUniqueId());
    }

}
