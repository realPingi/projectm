package com.yalcinkaya.lobby.queue;

import java.util.Set;
import java.util.UUID;

public interface Queueable {

    /**
     * Gets the {@link UUID} defining this {@link Queueable}
     *
     * @return {@link UUID}
     */
    UUID getUUID();

    /**
     * @return a set of {@link UUID}s.
     */
    Set<UUID> getUUIDs();

    /**
     * @param queueable the {@link Queueable} to compare to
     * @return true if <code>queueable</code> and <code>this</code> have a {@link UUID} in common.
     */
    default boolean isSimilar(Queueable queueable) {
        return getUUIDs().stream().anyMatch(uuid -> queueable.getUUIDs().contains(uuid));
    }

}