package com.yalcinkaya.ctf.user;

import com.yalcinkaya.core.user.User;
import com.yalcinkaya.ctf.CTF;
import com.yalcinkaya.ctf.flag.Flag;
import com.yalcinkaya.ctf.kit.Kit;
import com.yalcinkaya.ctf.team.Team;
import fr.mrmicky.fastboard.FastBoard;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CTFUser extends User {

    private Kit kit;
    private double energy;
    private boolean frozen;
    private Flag flag;
    private boolean cancelNextFallDamage;
    private long lastHeal;
    private boolean spectating;
    private FastBoard scoreboard;
    private UUID lastDamager;
    private boolean preventCapture;
    private CTFUser scoreboardSource;

    public CTFUser(UUID uuid) {
        super(uuid);
    }

    public boolean isCapturing() {
        return flag != null;
    }

    public Team getTeam() {
        Team blue = CTF.getInstance().getBlue();
        Team red = CTF.getInstance().getRed();
        if (blue.isMember(this)) return blue;
        if (red.isMember(this)) return red;
        return null;
    }

}
