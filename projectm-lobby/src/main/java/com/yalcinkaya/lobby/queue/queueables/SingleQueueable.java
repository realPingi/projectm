package com.yalcinkaya.lobby.queue.queueables;

import com.yalcinkaya.lobby.queue.Queueable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public interface SingleQueueable extends Queueable {

    default Set<UUID> getUUIDs() {
        return new HashSet<>(Collections.singletonList(getUUID()));
    }

    /**
     * Gets the {@link UUID} defining this {@link Queueable}
     *
     * @return {@link UUID}
     */
    UUID getUUID();

}
