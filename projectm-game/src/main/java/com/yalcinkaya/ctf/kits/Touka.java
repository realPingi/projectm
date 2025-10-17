package com.yalcinkaya.ctf.kits;

import com.yalcinkaya.ctf.kit.ClickItem;
import com.yalcinkaya.ctf.kit.ClickKit;
import com.yalcinkaya.ctf.kit.Kit;
import com.yalcinkaya.ctf.kits.clickItems.Ascent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class Touka extends Kit implements ClickKit {

    private final Ascent blaze = new Ascent();

    @Override
    public void onDeath(PlayerDeathEvent event) {
        if (blaze.isActive()) {
            blaze.cancel();
        }
    }

    @Override
    public void onLeave(PlayerQuitEvent event) {
        if (blaze.isActive()) {
            blaze.cancel();
        }
    }

    @Override
    public ClickItem[] getClickItems() {
        return new ClickItem[]{blaze};
    }

    @Override
    public String getName() {
        return "Touka";
    }

}
