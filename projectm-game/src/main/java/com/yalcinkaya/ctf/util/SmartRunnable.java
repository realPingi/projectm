package com.yalcinkaya.ctf.util;

import com.yalcinkaya.ctf.CTF;
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

    ;

    public int createTimer(int delay, final int cycles) {
        return createTimer(0, delay, cycles);
    }

    public int createTimer(int initalDelay, int delay, final int cycles) {
        this.cycles = cycles;
        runTaskTimer(CTF.getInstance(), initalDelay, delay);
        return getTaskId();
    }
}
