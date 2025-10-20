package com.yalcinkaya.lobby.hotbar;

import com.yalcinkaya.lobby.user.LobbyUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.function.BiConsumer;

@Data
@AllArgsConstructor
public class InteractiveItem {

    private ItemStack itemStack;
    private BiConsumer<LobbyUser, ClickType> action;
}
