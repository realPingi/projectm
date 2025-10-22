package com.yalcinkaya.ctf.kit;

import com.yalcinkaya.core.util.MessageType;
import com.yalcinkaya.ctf.util.CTFUtil;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

@Getter
@Setter
public class Cooldown {

    private long lastUse;
    private int seconds;
    private String id;
    private EnergyConsumer energyConsumer;

    public Cooldown(String id, int seconds) {
        this(id, seconds, null);
    }

    public Cooldown(String id, int seconds, EnergyConsumer energyConsumer) {
        this.id = id;
        this.seconds = seconds;
        this.energyConsumer = energyConsumer;
    }

    public boolean isActive() {
        return lastUse != 0 && System.currentTimeMillis() - lastUse < seconds * 1000L;
    }

    public int getSecondsLeft() {
        return lastUse == 0 ? 0 : Math.max(0, seconds - Math.round((float) (System.currentTimeMillis() - lastUse) / 1000));
    }

    public void use() {
        lastUse = System.currentTimeMillis();
    }

    public void sendWarning(Player player) {
        CTFUtil.sendMessage(player, MessageType.WARNING, "You are still on cooldown for ", "" + getSecondsLeft(), " seconds.");
    }
}
