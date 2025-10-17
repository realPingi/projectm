package com.yalcinkaya.ctf.commands;

import com.yalcinkaya.ctf.kit.Counter;
import com.yalcinkaya.ctf.kit.Kit;
import com.yalcinkaya.ctf.kit.MultiCounter;
import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetCounterCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            CTFUser user = CTFUtil.getUser(player.getUniqueId());
            Kit kit = user.getKit();
            if (kit instanceof MultiCounter) {
                String name = args[0];
                int count = Integer.parseInt(args[1]);
                Counter counter = ((MultiCounter) kit).getCounter(name);
                if (counter != null) {
                    counter.setCount(count);
                    return true;
                }
            }
        }
        return false;
    }
}
