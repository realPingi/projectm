package com.yalcinkaya.ctf.commands;

import com.yalcinkaya.core.redis.Rank;
import com.yalcinkaya.core.util.CoreUtil;
import com.yalcinkaya.core.util.MessageType;
import com.yalcinkaya.ctf.CTF;
import com.yalcinkaya.ctf.listener.CaptureStageListener;
import com.yalcinkaya.ctf.stages.CTFStage;
import com.yalcinkaya.ctf.stages.CaptureStage;
import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ToggleSpecCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {

            CTFUser user = CTFUtil.getUser(player.getUniqueId());

            if (!Rank.hasPermissions(user, Rank.MODERATOR)) {
                user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Insufficient permissions."));
                return true;
            }

            CTFStage currentStage = CTF.getInstance().getCurrentStage();

            if (!(currentStage instanceof CaptureStage)) {
                return true;
            }

            if (user.isSpectating()) {
                user.setSpectating(false);
                CTFUtil.setTeam(user, true);
                CaptureStageListener captureStageListener = ((CaptureStage) currentStage).getStageListener();
                captureStageListener.setupPlayer(player);
            } else {
                user.setSpectating(true);
                CTFUtil.setupSpectator(user);
            }
            return true;
        }

        return true;
    }

}
