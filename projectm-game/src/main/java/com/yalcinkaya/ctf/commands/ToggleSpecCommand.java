package com.yalcinkaya.ctf.commands;

import com.yalcinkaya.core.redis.Rank;
import com.yalcinkaya.core.util.CoreUtil;
import com.yalcinkaya.core.util.MessageType;
import com.yalcinkaya.ctf.CTF;
import com.yalcinkaya.ctf.listener.CaptureStageListener;
import com.yalcinkaya.ctf.stages.CTFStage;
import com.yalcinkaya.ctf.stages.CaptureStage;
import com.yalcinkaya.ctf.team.TeamColor;
import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

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

                if (args.length < 1) {
                    user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Usage: /togglespec <team>"));
                    return true;
                }

                String colorName = args[0].toUpperCase(Locale.ROOT);

                try {
                    TeamColor.valueOf(colorName);
                } catch (IllegalArgumentException e) {
                    user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "No such team."));
                    return true;
                }

                user.setSpectating(false);
                CTFUtil.setTeam(user, TeamColor.valueOf(colorName) == TeamColor.BLUE ? CTF.getInstance().getBlue() : CTF.getInstance().getRed());
                CaptureStageListener captureStageListener = ((CaptureStage) currentStage).getStageListener();
                captureStageListener.setupPlayer(player);
            } else {
                CTFUtil.removeTeam(user);
                CTFUtil.setupSpectator(user);
                CTFUtil.updateNametag(user);
                user.setSpectating(true);
            }
            return true;
        }

        return true;
    }

}
