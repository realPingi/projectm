package com.yalcinkaya.lobby.net;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class MatchLookupService {

    private final String orchestratorUrl = "http://91.98.203.124:4000";
    private final OkHttpClient http;
    private final Gson gson = new Gson();

    public MatchLookupService() {
        // konservative Timeouts, damit der Command snappy bleibt
        this.http = new OkHttpClient.Builder()
                .connectTimeout(1500, TimeUnit.MILLISECONDS)
                .readTimeout(2000, TimeUnit.MILLISECONDS)
                .writeTimeout(2000, TimeUnit.MILLISECONDS)
                .build();
    }

    /** Reusable: Ist der Spieler in irgendeinem laufenden Match registriert? */
    public boolean isPlayerInAnyMatch(UUID playerId) {
        return findMatchFor(playerId).isPresent();
    }

    /** Reusable: Liefert das Match (inkl. Port), falls der Spieler registriert ist. */
    public Optional<MatchInfo> findMatchFor(UUID playerId) {
        try {
            List<MatchInfo> matches = fetchMatches();
            if (matches == null || matches.isEmpty()) return Optional.empty();

            String needle = playerId.toString();
            for (MatchInfo m : matches) {
                if (m.teamsConfigBase64 == null || m.port <= 0) continue;
                Map<String, List<String>> teams = decodeTeams(m.teamsConfigBase64);
                if (teams == null || teams.isEmpty()) continue;
                if (containsPlayer(teams, needle)) return Optional.of(m);
            }
            return Optional.empty();
        } catch (Exception e) {
            Bukkit.getLogger().warning("[MatchLookupService] findMatchFor error: " + e.getMessage());
            return Optional.empty();
        }
    }

    /* ------------- intern ------------- */

    private List<MatchInfo> fetchMatches() throws IOException {
        Request req = new Request.Builder()
                .url(orchestratorUrl + "/matches")
                .get()
                .build();
        try (Response resp = http.newCall(req).execute()) {
            if (!resp.isSuccessful()) return Collections.emptyList();
            String body = resp.body() != null ? resp.body().string() : "{}";
            MatchesResponse parsed = gson.fromJson(body, MatchesResponse.class);
            if (parsed == null || !parsed.ok || parsed.matches == null) return Collections.emptyList();
            return parsed.matches;
        }
    }

    private Map<String, List<String>> decodeTeams(String b64) {
        try {
            byte[] raw = Base64.getDecoder().decode(b64);
            String json = new String(raw, StandardCharsets.UTF_8);
            Type type = new TypeToken<Map<String, List<String>>>() {}.getType();
            return gson.fromJson(json, type);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean containsPlayer(Map<String, List<String>> teams, String uuidStr) {
        for (List<String> list : teams.values()) {
            if (list != null && list.contains(uuidStr)) return true;
        }
        return false;
    }

    /* ------------- DTOs ------------- */

    public static class MatchesResponse {
        @SerializedName("ok") public boolean ok;
        @SerializedName("matches") public List<MatchInfo> matches;
    }

    public static class MatchInfo {
        @SerializedName("containerId") public String containerId;
        @SerializedName("name")        public String name;
        @SerializedName("port")        public int port;
        @SerializedName("teamsConfigBase64") public String teamsConfigBase64;

        /** Utility: Velocity-Servername wie in deiner Config */
        public String toVelocityServerName() {
            int gameNumber = port - 25565;
            return "game-" + gameNumber;
        }
    }
}