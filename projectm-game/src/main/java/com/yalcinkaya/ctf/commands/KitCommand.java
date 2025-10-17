package com.yalcinkaya.ctf.commands;

import com.yalcinkaya.ctf.CTF;
import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class KitCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            CTFUser user = CTFUtil.getUser(player);

            if (!player.isOp() && user.getKit() != null) {
                return false;
            }

            player.openInventory(CTF.getInstance().getPlayerListener().getKitInventory());
            return true;
        }

        return false;
    }

}
