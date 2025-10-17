package com.yalcinkaya.ctf.kits;

import com.yalcinkaya.ctf.CTF;
import com.yalcinkaya.ctf.kit.ClickItem;
import com.yalcinkaya.ctf.kit.ClickKit;
import com.yalcinkaya.ctf.kit.Kit;
import com.yalcinkaya.ctf.kits.clickItems.Rewind;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class AgentZero extends Kit implements ClickKit {

    private final Rewind rewind = new Rewind();

    @Override
    public ClickItem[] getClickItems() {
        return new ClickItem[]{rewind};
    }

    @Override
    public void onMove(PlayerMoveEvent event) {
        new BukkitRunnable() {

            @Override
            public void run() {
                if (rewind.isCompleted()) {
                    rewind.setSomeTimeAgo(event.getTo());
                }
            }

        }.runTaskLater(CTF.getInstance(), rewind.getRewindSpan() * 20L);
    }

    @Override
    public void onDeath(PlayerDeathEvent event) {
        rewind.setSomeTimeAgo(null);
    }

    @Override
    public void onLeave(PlayerQuitEvent event) {
        rewind.setSomeTimeAgo(null);
    }

    @Override
    public String getName() {
        return "Agent-0";
    }

}
