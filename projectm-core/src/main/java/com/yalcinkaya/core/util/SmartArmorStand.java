package com.yalcinkaya.core.util;

import com.yalcinkaya.core.ProjectM;
import org.bukkit.Bukkit;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class SmartArmorStand {

    private ArmorStand armorStand;
    private LivingEntity entity;
    private ItemStack disguise;
    private int taskId;

    public SmartArmorStand(LivingEntity entity, ItemStack disguise) {
        this.entity = entity;
        this.disguise = disguise;
    }

    public void follow() {
        armorStand = (ArmorStand) entity.getWorld().spawnEntity(entity.getLocation(), EntityType.ARMOR_STAND);
        armorStand.setInvisible(true);
        armorStand.setInvulnerable(true);
        armorStand.setGravity(false);
        armorStand.setHelmet(disguise);
        taskId = new BukkitRunnable() {

            @Override
            public void run() {
                armorStand.teleport(entity.getLocation().add(0, 2.5, 0));
                armorStand.setRotation(entity.getEyeLocation().getYaw(), entity.getEyeLocation().getPitch());
            }

        }.runTaskTimer(ProjectM.getInstance(), 0, 1).getTaskId();
    }

    public void cancel() {
        Bukkit.getScheduler().cancelTask(taskId);
        armorStand.remove();
    }

    public ArmorStand getArmorStand() {
        return armorStand;
    }

    public Entity getEntity() {
        return entity;
    }
}
