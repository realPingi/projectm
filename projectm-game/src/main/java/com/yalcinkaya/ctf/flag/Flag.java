package com.yalcinkaya.ctf.flag;

import com.yalcinkaya.ctf.team.TeamColor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

@Getter
@Setter
public class Flag {

    private FlagLocation location;
    private Location home;
    private CaptureStatus status = CaptureStatus.SAFE;
    private TeamColor team;

    public Flag(FlagLocation location, Location home, TeamColor team) {
        this.location = location;
        this.home = home;
        this.team = team;
    }

}
