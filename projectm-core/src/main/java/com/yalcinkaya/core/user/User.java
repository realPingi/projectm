package com.yalcinkaya.core.user;

import lombok.Getter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.UUID;

@Getter
public class User {

    private final UUID uuid;

    public User(UUID uuid) {
        this.uuid = uuid;
    }

    public void sendMessage(String message) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(message));
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return uuid != null && uuid.equals(user.uuid);
    }

    @Override
    public int hashCode() {
        return uuid != null ? uuid.hashCode() : 0;
    }

}
