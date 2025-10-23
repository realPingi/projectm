package com.yalcinkaya.lobby.hotbar;

import com.yalcinkaya.core.util.ItemBuilder;
import com.yalcinkaya.lobby.util.LobbyUtil;
import com.yalcinkaya.lobby.util.Place;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class LobbyHotbarGUI implements HotbarGUI {

    private final InteractiveItem[] items = new InteractiveItem[9];

    private final InteractiveItem kangaItem = new InteractiveItem(ItemBuilder.of(Material.FIREWORK_ROCKET).name("<italic:false><light_purple>Kanga<light_purple>").build(), (user, clickType) -> {
        Player player = LobbyUtil.getPlayer(user);
        if (clickType.isRightClick() && player.isOnGround()) {
            player.setVelocity(new Vector(0, 1, 0));
        } else if (clickType.isLeftClick()) {
            Vector boost = player.getEyeLocation().getDirection().normalize().multiply(2).setY(0.5);
            player.setVelocity(boost);
        }
    });

    private final InteractiveItem queuesItem = new InteractiveItem(ItemBuilder.of(Material.FLOW_BANNER_PATTERN).name("<italic:false><light_purple>Queues<light_purple>").build(), ((user, clickType) -> LobbyUtil.getPlayer(user).teleport(Place.QUEUES.getLocation())));
    private final InteractiveItem leaderboardsItem = new InteractiveItem(ItemBuilder.of(Material.DARK_OAK_HANGING_SIGN).name("<italic:false><light_purple>Leaderboards<light_purple>").build(), ((user, clickType) -> LobbyUtil.getPlayer(user).teleport(Place.LEADERBOARDS.getLocation())));

    public LobbyHotbarGUI() {
        items[3] = queuesItem;
        items[4] = kangaItem;
        items[5] = leaderboardsItem;
    }

    @Override
    public InteractiveItem[] getItems() {
        return items;
    }

}