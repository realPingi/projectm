package com.yalcinkaya.ctf.kits;

import com.yalcinkaya.ctf.kit.*;
import com.yalcinkaya.ctf.kits.clickItems.Reflect;
import com.yalcinkaya.ctf.util.CTFUtil;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class DrYeet extends Kit implements ClickKit, MultiCounter {

    private final Reflect reflect = new Reflect();

    @Override
    public void onDamagedByPlayer(EntityDamageByEntityEvent event) {

        if (reflect.isRage() && event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            event.setCancelled(true);
            Player damager = (Player) event.getDamager();
            damager.setVelocity(damager.getEyeLocation().getDirection().normalize().multiply(-2).setY(0.5));
            CTFUtil.getUser((Player) event.getEntity()).playSoundForWorld(Sound.BLOCK_ANVIL_HIT);
            reflect.setRage(false);
        } else if (!event.isCancelled()) {
            reflect.getCharges().increase();
        }
    }

    @Override
    public ClickItem[] getClickItems() {
        return new ClickItem[]{reflect};
    }

    @Override
    public String getName() {
        return "Dr. Yeet";
    }

    @Override
    public Counter[] getCounters() {
        return new Counter[]{reflect.getCharges()};
    }
}
