package com.yalcinkaya.ctf.commands;

import com.yalcinkaya.ctf.CTFKit;
import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetKitCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            CTFUser user = CTFUtil.getUser(player.getUniqueId());
            String kitName = args[0];
            CTFKit kit = CTFKit.getFromString(kitName);
            if (kit != null) {
                CTFKit.setKit(user, kit);
                CTFUtil.clearPlayer(player);
                CTFUtil.equipPlayer(player);
                return true;
            }
        }

        return false;
    }

}
