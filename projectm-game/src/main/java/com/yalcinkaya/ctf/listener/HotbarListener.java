package com.yalcinkaya.ctf.listener;

import com.yalcinkaya.ctf.CTF;
import com.yalcinkaya.ctf.hotbar.HotbarManager;
import com.yalcinkaya.ctf.util.CTFUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

public class HotbarListener implements Listener {

    private final Set<Action> acceptedActions = Set.of(Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK, Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK);

    @EventHandler
    public void onHotbarInteract(PlayerInteractEvent event) {
        HotbarManager hotbarManager = CTF.getInstance().getHotbarManager();
        ItemStack item = event.getItem();
        Action action = event.getAction();
        if (item != null && acceptedActions.contains(action) && hotbarManager.isHotbarGUIItem(item)) {
            hotbarManager.accept(CTFUtil.getUser(event.getPlayer()), fromAction(action), item);
            event.setCancelled(true);
        }
    }

    private ClickType fromAction(Action action) {
        if (action.isLeftClick()) {
            return ClickType.LEFT;
        } else if (action.isRightClick()) {
            return ClickType.RIGHT;
        } else {
            return null;
        }
    }

}
