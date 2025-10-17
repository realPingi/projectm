package com.yalcinkaya.ctf.kits;

import com.yalcinkaya.ctf.kit.*;
import com.yalcinkaya.ctf.kits.clickItems.Scissors;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class Gwen extends Kit implements ClickKit, MultiCounter {

    private final Scissors scissors = new Scissors();

    @Override
    public void onDamagePlayer(EntityDamageByEntityEvent event) {
        if (!event.isCancelled()) {
            scissors.getStacks().increase();
        }
    }

    @Override
    public void onDeath(PlayerDeathEvent event) {
        scissors.getStacks().reset();
    }

    @Override
    public void onLeave(PlayerQuitEvent event) {
        scissors.getStacks().reset();
    }

    @Override
    public ClickItem[] getClickItems() {
        return new ClickItem[]{scissors};
    }

    @Override
    public String getName() {
        return "Gwen";
    }

    @Override
    public Counter[] getCounters() {
        return new Counter[]{scissors.getStacks()};
    }
}
