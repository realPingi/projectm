package com.yalcinkaya.lobby.commands;

import com.yalcinkaya.lobby.Lobby;
import com.yalcinkaya.lobby.party.Party;
import com.yalcinkaya.lobby.party.PartyManager;
import com.yalcinkaya.lobby.user.LobbyUser;
import com.yalcinkaya.lobby.util.LobbyUtil;
import com.yalcinkaya.lobby.util.MessageType;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
            user.sendMessage(LobbyUtil.getLobbyMessage(MessageType.WARNING, ChatColor.GRAY + "Error loading your user profile."));
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
        user.sendMessage(LobbyUtil.getLobbyMessage(MessageType.INFO, ChatColor.YELLOW + "/party create " + ChatColor.GRAY + "- Creates a new party."));
        user.sendMessage(LobbyUtil.getLobbyMessage(MessageType.INFO, ChatColor.YELLOW + "/party invite <player> " + ChatColor.GRAY + "- Invites a player."));
        user.sendMessage(LobbyUtil.getLobbyMessage(MessageType.INFO, ChatColor.YELLOW + "/party join <player> " + ChatColor.GRAY + "- Joins a party."));
        user.sendMessage(LobbyUtil.getLobbyMessage(MessageType.INFO, ChatColor.YELLOW + "/party leave " + ChatColor.GRAY + "- Leaves your current party."));
        user.sendMessage(LobbyUtil.getLobbyMessage(MessageType.INFO, ChatColor.YELLOW + "/party list " + ChatColor.GRAY + "- Lists all party members."));
    }

    // --- Sub-Befehle ---

    private void handleList(LobbyUser user) {
        PartyManager partyManager = Lobby.getInstance().getPartyManager();
        Party party = partyManager.getParty(user);

        if (party == null) {
            user.sendMessage(LobbyUtil.getLobbyMessage(MessageType.WARNING, ChatColor.GRAY + "You are not in a party."));
            return;
        }

        party.getMembers().forEach(uuid -> user.sendMessage(LobbyUtil.getLobbyMessage(MessageType.INFO, ChatColor.GRAY + LobbyUtil.getPlayer(uuid).getName())));
    }

    private void handleCreate(LobbyUser user) {
        PartyManager partyManager = Lobby.getInstance().getPartyManager();
        if (partyManager.hasParty(user)) {
            user.sendMessage(LobbyUtil.getLobbyMessage(MessageType.WARNING, ChatColor.GRAY + "You are already in a party."));
            return;
        }

        partyManager.createParty(user);
        user.sendMessage(LobbyUtil.getLobbyMessage(MessageType.INFO, ChatColor.GRAY + "You have created a new party."));
    }

    private void handleInvite(LobbyUser user, String[] args) {
        PartyManager partyManager = Lobby.getInstance().getPartyManager();
        String inviterName = LobbyUtil.getPlayer(user).getName();

        if (!partyManager.hasParty(user)) {
            user.sendMessage(LobbyUtil.getLobbyMessage(MessageType.WARNING, ChatColor.GRAY + "You must create a party first (/party create)."));
            return;
        }

        Party party = partyManager.getParty(user);
        if (party.size() >= 5) {
            user.sendMessage(LobbyUtil.getLobbyMessage(MessageType.WARNING, ChatColor.GRAY + "The party is full."));
            return;
        }

        if (args.length < 2) {
            user.sendMessage(LobbyUtil.getLobbyMessage(MessageType.WARNING, ChatColor.GRAY + "Usage: /party invite <playerName>"));
            return;
        }

        String targetName = args[1];
        Player targetPlayer = Bukkit.getPlayerExact(targetName);

        if (targetPlayer == null) {
            user.sendMessage(LobbyUtil.getLobbyMessage(MessageType.WARNING, ChatColor.GRAY + "Player ", targetName, ChatColor.GRAY + " is not online."));
            return;
        }

        LobbyUser targetUser = LobbyUtil.getUser(targetPlayer);

        if (partyManager.hasParty(targetUser)) {
            user.sendMessage(LobbyUtil.getLobbyMessage(MessageType.WARNING, ChatColor.GRAY + "This player is already in a party."));
            return;
        }

        if (user.getUuid().equals(targetUser.getUuid())) {
            user.sendMessage(LobbyUtil.getLobbyMessage(MessageType.WARNING, ChatColor.GRAY + "You cannot invite yourself."));
            return;
        }

        // Benachrichtigung bei bereits bestehender Einladung (optional, aber nützlich)
        if (partyManager.getInviteParty(targetUser.getUuid()) != null) {
            user.sendMessage(LobbyUtil.getLobbyMessage(MessageType.WARNING, ChatColor.GRAY + "This player already has a pending party invite."));
            // Du kannst hier returnen oder einfach eine neue Einladung senden
        }


        // Einladung speichern und Timeout starten
        partyManager.sendInvite(user, targetUser);

        String inviteMessage = "<light_purple><bold>Info >></bold> " + inviterName + "<gray> has invited you to join their party. ";
        String clickablePart = "<click:run_command:/party join " + inviterName + "><green>(accept)</click>";

        LobbyUtil.getPlayer(targetUser).sendMessage(MiniMessage.miniMessage().deserialize(inviteMessage + clickablePart));
        user.sendMessage(LobbyUtil.getLobbyMessage(MessageType.INFO, ChatColor.GRAY + "You have invited ", targetName, ChatColor.GRAY + " to your party."));

        // Benachrichtige Party-Mitglieder über die Einladung
        String partyNotify = LobbyUtil.getLobbyMessage(MessageType.INFO, ChatColor.GRAY + "You have invited ", targetName, ChatColor.GRAY + " to the party.");
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
            user.sendMessage(LobbyUtil.getLobbyMessage(MessageType.WARNING, ChatColor.GRAY + "You are already in a party."));
            return;
        }

        if (args.length < 2) {
            user.sendMessage(LobbyUtil.getLobbyMessage(MessageType.WARNING, ChatColor.GRAY + "Usage: /party join <inviterName>"));
            return;
        }

        String inviterName = args[1];
        Player inviterPlayer = Bukkit.getPlayerExact(inviterName);

        if (inviterPlayer == null) {
            user.sendMessage(LobbyUtil.getLobbyMessage(MessageType.WARNING, ChatColor.GRAY + "The inviter ", inviterName, ChatColor.GRAY + " is not online."));
            return;
        }

        LobbyUser inviterUser = LobbyUtil.getUser(inviterPlayer);

        // Die Party der Einladung abrufen
        Party party = partyManager.getInviteParty(user.getUuid());

        if (party == null) {
            user.sendMessage(LobbyUtil.getLobbyMessage(MessageType.WARNING, ChatColor.GRAY + "You do not have an open invitation from ", inviterName, ChatColor.GRAY + "."));
            return;
        }

        // Überprüfen, ob die Einladung von der richtigen Person kam
        if (!party.getMembers().contains(inviterUser.getUuid())) {
            user.sendMessage(LobbyUtil.getLobbyMessage(MessageType.WARNING, ChatColor.GRAY + "The invitation from ", inviterName, ChatColor.GRAY + " is no longer valid (the inviter is no longer in the party)."));
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
            user.sendMessage(LobbyUtil.getLobbyMessage(MessageType.WARNING, ChatColor.GRAY + "You are not in a party."));
            return;
        }

        // Spieler entfernen
        party.removeMember(user);
        user.sendMessage(LobbyUtil.getLobbyMessage(MessageType.INFO, ChatColor.GRAY + "You have left the party."));

        // Nachricht an verbleibende Mitglieder
        String leaveMessage = LobbyUtil.getLobbyMessage(MessageType.INFO, playerName, ChatColor.GRAY + " has left the party.");

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
