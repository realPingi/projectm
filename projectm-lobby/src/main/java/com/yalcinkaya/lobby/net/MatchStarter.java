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

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
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

            participants.forEach(uuid -> LobbyUtil.getUser(uuid).sendMessage(LobbyUtil.getLobbyMessage(MessageType.INFO, ChatColor.GRAY + "Starting match...")));

            new SmartRunnable() {
                @Override
                public void cycle() {
                    if (isServerReady("91.98.203.124", serverPort)) {
                        redirectPlayers(participants, serverPort);
                        cancel();
                    }
                }
            }.createTimer(20 * 3, 20 * 3, 20);

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

    private boolean isServerReady(String host, int port) {
        final int timeoutMs = 1200;        // socket timeout pro Versuch
        final int requiredOk = 3;          // wie viele erfolgreiche Status-Pings in Folge
        final long gapBetweenOkMs = 300;   // Abstand zwischen Erfolgen (stabilität)
        final long graceAfterFirstOkMs = 500; // kleine Schonfrist, weil Status oft 1-2 Ticks vor Login steht

        int okInRow = 0;
        long firstOkAt = -1;

        for (int attempt = 0; attempt < 25; attempt++) {   // ~25 Versuche (insg. ~15-20s je nach Sleep)
            try {
                if (mcStatusPing(host, port, timeoutMs)) {
                    okInRow++;

                    if (firstOkAt < 0) {
                        firstOkAt = System.nanoTime();
                    } else {
                        // Mindestabstand zwischen Erfolgen erzwingen
                        Thread.sleep(gapBetweenOkMs);
                    }

                    // Optional: kurze Schonfrist nach erstem OK
                    long sinceFirst = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - firstOkAt);
                    if (okInRow >= requiredOk && sinceFirst >= graceAfterFirstOkMs) {
                        return true; // stabil bereit
                    }
                } else {
                    okInRow = 0; // Kette unterbrochen
                    firstOkAt = -1;
                }
            } catch (Exception ignore) {
                okInRow = 0;
                firstOkAt = -1;
            }

            // kleiner Backoff zwischen Versuchen
            try { Thread.sleep(250); } catch (InterruptedException ignored) {}
        }
        return false; // keine stabile Bereitschaft erreicht
    }

    /**
     * Führt den Minecraft-Status-Ping aus (Handshake -> Status Request) und gibt true zurück,
     * wenn eine valide JSON-Antwort kommt. Das ist deutlich näher an "Join möglich" als nur TCP.
     */
    private boolean mcStatusPing(String host, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            socket.setSoTimeout(timeoutMs);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream  in  = new DataInputStream(socket.getInputStream());

            // ---- Handshake (0x00) -> state=1 (status) ----
            ByteArrayOutputStream handshake = new ByteArrayOutputStream();
            DataOutputStream hs = new DataOutputStream(handshake);

            writeVarInt(hs, 0x00);                 // packet id
            writeVarInt(hs, 758);                  // protocol version (758 = 1.21.1; Wert ist hier egal für Status)
            writeString(hs, host);
            hs.writeShort(port & 0xFFFF);
            writeVarInt(hs, 0x01);                 // next state = 1 (status)

            writeVarInt(out, handshake.size());
            out.write(handshake.toByteArray());

            // ---- Request (0x00) ----
            writeVarInt(out, 1);  // length of next packet
            out.writeByte(0x00);  // id = 0 (status request)
            out.flush();

            // ---- Response ----
            int size = readVarInt(in);
            if (size <= 0 || size > 32767) return false;
            int packetId = readVarInt(in);
            if (packetId != 0x00) return false;

            int jsonLen = readVarInt(in);
            if (jsonLen <= 0 || jsonLen > 1_000_000) return false;

            byte[] jsonBytes = new byte[jsonLen];
            in.readFully(jsonBytes);

            String json = new String(jsonBytes, StandardCharsets.UTF_8);
            // Minimalprüfung: gültiges JSON-Objekt erwartet
            return json.startsWith("{") && json.endsWith("}");
        } catch (IOException e) {
            return false;
        }
    }

    /* ---------- VarInt & String Utils für das MC-Protokoll ---------- */

    private void writeVarInt(DataOutput out, int value) throws IOException {
        while ((value & ~0x7F) != 0) {
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    private int readVarInt(DataInput in) throws IOException {
        int numRead = 0;
        int result = 0;
        byte read;
        do {
            read = in.readByte();
            int value = (read & 0x7F);
            result |= (value << (7 * numRead));

            numRead++;
            if (numRead > 5) throw new IOException("VarInt too big");
        } while ((read & 0x80) == 0x80);
        return result;
    }

    private void writeString(DataOutput out, String s) throws IOException {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, b.length);
        out.write(b);
    }
}
