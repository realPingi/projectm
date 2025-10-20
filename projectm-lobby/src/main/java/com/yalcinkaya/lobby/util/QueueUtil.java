package com.yalcinkaya.lobby.util;

import com.yalcinkaya.lobby.queue.Queue;
import com.yalcinkaya.lobby.queue.Queueable;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class QueueUtil {

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

        desc.add("");
        desc.add("<italic:false><gray>Queuing" + " >> <gray><light_purple>" + queuing + "<light_purple>");
        desc.add("");
        desc.add("<italic:false><light_purple>Right<light_purple> <gray>click to queue<gray>");
        desc.add("<italic:false><light_purple>Left<light_purple> <gray>click to unqueue<gray>");
        return desc;
    }

}
