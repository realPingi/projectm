package com.yalcinkaya.lobby.party;

import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public class PartyInvite {
    private final UUID partyId;
    private final UUID inviterUuid;
    private final BukkitTask timeoutTask;

    public PartyInvite(UUID partyId, UUID inviterUuid, BukkitTask timeoutTask) {
        this.partyId = partyId;
        this.inviterUuid = inviterUuid;
        this.timeoutTask = timeoutTask;
    }

    public UUID getPartyId() {
        return partyId;
    }

    public UUID getInviterUuid() {
        return inviterUuid;
    }

    public void cancelTimeout() {
        if (timeoutTask != null) {
            timeoutTask.cancel();
        }
    }
}
