package com.yalcinkaya.ctf.user;

import com.yalcinkaya.ctf.flag.Flag;
import com.yalcinkaya.ctf.kit.Kit;
import com.yalcinkaya.ctf.team.Team;
import com.yalcinkaya.ctf.util.SmartArmorStand;
import fr.mrmicky.fastboard.FastBoard;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.UUID;

@Data
public class CTFUser {

    private UUID uuid;
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
        this.uuid = uuid;
    }

    public boolean isCapturing() {
        return flag != null;
    }

    public void sendMessage(String message) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            player.sendMessage(message);
        }
    }

    public void playSound(Sound sound) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            player.playSound(player.getLocation(), sound, 1F, 0);
        }
    }

    public void playSoundForWorld(Sound sound) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            player.getWorld().playSound(player.getLocation(), sound, 1F, 0);
        }
    }

}
