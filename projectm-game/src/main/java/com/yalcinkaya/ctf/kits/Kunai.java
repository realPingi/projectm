package com.yalcinkaya.ctf.kits;

import com.yalcinkaya.core.util.CoreUtil;
import com.yalcinkaya.core.util.MessageType;
import com.yalcinkaya.ctf.CTF;
import com.yalcinkaya.ctf.kit.ClickItem;
import com.yalcinkaya.ctf.kit.ClickKit;
import com.yalcinkaya.ctf.kit.Kit;
import com.yalcinkaya.ctf.kits.clickItems.Knife;
import com.yalcinkaya.ctf.util.CTFUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class Kunai extends Kit implements ClickKit {

    private final Knife knife = new Knife();
    private Player marked;

    @Override
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) {
            Player player = event.getPlayer();

            if (marked == null) {
                CTFUtil.getUser(player).sendMessage(CoreUtil.getMessage(MessageType.WARNING, "No player marked."));
                return;
            }

            float yaw = player.getLocation().getYaw();
            float pitch = player.getLocation().getPitch();
            Location location = marked.getLocation().clone();
            location.setYaw(yaw);
            location.setPitch(pitch);
            player.teleport(location);
        }
    }

    @Override
    public void onProjectileHitPlayer(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Arrow arrow && arrow.hasMetadata(knife.getId().toString())) {
            marked = (Player) event.getHitEntity();
            CTFUtil.getUser((Player) event.getEntity().getShooter()).sendMessage(CoreUtil.getMessage(MessageType.SUCCESS, "Player marked for", " 3 ", "seconds."));
            Bukkit.getScheduler().runTaskLater(CTF.getInstance(), () -> marked = null, 3 *20);
        }
    }

    @Override
    public ClickItem[] getClickItems() {
        return new ClickItem[]{knife};
    }

    @Override
    public String getName() {
        return "Kunai";
    }

}
