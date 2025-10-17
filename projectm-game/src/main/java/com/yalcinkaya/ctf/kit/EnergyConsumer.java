package com.yalcinkaya.ctf.kit;

import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import com.yalcinkaya.ctf.util.MathUtil;
import com.yalcinkaya.ctf.util.MessageType;
import org.bukkit.ChatColor;

public interface EnergyConsumer {

    default boolean tryConsume(CTFUser user) {
        if (!hasEnergy(user)) {
            user.sendMessage(CTFUtil.getCTFMessage(MessageType.WARNING, ChatColor.GRAY + "You are missing ", "" + getMissingEnergy(user), ChatColor.GRAY + " energy."));
            return false;
        } else {
            consume(user);
            return true;
        }
    }

    default boolean checkEnergy(CTFUser user) {
        if (!hasEnergy(user)) {
            user.sendMessage(CTFUtil.getCTFMessage(MessageType.WARNING, ChatColor.GRAY + "You are missing ", "" + getMissingEnergy(user), ChatColor.GRAY + " energy."));
            return false;
        }
        return true;
    }

    default void consume(CTFUser user) {
        consume(user, getEnergy());
    }

    default void consume(CTFUser user, double energy) {
        CTFUtil.modifyEnergy(user, -energy);
    }

    default boolean hasEnergy(CTFUser user) {
        return hasEnergy(user, getEnergy());
    }

    default boolean hasEnergy(CTFUser user, double energy) {
        return getMissingEnergy(user, energy) <= 0;
    }

    default double getMissingEnergy(CTFUser user) {
        return getMissingEnergy(user, getEnergy());
    }

    default double getMissingEnergy(CTFUser user, double energy) {
        return energy - MathUtil.roundDouble(user.getEnergy());
    }

    double getEnergy();
}
