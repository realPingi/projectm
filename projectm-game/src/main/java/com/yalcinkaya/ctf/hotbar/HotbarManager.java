package com.yalcinkaya.ctf.hotbar;

import com.yalcinkaya.core.user.User;
import com.yalcinkaya.core.util.hotbar.HotbarGUI;
import com.yalcinkaya.core.util.hotbar.InteractiveItem;
import lombok.Getter;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

@Getter
public class HotbarManager {

    private final SpecHotbarGUI specHotbarGUI = new SpecHotbarGUI();
    private final Set<HotbarGUI> hotbarGUIs = Set.of(specHotbarGUI);

    public boolean isHotbarGUIItem(ItemStack itemStack) {
        return hotbarGUIs.stream().anyMatch(hotbarGUI -> hotbarGUI.contains(itemStack));
    }

    public void accept(User user, ClickType clickType, ItemStack itemStack) {
        HotbarGUI hotbarGUI = hotbarGUIs.stream().filter(gui -> gui.contains(itemStack)).findFirst().orElse(null);
        if (hotbarGUI != null) {
            InteractiveItem interactiveItem = hotbarGUI.getInteractiveItem(itemStack);
            interactiveItem.getAction().accept(user, clickType);
        }
    }
}
