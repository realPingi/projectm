package com.yalcinkaya.ctf.commands;

import com.yalcinkaya.core.util.CoreUtil;
import com.yalcinkaya.core.util.MessageType;
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

            if (user.getKit() != null) {
                user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Already selected."));
                return true;
            }

            player.openInventory(CTF.getInstance().getPlayerListener().getKitInventory());
            return true;
        }

        return true;
    }

}
