package com.yalcinkaya.user;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.UUID;

@Data
@NoArgsConstructor
public class User {

    private UUID uuid;

    public User(UUID uuid) {
        this.uuid = uuid;
    }

    public void sendMessage(String message) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            player.sendMessage(message);
        }
    }

    public void playSound(Sound sound) {
        Player player = Bukkit.getPlayer(getUuid());
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
