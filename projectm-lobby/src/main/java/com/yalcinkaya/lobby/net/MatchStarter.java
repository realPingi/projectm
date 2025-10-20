package com.yalcinkaya.lobby.net;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.yalcinkaya.lobby.Lobby;
import okhttp3.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class MatchStarter {

    private static final String[] AVAILABLE_MAPS = {"basic"};

    private final Gson gson = new Gson();
    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(65))    // > READY_TIMEOUT_MS im Orchestrator
            .writeTimeout(Duration.ofSeconds(15))
            .callTimeout(Duration.ofSeconds(65))
            .build();

    private final String ORCHESTRATOR_URL = "http://91.98.203.124:4000";

    public void startMatch(List<UUID> blueTeam, List<UUID> redTeam, String mapId) {
        if (mapId == null || mapId.isEmpty()) mapId = selectRandomMap();

        Map<String, List<String>> teamsPayload = new HashMap<>();
        teamsPayload.put("BLUE", blueTeam.stream().map(UUID::toString).collect(Collectors.toList()));
        teamsPayload.put("RED",  redTeam.stream().map(UUID::toString).collect(Collectors.toList()));

        Map<String, Object> matchConfig = new HashMap<>();
        matchConfig.put("mapId", mapId);

        String teamsJson  = gson.toJson(teamsPayload);
        String teamsBase64 = Base64.getEncoder().encodeToString(teamsJson.getBytes(StandardCharsets.UTF_8));
        matchConfig.put("teamsConfigBase64", teamsBase64);

        String jsonConfig = gson.toJson(matchConfig);

        List<UUID> participants = new ArrayList<>();
        participants.addAll(blueTeam);
        participants.addAll(redTeam);

        final String reqJson = jsonConfig;
        Bukkit.getScheduler().runTaskAsynchronously(Lobby.getInstance(), () -> sendStartRequest(reqJson, participants));
    }

    private void sendStartRequest(String jsonConfig, List<UUID> participants) {
        RequestBody body = RequestBody.create(jsonConfig, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(ORCHESTRATOR_URL + "/create-match")
                .post(body)
                .build();

        try (Response response = http.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                throw new IOException("Orchestrator HTTP " + response.code() + ": " + responseBody);
            }

            OrchestratorResponse orch = gson.fromJson(responseBody, OrchestratorResponse.class);
            if (orch == null) throw new IOException("Empty response from orchestrator");
            if (!orch.ok)     throw new IOException("Orchestrator error: " + (orch.error != null ? orch.error : "unknown"));

            if (orch.gamePort <= 0) throw new IOException("Missing gamePort in response");

            // Log nett in die Konsole
            Lobby.getInstance().getLogger().info(String.format(
                    "Match ready on port %d (matchId=%s, fromPool=%s, container=%s)",
                    orch.gamePort, orNull(orch.matchId), String.valueOf(orch.fromPool), orNull(orch.containerId)));

            redirectPlayers(participants, orch.gamePort);

        } catch (Exception e) {
            Lobby.getInstance().getLogger().severe("Failed to start match: " + e.getMessage());
        }
    }

    private static String orNull(String s) { return s == null ? "-" : s; }

    /** Leitet alle Spieler via Bungee/Velocity Plugin Messaging zum Zielserver um. */
    private void redirectPlayers(List<UUID> participants, int port) {
        // Dein Proxy ist so konfiguriert, dass "game-{N}" -> 127.0.0.1 : (25565 + N) zeigt.
        final int gameNumber = port - 25565;
        final String serverName = "game-" + gameNumber;

        participants.stream()
                .map(Bukkit::getPlayer)
                .filter(p -> p != null && p.isOnline())
                .forEach(p -> sendPlayerConnect(p, serverName));
    }

    private void sendPlayerConnect(Player player, String serverName) {
        try (ByteArrayOutputStream b = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(b)) {

            out.writeUTF("Connect");
            out.writeUTF(serverName);
            player.sendPluginMessage(Lobby.getInstance(), "BungeeCord", b.toByteArray());

        } catch (IOException e) {
            Lobby.getInstance().getLogger().severe("Failed to send Connect message to proxy: " + e.getMessage());
            player.sendMessage("§cInterner Fehler bei der Weiterleitung.");
        }
    }

    public String selectRandomMap() {
        Random random = new Random();
        return AVAILABLE_MAPS[random.nextInt(AVAILABLE_MAPS.length)];
    }

    /* ───────── DTO für die Orchestrator-Antwort ───────── */
    static class OrchestratorResponse {
        boolean ok;
        String error;

        @SerializedName("matchId")   String matchId;
        @SerializedName("name")      String name;
        @SerializedName("gamePort")  int gamePort;
        @SerializedName("containerId") String containerId;
        @SerializedName("fromPool")  boolean fromPool;
    }
}