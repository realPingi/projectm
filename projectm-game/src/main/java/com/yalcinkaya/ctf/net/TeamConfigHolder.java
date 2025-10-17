package com.yalcinkaya.ctf.net;

import lombok.AllArgsConstructor;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
public class TeamConfigHolder {
    private final List<UUID> BLUE;
    private final List<UUID> RED;

    public List<UUID> getBlueTeam() {
        return BLUE;
    }

    public List<UUID> getRedTeam() {
        return RED;
    }
}
