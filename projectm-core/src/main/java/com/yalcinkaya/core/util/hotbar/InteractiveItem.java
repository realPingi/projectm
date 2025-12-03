package com.yalcinkaya.core.util.hotbar;

import com.yalcinkaya.core.user.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.function.BiConsumer;

@Data
@AllArgsConstructor
public class InteractiveItem {

    private ItemStack itemStack;
    private BiConsumer<User, ClickType> action;
}
