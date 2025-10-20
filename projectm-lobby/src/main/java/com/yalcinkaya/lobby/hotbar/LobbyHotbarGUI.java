package com.yalcinkaya.lobby.hotbar;

import com.yalcinkaya.lobby.Lobby;
import com.yalcinkaya.lobby.queue.Queue;
import com.yalcinkaya.lobby.util.ItemBuilder;
import com.yalcinkaya.lobby.util.LobbyUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class LobbyHotbarGUI implements HotbarGUI {

    private final InteractiveItem[] items = new InteractiveItem[9];

    private final InteractiveItem kangaItem = new InteractiveItem(ItemBuilder.of(Material.FIREWORK_ROCKET).name("<italic:false><light_purple>Kanga<light_purple>").build(), (user, clickType) -> {
        Player player = LobbyUtil.getPlayer(user);
        if (clickType.isRightClick()) {
            player.setVelocity(new Vector(0, 1, 0));
        } else if (clickType.isLeftClick()) {
            Vector boost = player.getEyeLocation().getDirection().normalize().multiply(2).setY(0.5);
            player.setVelocity(boost);
        }
    });

    public LobbyHotbarGUI() {
        items[4] = kangaItem;
    }

    @Override
    public InteractiveItem[] getItems() {
        return items;
    }

}
