package com.yalcinkaya.ctf.commands;

import com.yalcinkaya.core.redis.Rank;
import com.yalcinkaya.core.util.CoreUtil;
import com.yalcinkaya.core.util.MessageType;
import com.yalcinkaya.ctf.team.TeamColor;
import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SwapTeamCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {

            CTFUser user = CTFUtil.getUser(player.getUniqueId());

            if (!Rank.hasPermissions(user, Rank.MODERATOR)) {
                user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Insufficient permissions."));
                return true;
            }

            if (user.getTeam() == null) {
                return true;
            }

            CTFUtil.setTeam(user, user.getTeam().getColor() != TeamColor.BLUE);
        }

        return true;
    }

}
