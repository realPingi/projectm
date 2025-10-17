package com.yalcinkaya.lobby.queue;

import com.yalcinkaya.lobby.queue.queues.SoloQueue5v5;
import com.yalcinkaya.lobby.user.LobbyUser;
import lombok.Getter;

import java.util.HashMap;
import java.util.UUID;

@Getter
public class QueueManager {

    private final HashMap<UUID, Queue> queues = new HashMap<>();
    private final SoloQueue5v5 soloQueue5v5 = new SoloQueue5v5();

    public void loadQueues() {
        openQueue(soloQueue5v5);
    }

    public void openQueue(Queue queue) {
        registerQueue(queue);
        queue.init();
    }

    public void registerQueue(Queue queue) {
        queues.put(queue.getIdentifier(), queue);
    }

    public boolean isLock(Queueable queueable) {
        return queues.values().stream().anyMatch(queue -> queue.isQueued(queueable));
    }

    public void unqueue(LobbyUser user) {
        queues.values().forEach(queue -> queue.unqueue(user));
    }

    public void unqueue(Queueable queueable) {
        queues.values().forEach(queue -> queue.unqueue(queueable));
    }
}
