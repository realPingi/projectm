package com.yalcinkaya.lobby.queue;

import com.yalcinkaya.lobby.queue.queues.PartyQueue;
import com.yalcinkaya.lobby.queue.queues.SoloQueue;
import com.yalcinkaya.lobby.user.LobbyUser;
import lombok.Getter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;

import java.util.HashMap;
import java.util.UUID;

@Getter
public class QueueManager {

    private final HashMap<UUID, Queue> queues = new HashMap<>();
    private final SoloQueue soloQueue = new SoloQueue();
    private final PartyQueue partyQueue = new PartyQueue();

    public void loadQueues() {
        registerQueue(soloQueue);
        registerQueue(partyQueue);
    }

    public void registerQueue(Queue queue) {
        queues.put(queue.getIdentifier(), queue);
        queue.init();
    }

    public void unqueue(LobbyUser user) {
        queues.values().forEach(queue -> queue.unqueue(user));
    }

    public void unqueue(Queueable queueable) {
        queues.values().forEach(queue -> queue.unqueue(queueable));
    }

    public boolean isEntryDummy(Entity entity) {
        if (entity instanceof Villager villager) {
            return queues.values().stream().anyMatch(queue -> villager.hasMetadata(queue.getName()));
        }

        return false;
    }

    public Queue getFromDummy(Villager villager) {
        return queues.values().stream().filter(queue -> villager.hasMetadata(queue.getName())).findFirst().orElse(null);
    }
}
