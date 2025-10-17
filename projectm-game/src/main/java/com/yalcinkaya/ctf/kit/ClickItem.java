package com.yalcinkaya.ctf.kit;

import com.yalcinkaya.ctf.user.CTFUser;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@Setter
@Getter
public abstract class ClickItem implements EnergyConsumer {

    protected final Cooldown cooldown = new Cooldown("", 0);

    public ClickItem() {
        cooldown.setId(getName());
        cooldown.setSeconds(getCooldownSeconds());
    }

    public abstract boolean tryClick(CTFUser activator);

    public abstract String getName();

    public abstract ItemStack getItem();

    public abstract Sound getSound();

    public abstract int getCooldownSeconds();

    public boolean isCooldown() {
        return cooldown.isActive();
    }

    public int getCurrentCooldown() {
        return cooldown.getSecondsLeft();
    }

    public void useCooldown() {
        cooldown.use();
    }

    public void sendWarning(Player player) {
        cooldown.sendWarning(player);
    }

}
