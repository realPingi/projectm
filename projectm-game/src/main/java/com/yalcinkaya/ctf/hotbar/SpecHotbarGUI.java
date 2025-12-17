package com.yalcinkaya.ctf.hotbar;

import com.yalcinkaya.core.util.CoreUtil;
import com.yalcinkaya.core.util.ItemBuilder;
import com.yalcinkaya.core.util.MessageType;
import com.yalcinkaya.core.util.hotbar.HotbarGUI;
import com.yalcinkaya.core.util.hotbar.InteractiveItem;
import com.yalcinkaya.ctf.CTF;
import com.yalcinkaya.ctf.team.TeamColor;
import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import com.yalcinkaya.ctf.util.PlayerCamera;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

public class SpecHotbarGUI implements HotbarGUI {

    private InteractiveItem toItem(CTFUser user) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(user.getUuid());
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(offline);
        skull.setItemMeta(meta);
        String teamColor = user.getTeam() == null ? "<gray>" : (user.getTeam().getColor() == TeamColor.BLUE ? "<blue>" : "<red>");
        ItemStack head = ItemBuilder.of(skull).name("<italic:false>" + teamColor + CTFUtil.getPlayer(user).getName()).build();

        InteractiveItem item = new InteractiveItem(head, ((spec, clickType) -> {

            Player player = CTFUtil.getPlayer(spec.getUuid());
            Player target = CTFUtil.getPlayer(user.getUuid());
            CTFUser specUser = CTFUtil.getUser(spec.getUuid());

            if (clickType.isRightClick()) {
                CTF.getInstance().getCameras().stream().filter(c -> c.isAttached(player)).findFirst().ifPresent(current -> current.detach(player));

                if (target.getUniqueId().equals(player.getUniqueId())) {
                    specUser.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Can't attach to your own camera."));
                    return;
                }

                PlayerCamera camera = CTF.getInstance().getCameras().stream().filter(c -> c.getObservedId().equals(target.getUniqueId())).findFirst().orElse(null);
                if (camera == null) {
                    specUser.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "No camera to attach to."));
                    return;
                }

                camera.attach(player);
                specUser.sendMessage(CoreUtil.getMessage(MessageType.SUCCESS, "Attached."));
                return;
            }

            specUser.setScoreboardSource(user);
            specUser.sendMessage(CoreUtil.getMessage(MessageType.SUCCESS, "Set ", target.getName(), " as scoreboard source."));

        }));
        return item;
    }

    @Override
    public InteractiveItem[] getItems() {
        InteractiveItem[] items = new InteractiveItem[9];
        List<CTFUser> blue = CTF.getInstance().getBlue().getMembers();
        List<CTFUser> red = CTF.getInstance().getRed().getMembers();
        for (int i = 0; i < blue.size(); i++) {
            items[i] = toItem(blue.get(i));
        }
        for (int i = 0; i < red.size(); i++) {
            items[8 - i] = toItem(red.get(i));
        }
        items[4] = new InteractiveItem(ItemBuilder.of(Material.PAPER).name("<italic:false><light_purple>Detach<light_purple>").build(), (spec, clickType) -> {
            Player player = CTFUtil.getPlayer(spec.getUuid());
            CTF.getInstance().getCameras().stream().filter(c -> c.isAttached(player)).findFirst().ifPresent(current -> current.detach(player));
            CTFUtil.getUser(player).setScoreboardSource(null);
            spec.sendMessage(CoreUtil.getMessage(MessageType.SUCCESS, "Detached."));
        });
        return items;
    }
}
