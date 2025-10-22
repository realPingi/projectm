package com.yalcinkaya.ctf.kit;

import com.yalcinkaya.core.util.CoreUtil;
import com.yalcinkaya.core.util.MathUtil;
import com.yalcinkaya.core.util.MessageType;
import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;

public interface EnergyConsumer {

    default boolean tryConsume(CTFUser user) {
        if (!hasEnergy(user)) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "You are missing ", "" + getMissingEnergy(user), " energy."));
            return false;
        } else {
            consume(user);
            return true;
        }
    }

    default boolean checkEnergy(CTFUser user) {
        if (!hasEnergy(user)) {
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "You are missing ", "" + getMissingEnergy(user), " energy."));
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
