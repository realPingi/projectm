package com.yalcinkaya.lobby.net;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.bukkit.Bukkit;

import java.io.IOException;
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

    private static String normalizeUuid(String s) {
        return s == null ? "" : s.replace("-", "").toLowerCase(Locale.ROOT);
    }

    public boolean isPlayerWaitingForMatch(UUID playerId) {
        final String needle = playerId.toString().replace("-", "").toLowerCase(Locale.ROOT);

        try {
            // Angenommen, fetchInUse() liefert die InUseResponse (enthält pending und active Listen)
            InUseResponse iu = fetchInUse();

            if (iu != null && iu.ok) {
                // Spieler ist 'pending', wenn er in der PENDING-Liste ist
                if (iu.pending != null && iu.pending.contains(needle)) {

                    // Wir müssen auch prüfen, ob er NICHT GLEICHZEITIG active ist.
                    // Im Normalfall sollte PENDING nur Locks enthalten, die noch nicht in ACTIVE
                    // überführt wurden.
                    boolean isActive = iu.active != null && iu.active.contains(needle);

                    // Spieler wartet, wenn er pending ist, aber noch nicht als active geführt wird.
                    return !isActive;
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[MatchLookupService] Pending check failed: " + e.getMessage());
        }
        return false;
    }

    public boolean isPlayerInActiveMatch(UUID playerId) {
        final String needle = playerId.toString().replace("-", "").toLowerCase(Locale.ROOT);

        try {
            InUseResponse iu = fetchInUse();
            if (iu != null && iu.ok) {
                // Spieler ist 'active', wenn er in der ACTIVE-Liste ist
                return iu.active != null && iu.active.contains(needle);
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[MatchLookupService] Active check failed: " + e.getMessage());
        }

        // Fallback: Manuelle Prüfung der Match-Liste (wie in findMatchFor)
        return findMatchFor(playerId).isPresent();
    }

    /* ------------- intern ------------- */

    /**
     * Liefert das Match (inkl. Port), falls der Spieler registriert ist (aktive Matches).
     */
    public Optional<MatchInfo> findMatchFor(UUID playerId) {
        try {
            List<MatchInfo> matches = fetchMatches();
            if (matches == null || matches.isEmpty()) return Optional.empty();

            for (MatchInfo m : matches) {
                if (m.port <= 0) continue;
                // Verwendet die interne Cache-Logik des MatchInfo-Objekts
                if (m.containsPlayer(playerId)) return Optional.of(m);
            }
            return Optional.empty();
        } catch (Exception e) {
            Bukkit.getLogger().warning("[MatchLookupService] findMatchFor error: " + e.getMessage());
            return Optional.empty();
        }
    }

    public List<MatchInfo> fetchMatches() throws IOException {
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

    /**
     * /in-use liefert pending + active + vereinigt (inUse)
     */
    private InUseResponse fetchInUse() throws IOException {
        Request req = new Request.Builder()
                .url(orchestratorUrl + "/in-use")
                .get()
                .build();
        try (Response resp = http.newCall(req).execute()) {
            if (!resp.isSuccessful()) return null;
            String body = resp.body() != null ? resp.body().string() : "{}";
            return gson.fromJson(body, InUseResponse.class);
        }
    }

    /* ------------- DTOs ------------- */

    public static class MatchesResponse {
        @SerializedName("ok")
        public boolean ok;
        @SerializedName("matches")
        public List<MatchInfo> matches;

        // optional vorhanden (wenn du's im Orchestrator mitsendest)
        @SerializedName("pendingPlayers")
        public List<String> pendingPlayers;
    }

    public static class InUseResponse {
        @SerializedName("ok")
        public boolean ok;
        @SerializedName("inUse")
        public List<String> inUse;   // union(pending, active)
        @SerializedName("pending")
        public List<String> pending; // nur Locks/Queue
        @SerializedName("active")
        public List<String> active;  // aus laufenden Matches
    }

    public static class MatchInfo {
        // sehr tolerante Erkennung: 32 hex (ohne Striche) ODER 36 mit Strichen
        private static final java.util.regex.Pattern UUID_ANY =
                java.util.regex.Pattern.compile("\\b[0-9a-fA-F]{32}\\b|\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b");
        @SerializedName("containerId")
        public String containerId;
        @SerializedName("name")
        public String name;
        @SerializedName("port")
        public int port;
        @SerializedName("teamsConfigBase64")
        public String teamsConfigBase64;
        // Cache der normalisierten UUIDs (lowercase, ohne Striche)
        private transient Set<String> cachedPlayerIdsNorm;

        private static String normalizeUuid(String s) {
            return s == null ? "" : s.replace("-", "").toLowerCase(Locale.ROOT);
        }

        /**
         * traversiert beliebig tief: Arrays, Objekte, Primitive; sammelt alle UUID-ähnlichen Strings
         */
        private static void collectUuidsDeep(JsonElement el, Set<String> out) {
            if (el == null || el.isJsonNull()) return;

            if (el.isJsonPrimitive()) {
                JsonPrimitive p = el.getAsJsonPrimitive();
                if (p.isString()) {
                    String s = p.getAsString();
                    java.util.regex.Matcher m = UUID_ANY.matcher(s);
                    while (m.find()) out.add(normalizeUuid(m.group()));
                }
                return;
            }

            if (el.isJsonArray()) {
                for (JsonElement e : el.getAsJsonArray()) collectUuidsDeep(e, out);
                return;
            }

            if (el.isJsonObject()) {
                JsonObject o = el.getAsJsonObject();

                // häufige Felder direkt prüfen
                for (String key : Arrays.asList("id", "uuid", "player", "playerId")) {
                    if (o.has(key) && o.get(key).isJsonPrimitive()) {
                        String s = o.get(key).getAsString();
                        java.util.regex.Matcher m = UUID_ANY.matcher(s);
                        while (m.find()) out.add(normalizeUuid(m.group()));
                    }
                }

                // alle Werte traversieren (deckt Maps wie {"t1":[...], "t2":[...]} ab)
                for (Map.Entry<String, JsonElement> e : o.entrySet()) {
                    collectUuidsDeep(e.getValue(), out);
                }
            }
        }

        /**
         * True, wenn der Spieler in den Teams enthalten ist (robust gegen Formatunterschiede).
         */
        public boolean containsPlayer(UUID playerId) {
            if (playerId == null) return false;
            ensureDecodedPlayers();
            return cachedPlayerIdsNorm.contains(normalizeUuid(playerId.toString()));
        }

        /**
         * Utility: Velocity-Servername wie in deiner Config
         */
        public String toVelocityServerName() {
            int gameNumber = port - 25565;
            return "game-" + gameNumber;
        }

        /**
         * einmalig: TEAMS_CONFIG_B64 → JSON → alle UUID-Strings (mit/ohne Striche) einsammeln
         */
        private void ensureDecodedPlayers() {
            if (cachedPlayerIdsNorm != null) return;
            cachedPlayerIdsNorm = new HashSet<>();
            if (teamsConfigBase64 == null || teamsConfigBase64.isEmpty()) return;

            try {
                byte[] raw = Base64.getDecoder().decode(teamsConfigBase64);
                String json = new String(raw, StandardCharsets.UTF_8);
                // Wichtig: Verwende den neuen JsonParser
                JsonElement root = JsonParser.parseString(json);
                collectUuidsDeep(root, cachedPlayerIdsNorm);
            } catch (Exception ignored) {
                // leer lassen
            }
        }
    }

}
