package com.yalcinkaya.ctf.kits.clickItems;

import com.yalcinkaya.ctf.kit.AffectType;
import com.yalcinkaya.ctf.kit.Beam;
import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import com.yalcinkaya.ctf.util.PotentialObject;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

public class Extract extends Beam {

    private static final Set<PotentialObject<Color>> colors = CTFUtil.mixColors(Color.fromRGB(222, 145, 255), Color.fromRGB(245, 222, 255), Color.fromRGB(63, 0, 74), 50, 50, 20);
    private final ItemStack item = CTFUtil.createIcon("Extract", Material.PINK_GLAZED_TERRACOTTA);

    @Override
    public boolean tryAffect(CTFUser shooter, CTFUser hit) {
        Player hitPlayer = CTFUtil.getPlayer(hit);
        Player shooterPlayer = CTFUtil.getPlayer(shooter);
        hitPlayer.setMaxHealth(30);
        shooterPlayer.setHealth(shooterPlayer.getMaxHealth());
        hit.playSoundForWorld(Sound.BLOCK_AMETHYST_BLOCK_PLACE);
        return true;
    }

    @Override
    public Set<PotentialObject<Color>> getColors() {
        return colors;
    }

    @Override
    public int getLength() {
        return 30;
    }

    @Override
    public AffectType getAffectType() {
        return AffectType.HOSTILE;
    }

    @Override
    public double getRadius() {
        return 2;
    }

    @Override
    public String getName() {
        return "Extract";
    }

    @Override
    public ItemStack getItem() {
        return item;
    }

    @Override
    public int getCooldownSeconds() {
        return 60;
    }

    @Override
    public Sound getSound() {
        return Sound.BLOCK_AMETHYST_BLOCK_BREAK;
    }

    @Override
    public double getEnergy() {
        return 70;
    }
}
