package com.yalcinkaya.lobby;

import com.yalcinkaya.lobby.commands.PartyCommand;
import com.yalcinkaya.lobby.commands.ReconnectCommand;
import com.yalcinkaya.lobby.hotbar.HotbarManager;
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
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Level;

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

        queueManager.loadQueues();

        World world = Bukkit.getWorld("world");
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, PROXY_CHANNEL);
        getLogger().info("Projectm lobby plugin disabled.");
    }


    private void connectPlayerToMatch(Player player, String ip, int port) {
        String serverAddress = ip + ":" + port;

        try (ByteArrayOutputStream b = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(b)) {

            out.writeUTF("Connect");
            out.writeUTF(serverAddress);

            player.sendPluginMessage(this, PROXY_CHANNEL, b.toByteArray());

            player.sendMessage("§bMatch gefunden! Verbinde dich mit dem Server...");

        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Failed to send connect message to proxy for player " + player.getName(), e);
            player.sendMessage("§cInterner Fehler bei der Verbindung. Bitte melde diesen Fehler.");
        }
    }

}