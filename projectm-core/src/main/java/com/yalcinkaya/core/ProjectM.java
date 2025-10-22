package com.yalcinkaya.core;

import com.yalcinkaya.core.listener.PlayerListener;
import com.yalcinkaya.core.util.NametagManager;
import lombok.Getter;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ProjectM extends JavaPlugin {

    @Getter
    private NametagManager nametagManager;
    private final PluginManager pluginManager = getServer().getPluginManager();
    private final PlayerListener playerListener = new PlayerListener();
    @Getter
    private static ProjectM instance;

    @Override
    public void onEnable() {
        instance = this;
        nametagManager = new NametagManager();
        pluginManager.registerEvents(playerListener, this);
    }

    @Override
    public void onDisable() {

    }
}