package com.yalcinkaya.ctf.commands;

import com.yalcinkaya.core.redis.Rank;
import com.yalcinkaya.core.util.CoreUtil;
import com.yalcinkaya.core.util.MessageType;
import com.yalcinkaya.ctf.CTF;
import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import com.yalcinkaya.ctf.util.PlayerCamera;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AttachCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {

            CTFUser user = CTFUtil.getUser(player.getUniqueId());

            if (!Rank.hasPermissions(user, Rank.MODERATOR)) {
                user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Insufficient permissions."));
                return true;
            }

            if (args.length < 1) {
                user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Usage: /attach playerName"));
                return true;
            }

            // detach from current camera
            CTF.getInstance().getCameras().stream().filter(c -> c.isAttached(player)).findFirst().ifPresent(current -> current.detach(player));

            Player target = Bukkit.getOnlinePlayers().stream().filter(p -> p.getName().equalsIgnoreCase(args[0])).findFirst().orElse(null);
            if (target == null) {
                user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "No such player."));
                return true;
            }

            if (target.getUniqueId().equals(player.getUniqueId())) {
                user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Can't attach to your own camera."));
                return true;
            }

            PlayerCamera camera = CTF.getInstance().getCameras().stream().filter(c -> c.getObservedId().equals(target.getUniqueId())).findFirst().orElse(null);
            if (camera == null) {
                user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "No camera to attach to."));
                return true;
            }

            camera.attach(player);
            user.sendMessage(CoreUtil.getMessage(MessageType.SUCCESS, "Attached."));
        }

        return true;
    }
}
