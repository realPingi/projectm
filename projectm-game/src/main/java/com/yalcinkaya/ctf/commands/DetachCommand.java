package com.yalcinkaya.ctf.commands;

import com.yalcinkaya.core.redis.Rank;
import com.yalcinkaya.core.util.CoreUtil;
import com.yalcinkaya.core.util.MessageType;
import com.yalcinkaya.ctf.CTF;
import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import com.yalcinkaya.ctf.util.PlayerCamera;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DetachCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {

            CTFUser user = CTFUtil.getUser(player.getUniqueId());

            if (!Rank.hasPermissions(user, Rank.MODERATOR)) {
                user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Insufficient permissions."));
                return true;
            }

            PlayerCamera camera = CTF.getInstance().getCameras().stream().filter(c -> c.isAttached(player)).findFirst().orElse(null);
            if (camera == null) {
                user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "No camera to detach from."));
                return true;
            }

            camera.detach(player);
            user.sendMessage(CoreUtil.getMessage(MessageType.SUCCESS, "Detached."));
        }

        return true;
    }
}
