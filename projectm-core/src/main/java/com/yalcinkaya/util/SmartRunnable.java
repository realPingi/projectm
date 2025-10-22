package com.yalcinkaya.util;

import com.yalcinkaya.ProjectM;
import org.bukkit.scheduler.BukkitRunnable;

public class SmartRunnable extends BukkitRunnable {

    int cycles = -1;
    int currentCycles;

    @Override
    public void run() {
        if (cycles > 0) {
            currentCycles++;
            if (cycles < currentCycles) {
                cancel();
                return;
            }
        }
        cycle();
    }

    public void cycle() {
    }

    public int createTimer(int delay, final int cycles) {
        return createTimer(0, delay, cycles);
    }

    public int createTimer(int initalDelay, int delay, final int cycles) {
        this.cycles = cycles;
        runTaskTimer(ProjectM.getInstance(), initalDelay, delay);
        return getTaskId();
    }
}
