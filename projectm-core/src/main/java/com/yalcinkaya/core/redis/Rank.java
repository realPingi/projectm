package com.yalcinkaya.core.redis;

import com.yalcinkaya.core.ProjectM;
import com.yalcinkaya.core.user.User;
import com.yalcinkaya.core.util.CoreUtil;
import com.yalcinkaya.core.util.MessageType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

@Getter
@AllArgsConstructor
public enum Rank {

    OWNER(NamedTextColor.DARK_RED, ChatColor.DARK_RED, 5),
    ADMIN(NamedTextColor.RED, ChatColor.RED, 4),
    MODERATOR(NamedTextColor.DARK_PURPLE, ChatColor.DARK_PURPLE, 3),
    CONTENT(NamedTextColor.AQUA, ChatColor.AQUA, 2),
    DEFAULT(NamedTextColor.GRAY, ChatColor.GRAY, 1);

    private final NamedTextColor color;
    private final ChatColor legacyColor;
    private final int permissionLevel;

    public static boolean hasPermissions(User user, Rank min) {
        return ProjectM.getInstance().getRedisDataService().getRank(user.getUuid().toString()).getPermissionLevel() >= min.permissionLevel;
    }
}
