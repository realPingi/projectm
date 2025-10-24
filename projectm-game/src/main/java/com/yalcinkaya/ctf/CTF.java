package com.yalcinkaya.ctf;

import com.yalcinkaya.core.redis.QueueType;
import com.yalcinkaya.core.util.IntTuple;
import com.yalcinkaya.ctf.commands.*;
import com.yalcinkaya.ctf.listener.KitListener;
import com.yalcinkaya.ctf.listener.PlayerListener;
import com.yalcinkaya.ctf.map.Map;
import com.yalcinkaya.ctf.stages.CTFStage;
import com.yalcinkaya.ctf.stages.PreStage;
import com.yalcinkaya.ctf.team.Team;
import com.yalcinkaya.ctf.team.TeamColor;
import com.yalcinkaya.ctf.user.CTFUserManager;
import lombok.Getter;
import lombok.Setter;
import okhttp3.OkHttpClient;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.logging.Logger;

@Getter
public class CTF extends JavaPlugin {

    public static final String ctfWorld = "ctfWorld";
    private static final Logger LOGGER = Logger.getLogger("ctf");
    @Getter
    private static CTF instance;
    private final Team blue = new Team(TeamColor.BLUE);
    private final Team red = new Team(TeamColor.RED);
    private final HashMap<IntTuple, Integer> heightMap = new HashMap<>();
    private final PlayerListener playerListener = new PlayerListener(new CTFUserManager());
    private final KitListener kitListener = new KitListener();
    private final PluginManager pluginManager = getServer().getPluginManager();
    private OkHttpClient http = new OkHttpClient();
    @Setter
    private Map map;
    @Setter
    private CTFStage currentStage;
    @Setter
    private String matchId;
    @Setter
    private String mapId;
    @Setter
    private String teamJson;
    @Setter
    private QueueType queueType;

    public void onEnable() {
        LOGGER.info("projectm enabled");

        instance = this;

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        pluginManager.registerEvents(kitListener, this);
        pluginManager.registerEvents(playerListener, this);

        this.getCommand("kit").setExecutor(new KitCommand());
        this.getCommand("setkit").setExecutor(new SetKitCommand());
        this.getCommand("energy").setExecutor(new EnergyCommand());
        this.getCommand("swapteam").setExecutor(new SwapTeamCommand());
        this.getCommand("togglespec").setExecutor(new ToggleSpecCommand());
        this.getCommand("setcounter").setExecutor(new SetCounterCommand());
        this.getCommand("settimer").setExecutor(new SetTimerCommand());
        this.getCommand("setflag").setExecutor(new SetFlagCommand());
        this.getCommand("forcewin").setExecutor(new ForceWinCommand());
        this.getCommand("assignmatch").setExecutor(new AssignMatchCommand());
    }

    public void start() {
        new PreStage().start();
    }

    public void onDisable() {
        LOGGER.info("projectm disabled");
    }

}
