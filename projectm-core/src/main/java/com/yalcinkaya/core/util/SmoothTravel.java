package com.yalcinkaya.core.util;

import com.yalcinkaya.core.ProjectM;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

@Getter
public class SmoothTravel implements Listener {

    private Player traveller;
    private Location start;
    private Location to;
    private double travelDistance;
    private double time;
    private double speed;
    private boolean spec;
    private boolean dontStopMeNow;
    private Vector vector;
    private boolean completed;
    private int taskId;

    public SmoothTravel(Player traveller, Location to, double time, boolean spec, boolean dontStopMeNow) {
        this.traveller = traveller;
        start = traveller.getLocation();
        this.to = to;
        travelDistance = start.distance(to);
        this.time = time;
        speed = travelDistance / (20 * time);
        this.spec = spec;
        this.dontStopMeNow = dontStopMeNow;

        if (travelDistance == 0) {
            vector = new Vector(0, 0, 0);
        }
    }

    public void start() {
        SmoothTravel smoothTravel = this;
        ProjectM.getInstance().getServer().getPluginManager().registerEvents(smoothTravel, ProjectM.getInstance());
        if (spec) {
            traveller.setGameMode(GameMode.SPECTATOR);
        }
        taskId = new BukkitRunnable() {

            @Override
            public void run() {
                if (travelDistance > 0) {
                    vector = to.toVector().subtract(traveller.getLocation().toVector()).normalize().multiply(speed);
                }
                traveller.setVelocity(vector);
                if (!dontStopMeNow && hasArrived()) {
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
        return traveller.getLocation().distance(start) >= travelDistance;
    }

}
