package com.yalcinkaya.ctf.commands;

import com.yalcinkaya.core.redis.Rank;
import com.yalcinkaya.core.util.CoreUtil;
import com.yalcinkaya.core.util.MessageType;
import com.yalcinkaya.ctf.CTF;
import com.yalcinkaya.ctf.CTFKit;
import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SetKitCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {

            CTFUser user = CTFUtil.getUser(player.getUniqueId());

            if (!Rank.hasPermissions(user, Rank.MODERATOR)) {
                user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Insufficient permissions."));
                return true;
            }

            if (args.length < 1) {
                player.openInventory(CTF.getInstance().getPlayerListener().getKitInventory());
                return true;
            }

            String kitName = args[0];
            CTFKit kit = CTFKit.getFromString(kitName);
            if (kit != null) {
                CTFKit.setKit(user, kit);
                CTFUtil.clearPlayer(player);
                CTFUtil.equipPlayer(player);
                CTFUtil.updateNametag(user);
                return true;
            }

            List<String> kitNames = Arrays.stream(CTFKit.values()).map(CTFKit::name).toList();
            List<String> kitInfoMessage = new ArrayList<>();
            kitInfoMessage.add("Invalid kit name. Available kits: ");
            kitNames.forEach(name -> {
                kitInfoMessage.add(name.toLowerCase());
                kitInfoMessage.add(", ");
            });
            kitInfoMessage.removeLast();
            user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, kitInfoMessage.toArray(new String[0])));

        }

        return true;
    }

}
