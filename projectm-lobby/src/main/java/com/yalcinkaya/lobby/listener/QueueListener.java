package com.yalcinkaya.lobby.listener;

import com.yalcinkaya.lobby.Lobby;
import com.yalcinkaya.lobby.queue.Queue;
import com.yalcinkaya.lobby.queue.QueueManager;
import com.yalcinkaya.lobby.util.LobbyUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class QueueListener implements Listener {

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        QueueManager queueManager = Lobby.getInstance().getQueueManager();
        if (Lobby.getInstance().getQueueManager().isEntryDummy(entity)) {
            Queue queue = queueManager.getFromDummy((Villager) entity);
            queue.accept(LobbyUtil.getUser(player), player.isSneaking());
            event.setCancelled(true);
        }
    }

}
