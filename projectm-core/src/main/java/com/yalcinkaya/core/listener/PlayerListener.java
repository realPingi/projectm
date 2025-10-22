package com.yalcinkaya.core.listener;

import com.yalcinkaya.core.ProjectM;
import com.yalcinkaya.core.util.NametagManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PlayerListener implements Listener {

    @EventHandler
    public void onAsyncChat(AsyncChatEvent event) {
        NametagManager nametagManager = ProjectM.getInstance().getNametagManager();
        Player player = event.getPlayer();
        TextColor color = nametagManager.getNametagColor(player);
        Component displayName = player.displayName().color(color);

        event.renderer((source, sourceDisplayName, chatMessage, viewer) -> {

            Component grayMessage = chatMessage.color(NamedTextColor.GRAY);

            return Component.empty()
                    .append(displayName)
                    .append(Component.text(" >> ", NamedTextColor.GOLD)) // Trennzeichen
                    .append(grayMessage);
        });
    }

}
