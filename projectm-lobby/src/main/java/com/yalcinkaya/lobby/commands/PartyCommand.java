package com.yalcinkaya.lobby.commands;

import com.yalcinkaya.core.util.CoreUtil;
import com.yalcinkaya.core.util.MessageType;
import com.yalcinkaya.lobby.Lobby;
import com.yalcinkaya.lobby.party.Party;
import com.yalcinkaya.lobby.party.PartyManager;
import com.yalcinkaya.lobby.user.LobbyUser;
import com.yalcinkaya.lobby.util.LobbyUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PartyCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        LobbyUser user = LobbyUtil.getUser(player);
        if (user == null) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Error loading your user profile."));
            return true;
        }

        if (args.length == 0) {
            sendHelp(user);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create" -> handleCreate(user);
            case "invite" -> handleInvite(user, args);
            case "join" -> handleJoin(user, args);
            case "leave" -> handleLeave(user);
            case "list" -> handleList(user);
            default -> sendHelp(user);
        }

        return true;
    }

    private void sendHelp(LobbyUser user) {
        user.sendMessage(CoreUtil.getMessage(MessageType.INFO, "<yellow>/party create ", "- Creates a new party."));
        user.sendMessage(CoreUtil.getMessage(MessageType.INFO, "<yellow>/party invite <player> ", "- Invites a player."));
        user.sendMessage(CoreUtil.getMessage(MessageType.INFO, "<yellow>/party join <player> ", "- Joins a party."));
        user.sendMessage(CoreUtil.getMessage(MessageType.INFO, "<yellow>/party leave ", "- Leaves your current party."));
        user.sendMessage(CoreUtil.getMessage(MessageType.INFO, "<yellow>/party list ", "- Lists all party members."));
    }

    // --- Sub-Befehle ---

    private void handleList(LobbyUser user) {
        PartyManager partyManager = Lobby.getInstance().getPartyManager();
        Party party = partyManager.getParty(user);

        if (party == null) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "You are not in a party."));
            return;
        }

        party.getMembers().forEach(uuid -> user.sendMessage(CoreUtil.getMessage(MessageType.INFO, LobbyUtil.getPlayer(uuid).getName())));
    }

    private void handleCreate(LobbyUser user) {
        PartyManager partyManager = Lobby.getInstance().getPartyManager();
        if (partyManager.hasParty(user)) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "You are already in a party."));
            return;
        }

        partyManager.createParty(user);
        user.sendMessage(CoreUtil.getMessage(MessageType.INFO, "You have created a new party."));
    }

    private void handleInvite(LobbyUser user, String[] args) {
        PartyManager partyManager = Lobby.getInstance().getPartyManager();
        String inviterName = LobbyUtil.getPlayer(user).getName();

        if (!partyManager.hasParty(user)) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "You must create a party first (/party create)."));
            return;
        }

        Party party = partyManager.getParty(user);
        if (party.size() >= 5) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "The party is full."));
            return;
        }

        if (args.length < 2) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Usage: /party invite <playerName>"));
            return;
        }

        String targetName = args[1];
        Player targetPlayer = Bukkit.getPlayerExact(targetName);

        if (targetPlayer == null) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Player ", targetName, " is not online."));
            return;
        }

        LobbyUser targetUser = LobbyUtil.getUser(targetPlayer);

        if (partyManager.hasParty(targetUser)) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "This player is already in a party."));
            return;
        }

        if (user.getUuid().equals(targetUser.getUuid())) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "You cannot invite yourself."));
            return;
        }

        // Benachrichtigung bei bereits bestehender Einladung (optional, aber nützlich)
        if (partyManager.getInviteParty(targetUser.getUuid()) != null) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "This player already has a pending party invite."));
            // Du kannst hier returnen oder einfach eine neue Einladung senden
        }


        // Einladung speichern und Timeout starten
        partyManager.sendInvite(user, targetUser);
        String clickablePart = "<click:run_command:/party join " + inviterName + "><green>(accept)</click>";

        user.sendMessage(CoreUtil.getMessage(MessageType.INFO, "", inviterName, " has invited you to join their party. " + clickablePart));
        user.sendMessage(CoreUtil.getMessage(MessageType.INFO, "You have invited ", targetName, " to your party."));

        // Benachrichtige Party-Mitglieder über die Einladung
        String partyNotify = CoreUtil.getMessage(MessageType.INFO, "You have invited ", targetName, " to the party.");
        party.getUUIDs().stream()
                .filter(uuid -> !uuid.equals(user.getUuid()))
                .map(LobbyUtil::getUser)
                .forEach(member -> {
                    if(member != null) member.sendMessage(partyNotify);
                });
    }

    private void handleJoin(LobbyUser user, String[] args) {
        PartyManager partyManager = Lobby.getInstance().getPartyManager();
        if (partyManager.hasParty(user)) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "You are already in a party."));
            return;
        }

        if (args.length < 2) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Usage: /party join <inviterName>"));
            return;
        }

        String inviterName = args[1];
        Player inviterPlayer = Bukkit.getPlayerExact(inviterName);

        if (inviterPlayer == null) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "The inviter ", inviterName, " is not online."));
            return;
        }

        LobbyUser inviterUser = LobbyUtil.getUser(inviterPlayer);

        // Die Party der Einladung abrufen
        Party party = partyManager.getInviteParty(user.getUuid());

        if (party == null) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "You do not have an open invitation from ", inviterName, "."));
            return;
        }

        // Überprüfen, ob die Einladung von der richtigen Person kam
        if (!party.getMembers().contains(inviterUser.getUuid())) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "The invitation from ", inviterName, " is no longer valid (the inviter is no longer in the party)."));
            partyManager.removeInvite(user.getUuid());
            return;
        }

        // Party beitreten (PartyManager übernimmt die Erfolgsnachrichten an alle)
        partyManager.joinParty(user, party);
    }

    private void handleLeave(LobbyUser user) {
        PartyManager partyManager = Lobby.getInstance().getPartyManager();
        Party party = partyManager.getParty(user);
        String playerName = LobbyUtil.getPlayer(user).getName();

        if (party == null) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "You are not in a party."));
            return;
        }

        // Spieler entfernen
        party.removeMember(user);
        user.sendMessage(CoreUtil.getMessage(MessageType.INFO, "You have left the party."));

        // Nachricht an verbleibende Mitglieder
        String leaveMessage = CoreUtil.getMessage(MessageType.INFO, "", playerName, " has left the party.");

        Set<UUID> remainingMembers = new HashSet<>(party.getUUIDs());

        remainingMembers.forEach(memberUuid -> {
            LobbyUser memberUser = LobbyUtil.getUser(memberUuid);
            if (memberUser != null) {
                memberUser.sendMessage(leaveMessage);
            }
        });

        // Party auflösen, wenn sie leer ist
        if (party.size() == 0) {
            partyManager.removeParty(party.getId());
        }
    }
}
