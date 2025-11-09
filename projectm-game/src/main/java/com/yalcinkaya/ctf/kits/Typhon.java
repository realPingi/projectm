package com.yalcinkaya.ctf.kits;

import com.yalcinkaya.core.util.ItemBuilder;
import com.yalcinkaya.core.util.MessageType;
import com.yalcinkaya.core.util.SmartRunnable;
import com.yalcinkaya.core.util.SmoothTravel;
import com.yalcinkaya.core.util.parametrization.builder.Illustrations;
import com.yalcinkaya.core.util.parametrization.builder.MultiColorParticle;
import com.yalcinkaya.core.util.parametrization.builder.SurfaceBuilder;
import com.yalcinkaya.core.util.parametrization.domains.Area;
import com.yalcinkaya.core.util.parametrization.functions.Surface;
import com.yalcinkaya.core.util.parametrization.types.SurfaceTypes;
import com.yalcinkaya.ctf.CTF;
import com.yalcinkaya.ctf.kit.Cooldown;
import com.yalcinkaya.ctf.kit.EnergyConsumer;
import com.yalcinkaya.ctf.kit.Kit;
import com.yalcinkaya.ctf.kit.MultiCooldown;
import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;


public class Typhon extends Kit implements EnergyConsumer, MultiCooldown {

    private final ItemStack typhonBow = ItemBuilder.of(CTFUtil.createIcon("Levitate", Material.BOW)).enchant(Enchantment.INFINITY, 1, true).unbreakable(true).build();
    private final ItemStack typhonArrow = CTFUtil.createIcon("Typhon's Arrow", Material.ARROW);
    private final int duration = 8;
    private final int radius = 3;
    private final Cooldown cooldown = new Cooldown("Storm", 60, this);

    private Set<SmoothTravel> travels = new HashSet<>();

    @Override
    public String getName() {
        return "Typhon";
    }

    @Override
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof Arrow arrow) {
            Player shooter = (Player) arrow.getShooter();
            CTFUser user = CTFUtil.getUser(shooter.getUniqueId());
            if (shooter.getItemInHand().equals(typhonBow)) {
                if (cooldown.isActive()) {
                    cooldown.sendWarning(shooter);
                    event.setCancelled(true);
                    return;
                }
                if (tryConsume(user)) {
                    arrow.setMetadata("typhonData", new FixedMetadataValue(CTF.getInstance(), true));
                    cooldown.use();
                } else {
                    event.setCancelled(true);
                }
            }
        }
    }

    @Override
    public void onProjectileHitBlock(ProjectileHitEvent event) {
        if (event.getEntity() instanceof Arrow arrow) {
            Block hit = event.getHitBlock();
            Player shooter = (Player) arrow.getShooter();
            CTFUser shooterUser = CTFUtil.getUser(shooter.getUniqueId());
            if (arrow.hasMetadata("typhonData")) {
                Surface cylinder = new SurfaceBuilder(new Area(0, 2 * Math.PI, 0, duration * 3), SurfaceTypes.CYLINDER_1).scaleX(radius).scaleZ(radius).translate(hit.getLocation().toVector()).build();
                Illustrations.display(cylinder, 512, duration * 20, new MultiColorParticle(Color.fromRGB(54, 179, 145), 1, 0), hit.getWorld());
                CTFUtil.broadcastMessageForAll(MessageType.BROADCAST, "", CTFUtil.getColoredName(shooterUser), " is manipulating gravity.");
                new SmartRunnable() {
                    @Override
                    public void cycle() {
                        CTFUtil.getNearbyUsers(hit.getLocation(), radius).forEach(user -> {
                            if (!isLevitating(user)) {
                                SmoothTravel travel = new SmoothTravel(CTFUtil.getPlayer(user), hit.getLocation().add(new Vector(0, duration * 3, 0)), duration, false);
                                travel.start();
                                travels.add(travel);
                            }
                        });
                    }
                }.createTimer(1, duration * 20);

                Bukkit.getScheduler().runTaskLater(CTF.getInstance(), () -> {
                    travels.forEach(SmoothTravel::cancel);
                    travels.clear();
                }, duration * 20);
            }
        }
    }

    private boolean isLevitating(CTFUser user) {
        return travels.stream().map(SmoothTravel::getTraveller).map(Player::getUniqueId).anyMatch(uuid -> uuid.equals(user.getUuid()));
    }

    @Override
    public double getEnergy() {
        return 80;
    }

    @Override
    public ItemStack[] getStartItems() {
        return new ItemStack[]{typhonBow, typhonArrow};
    }

    @Override
    public Cooldown[] getCooldowns() {
        return new Cooldown[]{cooldown};
    }
}
