package com.yalcinkaya.ctf.stages;

import com.yalcinkaya.ctf.CTF;
import com.yalcinkaya.ctf.listener.StageListener;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

@Getter
@Setter
public abstract class CTFStage<T extends StageListener> {

    protected BukkitTask task;
    protected int timer;
    protected boolean countdown;
    protected T stageListener;
    protected boolean cancelled;

    public CTFStage(T stageListener) {
        this.stageListener = stageListener;
    }

    public void idle() {
    }

    public void start() {
        CTF.getInstance().setCurrentStage(this);
        CTF.getInstance().getServer().getPluginManager().registerEvents(stageListener, CTF.getInstance());
        task = new BukkitRunnable() {

            @Override
            public void run() {
                timer += countdown ? -1 : 1;
                if (task.isCancelled() || cancelled) {
                    return;
                }
                idle();
            }

        }.runTaskTimer(CTF.getInstance(),0, 1);
    }

    public void advance(CTFStage gameStage) {
        HandlerList.unregisterAll(stageListener);
        Bukkit.getScheduler().cancelTask(task.getTaskId());
        cancelled = true;
        gameStage.start();
    }

    public int getTime() {
        return timer / 20;
    }
}
