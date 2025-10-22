package com.yalcinkaya.lobby.queue;

import com.yalcinkaya.core.util.CoreUtil;
import com.yalcinkaya.core.util.MessageType;
import com.yalcinkaya.lobby.Lobby;
import com.yalcinkaya.lobby.net.MatchLookupService;
import com.yalcinkaya.lobby.user.LobbyUser;
import com.yalcinkaya.lobby.util.LobbyUtil;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.HashSet;
import java.util.List;
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
     * The physical entry point of this queue.
     */
    @Getter
    private final Entry entry = new Entry();

    /**
     * Schedules {@link #run()}.
     */
    public void init() {
        entry.open();
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
        entry.update();
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
        matches.forEach(match -> match.getUUIDs().forEach(uuid -> LobbyUtil.getUser(uuid).sendMessage(CoreUtil.getMessage(MessageType.INFO, "Match found for ", getName(), "! Starting game server..."))));
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
     * @return the name of this queue.
     */
    public abstract String getName();

    /**
     * Toggles the queue state of a {@link Queueable} depending on a {@link ClickType} input.
     *
     * @param sender    the {@link LobbyUser} sending the request.
     * @param crouching the {@link Boolean} involved in the request.
     */
    public void accept(LobbyUser sender, boolean crouching) {
        if (crouching) {
            if (isQueued(sender)) {
                T queueable = find(sender);
                unqueue(queueable);
                queueable.getUUIDs().forEach(uuid -> LobbyUtil.getUser(uuid).sendMessage(CoreUtil.getMessage(MessageType.INFO, "You left ", getName(), ".")));
            } else {
                sender.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "You are not queued."));
            }
        } else {
            if (!isQueued(sender)) {
                T queueable = queuebalize(sender);
                if (queueable == null) {
                    return;
                }
                MatchLookupService lookup = new MatchLookupService();
                if (queueable.getUUIDs().stream().anyMatch(lookup::isPlayerWaitingForMatch)) {
                    sender.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "You have already been matched. Waiting for game server..."));
                    return;
                }
                if (queueable.getUUIDs().stream().anyMatch(lookup::isPlayerInActiveMatch)) {
                    sender.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Your match is still running. Use ", "/reconnect", "."));
                    return;
                }
                if (queue(queueable)) {
                    queueable.getUUIDs().forEach(uuid -> LobbyUtil.getUser(uuid).sendMessage(CoreUtil.getMessage(MessageType.INFO, "You joined ", getName(), ".")));
                }
            } else {
                sender.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "You are already queued."));
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


    public class Entry {
        private Hologram hologram;
        @Getter
        private Villager dummy;

        public void open() {
            String hologramName = getName().replace(" ", "_");

            hologram = DHAPI.createHologram(
                    hologramName,
                    getPhysicalEntry().clone().add(0, 3, 0),
                    List.of(
                            "&6" + "--- " + getName() + " ---",
                            "&7" + "Queuing >> " + "&6" + getQueued()
                    )
            );
            dummy = (Villager) getPhysicalEntry().getWorld().spawnEntity(getPhysicalEntry(), EntityType.VILLAGER);
            dummy.setAdult();
            dummy.setVillagerType(Villager.Type.SAVANNA);
            dummy.setProfession(Villager.Profession.CARTOGRAPHER);
            dummy.setAI(false);
            dummy.setInvulnerable(true);
            dummy.setSilent(true);
            dummy.setGravity(false);
            dummy.setPersistent(true);
            dummy.setMetadata(getName(), new FixedMetadataValue(Lobby.getInstance(), null));
        }

        public void update() {
            DHAPI.setHologramLine(hologram, 1, "&7" + "Queuing >> " + "&6" + getQueued());
        }

    }

    public abstract Location getPhysicalEntry();


}
