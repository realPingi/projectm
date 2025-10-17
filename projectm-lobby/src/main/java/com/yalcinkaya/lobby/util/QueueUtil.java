package com.yalcinkaya.lobby.util;

import com.yalcinkaya.lobby.menu.Menu;
import com.yalcinkaya.lobby.queue.Queue;
import com.yalcinkaya.lobby.queue.Queueable;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class QueueUtil {

    public static final ChatColor QUEUE_COLOR = ChatColor.LIGHT_PURPLE;

    public static <T extends Queueable> void addQueueItem(Menu menu, int slot, Queue<T> queue) {
        menu.setItem(slot, queue.getIcon(), queue::accept);
    }

    public static ItemStack buildIcon(Queue queue) {
        return new ItemBuilder(queue.getDisplayMaterial())
                .name(QUEUE_COLOR + queue.getName())
                .amount(queue.getIngame() + queue.getQueuedPlayers())
                //.lore(buildDescription(queue))
                .build();
    }

    public static List<String> buildDescription(Queue queue) {
        final List<String> desc = new ArrayList<>();

        int queuing = queue.getQueuedPlayers();
        int ingame = queue.getIngame();

        desc.add("");
        desc.add(ChatColor.GRAY + "Ingame" + " >> " + ChatColor.AQUA + ingame);
        desc.add(ChatColor.GRAY + "Queuing" + " >> " + ChatColor.AQUA + queuing);
        desc.add("");
        desc.add(ChatColor.WHITE + "Left" + ChatColor.GRAY + " click to queue");
        desc.add(ChatColor.WHITE + "Shift" + ChatColor.GRAY + " click to leave queue");
        return desc;
    }

}
