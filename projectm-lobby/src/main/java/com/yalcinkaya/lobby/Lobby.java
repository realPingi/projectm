package com.yalcinkaya.lobby;

import com.yalcinkaya.lobby.commands.*;
import com.yalcinkaya.lobby.hotbar.HotbarManager;
import com.yalcinkaya.lobby.leaderboard.LeaderboardManager;
import com.yalcinkaya.lobby.listener.HotbarListener;
import com.yalcinkaya.lobby.listener.PlayerListener;
import com.yalcinkaya.lobby.listener.QueueListener;
import com.yalcinkaya.lobby.net.MatchStarter;
import com.yalcinkaya.lobby.party.PartyManager;
import com.yalcinkaya.lobby.queue.QueueManager;
import com.yalcinkaya.lobby.user.LobbyUserManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Lobby extends JavaPlugin {

    private static final String PROXY_CHANNEL = "BungeeCord";
    @Getter
    private static Lobby instance;
    @Getter
    private final QueueManager queueManager = new QueueManager();
    @Getter
    private HotbarManager hotbarManager = new HotbarManager();
    @Getter
    private PartyManager partyManager = new PartyManager();
    @Getter
    private final LobbyUserManager userManager = new LobbyUserManager();
    @Getter
    private final LeaderboardManager leaderboardManager = new LeaderboardManager();

    private final PlayerListener playerListener = new PlayerListener();
    private final HotbarListener hotbarListener = new HotbarListener();
    private final QueueListener queueListener = new QueueListener();
    private final PluginManager pluginManager = getServer().getPluginManager();

    @Getter
    private final MatchStarter matchStarter = new MatchStarter();

    @Override
    public void onEnable() {
        instance = this;

        getServer().getMessenger().registerOutgoingPluginChannel(this, PROXY_CHANNEL);
        getLogger().info("ProjectM lobby plugin enabled and registered for proxy communication.");

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        pluginManager.registerEvents(playerListener, this);
        pluginManager.registerEvents(hotbarListener, this);
        pluginManager.registerEvents(queueListener, this);

        this.getCommand("reconnect").setExecutor(new ReconnectCommand());
        this.getCommand("party").setExecutor(new PartyCommand());
        this.getCommand("spawn").setExecutor(new SpawnCommand());
        this.getCommand("elo").setExecutor(new EloCommand());
        this.getCommand("setelo").setExecutor(new SetEloCommand());
        this.getCommand("addelo").setExecutor(new AddEloCommand());
        this.getCommand("setrank").setExecutor(new SetRankCommand());
        this.getCommand("setsq").setExecutor(new SetSQCommand());
        this.getCommand("setpq").setExecutor(new SetPQCommand());
        this.getCommand("spectate").setExecutor(new SpectateCommand());

        queueManager.loadQueues();
        leaderboardManager.init();

        World world = Bukkit.getWorld("world");
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, PROXY_CHANNEL);
        getLogger().info("Projectm lobby plugin disabled.");
    }

}