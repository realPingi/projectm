package com.yalcinkaya.ctf.kits;

import com.yalcinkaya.ctf.kit.ClickItem;
import com.yalcinkaya.ctf.kit.ClickKit;
import com.yalcinkaya.ctf.kit.Kit;
import com.yalcinkaya.ctf.kits.clickItems.Ascent;
import com.yalcinkaya.ctf.util.CTFUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class Touka extends Kit implements ClickKit {

    private final Ascent blaze = new Ascent();

    @Override
    public void onMove(PlayerMoveEvent event) {
        if (blaze.isActive()) {
            CTFUtil.getNearbyOpponents(CTFUtil.getUser(event.getPlayer()), event.getPlayer().getLocation(), 3).forEach(enemy -> {
                CTFUtil.getPlayer(enemy).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 3 * 20, 3));
                CTFUtil.getPlayer(enemy).setVelocity(new Vector(0, 1, 0));
            });
        }
    }

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
