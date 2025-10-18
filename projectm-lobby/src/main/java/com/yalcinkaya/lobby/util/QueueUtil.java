package com.yalcinkaya.lobby.util;

import com.yalcinkaya.lobby.menu.Menu;
import com.yalcinkaya.lobby.queue.Queue;
import com.yalcinkaya.lobby.queue.Queueable;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class QueueUtil {

    public static <T extends Queueable> void addQueueItem(Menu menu, int slot, Queue<T> queue) {
        menu.setItem(slot, queue.getIcon(), queue::accept);
    }

    public static ItemStack buildIcon(Queue queue) {
        return ItemBuilder.of(queue.getDisplayMaterial())
                .name("<italic:false><light_purple>" + queue.getName() + "<light_purple>")
                .amount(queue.getIngame() + queue.getQueuedPlayers())
                .lore(buildDescription(queue))
                .build();
    }

    public static List<String> buildDescription(Queue queue) {
        final List<String> desc = new ArrayList<>();

        int queuing = queue.getQueuedPlayers();
        int ingame = queue.getIngame();

        desc.add("");
        desc.add("<italic:false><gray>Queuing" + " >> <gray><light_purple>" + queuing + "<light_purple>");
        desc.add("");
        desc.add("<italic:false><light_purple>Left<light_purple> <gray>click to queue<gray>");
        desc.add("<italic:false><light_purple>Shift<light_purple> <gray>click to leave queue<gray>");
        return desc;
    }

}
