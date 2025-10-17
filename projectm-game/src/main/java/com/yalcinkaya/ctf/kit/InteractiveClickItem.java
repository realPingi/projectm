package com.yalcinkaya.ctf.kit;

import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import lombok.Setter;
import org.bukkit.entity.Player;

@Setter
public abstract class InteractiveClickItem extends ClickItem {

    private CTFUser clicked;

    public CTFUser getClickedUser() {
        return clicked;
    }

    public Player getClickedPlayer() {
        return CTFUtil.getPlayer(clicked);
    }

}
