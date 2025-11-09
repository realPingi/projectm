package com.yalcinkaya.ctf.stages;

import com.google.gson.Gson;
import com.yalcinkaya.core.util.MathUtil;
import com.yalcinkaya.ctf.CTF;
import com.yalcinkaya.ctf.listener.PreStageListener;
import com.yalcinkaya.ctf.net.TeamConfigHolder;
import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import com.yalcinkaya.ctf.util.PlayerCamera;
import fr.mrmicky.fastboard.FastBoard;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Base64;
import java.util.UUID;

import static com.yalcinkaya.ctf.CTF.ctfWorld;
import static org.bukkit.Bukkit.getLogger;

public class PreStage extends CTFStage<PreStageListener> {

    private static final int windup = 120;

    public PreStage() {
        super(new PreStageListener());
    }

    @Override
    public void start() {
        super.start();
        setTimer(windup * 20);
        setCountdown(true);

        World world = Bukkit.getWorld(ctfWorld);
        world.setTime(6000);

        CTFUtil.loadMap();
        initializeTeams();

        Bukkit.getOnlinePlayers().forEach(player -> stageListener.setupPlayer(player));
    }

    @Override
    public void idle() {

        if (getTimer() == 0) {
            advance(new CaptureStage());
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {

            CTFUser user = CTFUtil.getUser(player.getUniqueId());

            if (user == null) {
                continue;
            }

            FastBoard board = user.getScoreboard();

            if (board == null) {
                continue;
            }

            String kitName = user.getKit() == null ? "None" : user.getKit().getName();

            board.updateTitle(ChatColor.GOLD + "" + ChatColor.BOLD + "ProjectM");
            board.updateLines(ChatColor.GRAY + "Starting in: " + ChatColor.GOLD + "" + MathUtil.getReadableSeconds(getTime()),
                    ChatColor.GRAY + "Kit: " + ChatColor.GOLD + kitName);
        }
    }

    public void initializeTeams() {

        Gson gson = new Gson();

        // 1. Base64-String aus Umgebungsvariable lesen
        String teamsConfigBase64 = CTF.getInstance().getTeamJson();

        if (teamsConfigBase64 == null || teamsConfigBase64.isEmpty()) {
            getLogger().severe("Keine TEAMS_CONFIG_B64 Umgebungsvariable gefunden! Match-Start fehlgeschlagen.");
            return;
        }

        try {
            // 2. Base64 dekodieren
            byte[] decodedBytes = Base64.getDecoder().decode(teamsConfigBase64);
            String teamsConfigJson = new String(decodedBytes);

            getLogger().info("Decoded Team Config JSON: " + teamsConfigJson.substring(0, Math.min(200, teamsConfigJson.length())) + "...");

            // 3. JSON in das Java-Objekt deserialisieren (Gson)
            TeamConfigHolder teamData = gson.fromJson(teamsConfigJson, TeamConfigHolder.class);

            // 4. Teams zuweisen und Manager füllen (DEINE LOGIK)
            assignPredefinedTeams(teamData);

        } catch (IllegalArgumentException e) {
            getLogger().severe("FEHLER beim Dekodieren oder Parsen der TEAMS_CONFIG (Ungültiges Base64 oder JSON): " + e.getMessage());
            // Plugin sollte hier deaktiviert werden, da die Konfiguration fehlerhaft ist.
            Bukkit.getPluginManager().disablePlugin(CTF.getInstance());
        } catch (Exception e) {
            getLogger().severe("Unerwarteter Fehler beim Initialisieren der Teams: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Zuweisungs-Logik: Fügt alle Spieler aus der Konfiguration dem UserManager hinzu
     */
    private void assignPredefinedTeams(TeamConfigHolder teamData) {

        teamData.getRedTeam().forEach(uuid -> assignTeam(uuid, false));
        teamData.getBlueTeam().forEach(uuid -> assignTeam(uuid, true));

        getLogger().info("Teams für das Match wurden erfolgreich zugewiesen.");
    }

    private void assignTeam(UUID uuid, boolean blue) {
        CTF.getInstance().getPlayerListener().getUserManager().addUser(uuid);
        CTFUser user = CTFUtil.getUser(uuid);
        CTFUtil.setTeam(user, blue);
        PlayerCamera camera = new PlayerCamera(uuid);
        CTF.getInstance().getCameras().add(camera);
    }
}
