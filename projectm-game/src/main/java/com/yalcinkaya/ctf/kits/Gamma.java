package com.yalcinkaya.ctf.kits;

import com.yalcinkaya.ctf.kit.ClickItem;
import com.yalcinkaya.ctf.kit.ClickKit;
import com.yalcinkaya.ctf.kit.Kit;
import com.yalcinkaya.ctf.kits.clickItems.Nuke;
import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class Gamma extends Kit implements ClickKit {

    private final Nuke nuke = new Nuke();

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {

        CTFUser user = CTFUtil.getUser(event.getPlayer().getUniqueId());
        if (user.getKit() instanceof Gamma gamma) {
            if (gamma.getNuke().getStage() == 1) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {

        CTFUser user = CTFUtil.getUser(event.getPlayer().getUniqueId());
        if (user.getKit() instanceof Gamma gamma) {
            if (gamma.getNuke().getStage() == 1) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {

        if (event.getEntity() instanceof Player player) {
            CTFUser user = CTFUtil.getUser(player.getUniqueId());
            if (user.getKit() instanceof Gamma) {
                Gamma gamma = (Gamma) user.getKit();
                if (gamma.getNuke().getStage() == 1) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDamageByPlayer(EntityDamageByEntityEvent event) {

        if (event.getDamager() instanceof Player player) {
            CTFUser user = CTFUtil.getUser(player.getUniqueId());
            if (user.getKit() instanceof Gamma gamma) {
                if (gamma.getNuke().getStage() == 1) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @Override
    public ClickItem[] getClickItems() {
        return new ClickItem[]{nuke};
    }

    @Override
    public String getName() {
        return "Gamma";
    }

    public Nuke getNuke() {
        return nuke;
    }

}
