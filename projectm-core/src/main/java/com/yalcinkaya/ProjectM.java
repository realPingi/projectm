package com.yalcinkaya;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

public class ProjectM extends JavaPlugin {

    @Getter
    private static ProjectM instance;

    @Override
    public void onEnable() {
        instance = this;
    }

    @Override
    public void onDisable() {

    }
}