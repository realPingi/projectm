package com.yalcinkaya.lobby.party;

import com.yalcinkaya.core.util.CoreUtil;
import com.yalcinkaya.core.util.MessageType;
import com.yalcinkaya.lobby.Lobby;
import com.yalcinkaya.lobby.user.LobbyUser;
import com.yalcinkaya.lobby.util.LobbyUtil;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.UUID;

public class PartyManager {

    private static final long INVITE_TIMEOUT_SECONDS = 60; // 60 Sekunden Timeout
    private final HashMap<UUID, Party> parties = new HashMap<>();
    // Map: Invited Player UUID -> PartyInvite
    private final HashMap<UUID, PartyInvite> pendingInvites = new HashMap<>();

    public void createParty(LobbyUser user) {
        Party party = new Party(user.getUuid());
        parties.put(party.getId(), party);
    }

    public void removeParty(UUID partyId) {
        parties.remove(partyId);
    }

    public boolean hasParty(LobbyUser user) {
        return getParty(user) != null;
    }

    public Party getParty(LobbyUser user) {
        return parties.values().stream().filter(party -> party.getMembers().contains(user.getUuid())).findFirst().orElse(null);
    }

    // --- Einladungslogik ---

    public void sendInvite(LobbyUser inviter, LobbyUser invited) {
        Party party = getParty(inviter);
        if (party == null) return; // Sollte nicht passieren, aber zur Sicherheit

        // Bestehende Einladung entfernen, falls vorhanden
        removeInvite(invited.getUuid());

        // Timeout-Task erstellen
        BukkitTask task = Bukkit.getScheduler().runTaskLater(Lobby.getInstance(), () -> {
            removeInvite(invited.getUuid());
            invited.sendMessage(CoreUtil.getMessage(MessageType.INFO, "The invitation from ", LobbyUtil.getPlayer(inviter).getName(), " expired."));
        }, INVITE_TIMEOUT_SECONDS * 20L); // 20 Ticks pro Sekunde

        // Einladung speichern
        PartyInvite invite = new PartyInvite(party.getId(), inviter.getUuid(), task);
        pendingInvites.put(invited.getUuid(), invite);
    }

    public Party getInviteParty(UUID invitedUuid) {
        PartyInvite invite = pendingInvites.get(invitedUuid);
        if (invite == null) return null;
        return parties.get(invite.getPartyId());
    }

    public void removeInvite(UUID invitedUuid) {
        PartyInvite invite = pendingInvites.remove(invitedUuid);
        if (invite != null) {
            invite.cancelTimeout();
        }
    }

    // Party joinen
    public boolean joinParty(LobbyUser user, Party party) {
        if (getParty(user) != null) return false;

        party.addMember(user);
        removeInvite(user.getUuid());

        // Benachrichtige alle (inklusive dem neuen Mitglied)
        party.getUUIDs().forEach(memberUuid -> {
            LobbyUser memberUser = LobbyUtil.getUser(memberUuid);
            if (memberUser != null) {
                memberUser.sendMessage(CoreUtil.getMessage(MessageType.INFO, "", LobbyUtil.getPlayer(user).getName(), " has joined the party."));
            }
        });

        return true;
    }
}
