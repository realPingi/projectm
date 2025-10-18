package com.yalcinkaya.lobby.queue;

import com.yalcinkaya.lobby.Lobby;
import com.yalcinkaya.lobby.menu.Menu;
import com.yalcinkaya.lobby.net.MatchLookupService;
import com.yalcinkaya.lobby.user.LobbyUser;
import com.yalcinkaya.lobby.util.LobbyUtil;
import com.yalcinkaya.lobby.util.MessageType;
import com.yalcinkaya.lobby.util.QueueUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public abstract class Queue<T extends Queueable> implements Runnable {

    /**
     * A set containing the queued {@link Queueable}s.
     */
    protected final Set<T> queue = new HashSet<>();
    /**
     * The associated {@link UUID} of this queue.
     */
    @Getter
    private final UUID identifier = UUID.randomUUID();

    /**
     * Schedules {@link #run()}.
     */
    public void init() {
        Bukkit.getScheduler().runTaskTimer(Lobby.getInstance(), this, 0, 1);
    }

    /**
     * Handles the output of {@link Queue#findMatch()}.
     */
    @Override
    public void run() {
        Set<T> matches = findMatch();
        if (matches != null) {
            onMatchFound(matches);
            matches.forEach(match -> Lobby.getInstance().getQueueManager().unqueue(match));
        } else {
            onMatchNotFound();
        }
    }

    /**
     * Adds a {@link Queueable} to the queue.
     *
     * @param queueable The {@link Queueable} to be queued.
     * @return true if <code>queueable</code> is queued.
     */
    public boolean queue(T queueable) {
        if (queueable == null) {
            return false;
        }
        return !isQueued(queueable) && queue.add(queueable);
    }

    /**
     * @param queueable The {@link Queueable} to be unqueued.
     * @return true if <code>queueable</code> is unqueued.
     */
    public boolean unqueue(T queueable) {
        if (queueable == null) return false;
        return queue.removeIf(q -> q == queueable || q.isSimilar(queueable));
    }

    public boolean isQueued(Queueable queueable) {
        return queueable != null && find(queueable) != null;
    }

    /**
     * Searches for matches inside the queue.
     *
     * @return a set of matching {@link Queueable}s, or null if there is no match.
     */
    public abstract Set<T> findMatch();

    /**
     * Handles matches found by {@link Queue#findMatch()}.
     *
     * @param matches The {@link Queueable}s that were previously matched.
     */
    public void onMatchFound(Set<T> matches) {
        matches.forEach(match -> match.getUUIDs().forEach(uuid -> LobbyUtil.getUser(uuid).sendMessage(LobbyUtil.getLobbyMessage(MessageType.INFO, ChatColor.GRAY + "Match found: ", getName()))));
    }

    /**
     * Called when {@link Queue#findMatch()} returns null.
     */
    public void onMatchNotFound() {
    }

    /**
     * @param queueable {@link Queueable}
     * @return {@link Queueable}.
     * @return the {@link Queueable} similiar to <code>queueable</code>.
     */
    public T find(Queueable queueable) {
        return queue.stream().filter(q -> q.isSimilar(queueable)).findFirst().orElse(null);
    }

    /**
     * @return the amount of {@link Queueable}s inside the queue.
     */
    public int getQueued() {
        return queue.size();
    }

    /**
     * @return the amount of players inside the queue.
     */
    public int getQueuedPlayers() {
        int queuedPlayers = 0;
        for (T q : queue) {
            queuedPlayers += q.getUUIDs().size();
        }
        return queuedPlayers;
    }

    /**
     * @return the amount of players that have been matched by this queue.
     */
    public int getIngame() {
        return 0;
    }

    /**
     * @return the display material of this queue.
     */
    public abstract Material getDisplayMaterial();

    /**
     * @return an {@link ItemStack} containing information about this queue.
     * @seetag {@link QueueUtil#buildIcon(Queue queue)}
     */
    public ItemStack getIcon() {
        return QueueUtil.buildIcon(this);
    }

    /**
     * @return the name of this queue.
     */
    public abstract String getName();

    /**
     * Toggles the queue state of a {@link Queueable} depending on a {@link ClickType} input.
     *
     * @param sender    the {@link LobbyUser} sending the request.
     * @param clickType the {@link ClickType} involved in the request.
     */
    public void accept(LobbyUser sender, ClickType clickType) {
        Player player = LobbyUtil.getPlayer(sender);
        Menu menu = sender.getMenu();
        if (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT) {
            if (isQueued(sender)) {
                T queueable = find(sender);
                unqueue(queueable);
                queueable.getUUIDs().forEach(uuid -> LobbyUtil.getUser(uuid).sendMessage(LobbyUtil.getLobbyMessage(MessageType.INFO, ChatColor.GRAY + "You left ", getName(), ChatColor.GRAY + ".")));
                if (menu != null) {
                    menu.closeAndOpen(sender);
                }
            } else {
                sender.sendMessage(LobbyUtil.getLobbyMessage(MessageType.WARNING, ChatColor.GRAY + "You are not queued for ", getName(), ChatColor.GRAY + "."));
            }
        } else {
            if (!isQueued(sender)) {
                T queueable = queuebalize(sender);
                if (queueable == null) {
                    return;
                }
                MatchLookupService lookup = new MatchLookupService();
                if (queueable.getUUIDs().stream().anyMatch(lookup::isPlayerInAnyMatch)) {
                    player.closeInventory();
                    sender.sendMessage(LobbyUtil.getLobbyMessage(MessageType.WARNING, ChatColor.GRAY + "Your match is still running. Use ", "/reconnect", ChatColor.GRAY + "."));
                    return;
                }
                if (queue(queueable)) {
                    queueable.getUUIDs().forEach(uuid -> LobbyUtil.getUser(uuid).sendMessage(LobbyUtil.getLobbyMessage(MessageType.INFO, ChatColor.GRAY + "You joined ", getName(), ChatColor.GRAY + ".")));
                    if (sender.getMenu() != null) {
                        menu.closeAndOpen(sender);
                    }
                }
            } else {
                sender.sendMessage(LobbyUtil.getLobbyMessage(MessageType.WARNING, ChatColor.GRAY + "You are already queued."));
            }
        }
    }

    /**
     * Transforms a user to a {@link Queueable} that fits this queue.
     *
     * @param user {@link LobbyUser}
     * @return a {@link Queueable} of type {@link T}
     */
    public abstract T queuebalize(LobbyUser user);

}
