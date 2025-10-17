package com.yalcinkaya.lobby.net;

import com.google.gson.Gson;
import com.velocitypowered.api.proxy.ProxyServer;
import com.yalcinkaya.lobby.Lobby;
import com.yalcinkaya.lobby.util.LobbyUtil;
import com.yalcinkaya.lobby.util.MessageType;
import com.yalcinkaya.lobby.util.SmartRunnable;
import okhttp3.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class MatchStarter {

    private static final String[] AVAILABLE_MAPS = {"basic"};
    private final Gson gson = new Gson();
    private final OkHttpClient http = new OkHttpClient();
    private final String ORCHESTRATOR_URL = "http://91.98.203.124:4000";

    public void startMatch(List<UUID> blueTeam, List<UUID> redTeam, String mapId) {

        Map<String, List<String>> teamsPayload = new HashMap<>();
        teamsPayload.put("BLUE", blueTeam.stream().map(UUID::toString).collect(Collectors.toList()));
        teamsPayload.put("RED", redTeam.stream().map(UUID::toString).collect(Collectors.toList()));

        Map<String, Object> matchConfig = new HashMap<>();
        matchConfig.put("mapId", mapId);

        String teamsJson = gson.toJson(teamsPayload);
        String teamsBase64 = Base64.getEncoder().encodeToString(teamsJson.getBytes(StandardCharsets.UTF_8));
        matchConfig.put("teamsConfigBase64", teamsBase64);

        String jsonConfig = gson.toJson(matchConfig);

        List<UUID> participants = new ArrayList<>();
        participants.addAll(blueTeam);
        participants.addAll(redTeam);

        Bukkit.getScheduler().runTaskAsynchronously(Lobby.getInstance(), () -> sendStartRequest(jsonConfig, participants));
    }

    private void sendStartRequest(String jsonConfig, List<UUID> participants) {

        RequestBody body = RequestBody.create(jsonConfig, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder().url(ORCHESTRATOR_URL + "/create-match").post(body).build();

        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Orchestrator returned error: " + response.code());
            }

            String responseBody = response.body().string();
            OrchestratorResponse orchResponse = gson.fromJson(responseBody, OrchestratorResponse.class);

            int serverPort = orchResponse.getGamePort();

            participants.forEach(uuid -> LobbyUtil.getUser(uuid).sendMessage(LobbyUtil.getLobbyMessage(MessageType.INFO, ChatColor.GRAY + "Starting match on game server: ", "91.98.203.124:" + serverPort)));

            new SmartRunnable() {
                @Override
                public void cycle() {
                    redirectPlayers(participants, serverPort);
                }
            }.createTimer(20 * 8, 20 * 4, 10);

        } catch (Exception e) {
            Lobby.getInstance().getLogger().severe("Failed to start match: " + e.getMessage());
        }
    }

    private void redirectPlayers(List<UUID> participants, int port) {
        int gameNumber = port - 25565;
        participants.stream()
                .map(Bukkit::getPlayer)
                .filter(p -> p != null && p.isOnline())
                .forEach(player -> sendPlayerConnect(player, gameNumber));
    }

    private void sendPlayerConnect(Player player, int gameNumber) {

        final String serverName = "game-" + gameNumber;

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
        int randomIndex = random.nextInt(AVAILABLE_MAPS.length);
        return AVAILABLE_MAPS[randomIndex];
    }
}
