package com.yalcinkaya.lobby.util;

import org.bukkit.ChatColor;

public enum MessageType {

    WARNING(ChatColor.RED + "" + ChatColor.BOLD + "Warning >> ", ChatColor.RED + ""),
    SUCCESS(ChatColor.GREEN + "" + ChatColor.BOLD + "Success >> ", ChatColor.GREEN + ""),
    INFO(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Info >> ", ChatColor.LIGHT_PURPLE + ""),
    BROADCAST(ChatColor.GOLD + "" + ChatColor.BOLD + "ProjectM >> ", ChatColor.GRAY + "");

    private String prefix;
    private String format;

    MessageType(String prefix, String format) {
        this.format = format;
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getFormat() {
        return format;
    }
}
