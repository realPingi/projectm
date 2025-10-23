package com.yalcinkaya.ctf.listener;

import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public abstract class StageListener implements Listener {

    public abstract void setupPlayer(Player player);

    public abstract Component getJoinMessage(CTFUser user);

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        CTFUser user = CTFUtil.getUser(player);
        if (!user.isSpectating()) {
            setupPlayer(player);
            event.joinMessage(getJoinMessage(user));
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        CTFUser user = CTFUtil.getUser(player);
        if (!user.isSpectating()) {
            setupPlayer(player);
        }
    }
}
