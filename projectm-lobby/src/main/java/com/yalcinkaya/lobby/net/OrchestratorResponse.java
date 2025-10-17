package com.yalcinkaya.lobby.net;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter          // Generiert alle Getter-Methoden (z.B. getGamePort())
@NoArgsConstructor // Wichtig für Gson (parameterloser Konstruktor)
@AllArgsConstructor // Optional, aber nützlich für Tests
public class OrchestratorResponse {

    // Muss dem 'ok: true' Status aus Node.js entsprechen
    private boolean ok;

    // Muss dem 'matchId' String aus Node.js entsprechen
    private String matchId;

    // Process ID des Kindprozesses (für Debugging)
    private int pid;

    // KRITISCH: Der dynamische Game Server Port
    private int gamePort;

    // HILFSFUNKTIONEN (Optional, aber empfohlen)

    /**
     * Konvertiert den matchId String in eine UUID.
     *
     * @return Die Java UUID.
     */
    public UUID getMatchUuid() {
        return UUID.fromString(this.matchId);
    }
}
