package com.yalcinkaya.lobby.queue;

import java.util.Set;
import java.util.UUID;

public interface Queueable {

    /**
     * @return a set of {@link UUID}s.
     */
    Set<UUID> getUUIDs();

    /**
     * @param other the {@link Queueable} to compare to
     * @return true if <code>queueable</code> and <code>this</code> have a {@link UUID} in common.
     */
    default boolean isSimilar(Queueable other) {
        if (other == this) return true;
        if (other == null) return false;
        var a = getUUIDs();
        var b = other.getUUIDs();
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) return false;
        return a.stream().anyMatch(b::contains);
    }

}