package com.yalcinkaya.core.util;

import com.yalcinkaya.core.ProjectM;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

@Getter
public class SmoothTravel implements Listener {

    private final Player traveller;
    private Player target;
    private Vector offset;
    private Location start;
    private Location to;
    private double travelDistance;
    private double speed;
    private boolean spec;
    private Vector vector;
    private boolean completed;
    private int taskId;

    public SmoothTravel(Player traveller, Player target, Vector offset) {
        this.traveller = traveller;
        this.target = target;
        this.offset = offset;
        start = traveller.getLocation();
    }

    public SmoothTravel(Player traveller, Location to, double time, boolean spec) {
        this.traveller = traveller;
        start = traveller.getLocation();
        this.to = to;
        travelDistance = start.distance(to);
        speed = travelDistance / (20 * time);
        this.spec = spec;
    }

    @EventHandler
    public void onTargetMove(PlayerMoveEvent event) {

        if (target == null) {
            return;
        }

        if (event.getPlayer().getUniqueId().equals(target.getUniqueId())) {
            Vector direction = event.getTo().toVector().subtract(event.getFrom().toVector()).setY(0);
            traveller.setVelocity(direction);
        }
    }

    public void start() {
        SmoothTravel smoothTravel = this;
        ProjectM.getInstance().getServer().getPluginManager().registerEvents(smoothTravel, ProjectM.getInstance());
        if (spec) {
            traveller.setGameMode(GameMode.SPECTATOR);
        }
        if (target != null) {
            traveller.teleport(target.getLocation().clone().add(offset));
        }
        taskId = new BukkitRunnable() {

            @Override
            public void run() {

                if (target != null) {
                    return;
                }

                vector = to.toVector().subtract(traveller.getLocation().toVector()).normalize().multiply(speed);
                traveller.setVelocity(vector);
                if (hasArrived()) {
                    smoothTravel.cancel();
                }
            }

        }.runTaskTimer(ProjectM.getInstance(), 0, 1).getTaskId();
    }

    public void cancel() {
        if (spec) {
            traveller.setGameMode(GameMode.SURVIVAL);
        }
        completed = true;
        traveller.setFallDistance(0);
        traveller.setVelocity(new Vector(0, 0, 0));
        HandlerList.unregisterAll(this);
        Bukkit.getServer().getScheduler().cancelTask(taskId);
    }

    private boolean hasArrived() {
        return travelDistance == 0 || traveller.getLocation().distance(start) >= travelDistance;
    }

}
