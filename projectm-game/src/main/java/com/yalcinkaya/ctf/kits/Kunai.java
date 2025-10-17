package com.yalcinkaya.ctf.kits;

import com.yalcinkaya.ctf.kit.ClickItem;
import com.yalcinkaya.ctf.kit.ClickKit;
import com.yalcinkaya.ctf.kit.Kit;
import com.yalcinkaya.ctf.kits.clickItems.Knife;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class Kunai extends Kit implements ClickKit {

    private final Knife knife = new Knife();

    @Override
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        Arrow arrow = knife.getArrow();
        if (event.isSneaking() && arrow != null && !arrow.isOnGround() && !arrow.isDead()) {
            Player player = event.getPlayer();
            float yaw = player.getLocation().getYaw();
            float pitch = player.getLocation().getPitch();
            Location location = arrow.getLocation().clone();
            location.setYaw(yaw);
            location.setPitch(pitch);
            event.getPlayer().teleport(location);
            arrow.remove();
        }
    }

    @Override
    public ClickItem[] getClickItems() {
        return new ClickItem[]{knife};
    }

    @Override
    public String getName() {
        return "Kunai";
    }

}
