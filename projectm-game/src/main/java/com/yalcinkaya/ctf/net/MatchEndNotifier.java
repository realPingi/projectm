package com.yalcinkaya.ctf.net;

import okhttp3.*;

import java.io.IOException;

public class MatchEndNotifier {

    private static final OkHttpClient client = new OkHttpClient();
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final String ORCHESTRATOR_URL = "http://91.98.203.124:4000/end-match";

    /**
     * Fordert den den Orchestrator zum Beenden auf.
     */
    public void closeMatch(String matchId) {
        sendTerminationSignal(matchId);
    }

    private void sendTerminationSignal(String matchId) {
        String jsonBody = "{\"matchId\":\"" + matchId + "\"}";
        Request request = new Request.Builder()
                .url(ORCHESTRATOR_URL)
                .post(RequestBody.create(JSON, jsonBody))
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                System.err.println("[MatchNotifier] FAILED to send termination signal to Orchestrator: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    System.out.println("[MatchNotifier] Termination signal successfully sent to Orchestrator.");
                } else {
                    System.err.println("[MatchNotifier] Orchestrator responded with HTTP error: " + response.code() + ". Termination may have failed.");
                }

                response.close();
            }
        });
    }
}
