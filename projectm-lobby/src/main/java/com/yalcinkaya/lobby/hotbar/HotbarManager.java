package com.yalcinkaya.lobby.hotbar;

import com.yalcinkaya.lobby.user.LobbyUser;
import lombok.Getter;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

@Getter
public class HotbarManager {

    private final LobbyHotbarGUI lobbyHotbarGUI = new LobbyHotbarGUI();
    private final Set<HotbarGUI> hotbarGUIs = Set.of(lobbyHotbarGUI);

    public boolean isHotbarGUIItem(ItemStack itemStack) {
        return hotbarGUIs.stream().anyMatch(hotbarGUI -> hotbarGUI.contains(itemStack));
    }

    public void accept(LobbyUser user, ClickType clickType, ItemStack itemStack) {
        HotbarGUI hotbarGUI = hotbarGUIs.stream().filter(gui -> gui.contains(itemStack)).findFirst().orElse(null);
        if (hotbarGUI != null) {
            InteractiveItem interactiveItem = hotbarGUI.getInteractiveItem(itemStack);
            interactiveItem.getAction().accept(user, clickType);
        }
    }
}
