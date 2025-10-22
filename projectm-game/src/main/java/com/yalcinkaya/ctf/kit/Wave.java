package com.yalcinkaya.ctf.kit;

import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import com.yalcinkaya.util.MessageType;

public abstract class Wave extends ClickItem implements EnergyConsumer {

    @Override
    public boolean tryClick(CTFUser activator) {
        if (tryConsume(activator)) {
            if (isAffectAllies()) {
                activator.getTeam().getMembers().forEach(this::affect);
            }
            if (isAffectEnemies()) {
                CTFUtil.getEnemyTeam(activator).getMembers().forEach(this::affect);
            }
            CTFUtil.broadcastMessageForAll(MessageType.BROADCAST, CTFUtil.getColoredName(activator), " summoned a ", MessageType.INFO.getFormat() + getName(), ".");
            return true;
        }
        return false;
    }

    public abstract void affect(CTFUser user);

    public abstract boolean isAffectAllies();

    public abstract boolean isAffectEnemies();

}
