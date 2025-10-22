package com.yalcinkaya.ctf.user;

import com.yalcinkaya.ctf.flag.Flag;
import com.yalcinkaya.ctf.kit.Kit;
import com.yalcinkaya.ctf.team.Team;
import com.yalcinkaya.util.SmartArmorStand;
import com.yalcinkaya.user.User;
import fr.mrmicky.fastboard.FastBoard;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class CTFUser extends User {

    private Team team;
    private Kit kit;
    private double energy;
    private boolean frozen;
    private Flag flag;
    private SmartArmorStand flagDisplay;
    private boolean cancelNextFallDamage;
    private long lastHeal;
    private boolean spectating;
    private FastBoard scoreboard;
    private UUID lastDamager;
    private boolean preventCapture;

    public CTFUser(UUID uuid) {
        super(uuid);
    }

    public boolean isCapturing() {
        return flag != null;
    }

}
