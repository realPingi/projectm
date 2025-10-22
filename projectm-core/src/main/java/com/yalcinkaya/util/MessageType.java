package com.yalcinkaya.util;

import org.bukkit.ChatColor;

public enum MessageType {

    WARNING(ChatColor.RED + "Warning >> ", ChatColor.RED + ""),
    SUCCESS(ChatColor.GREEN + "Success >> ", ChatColor.GREEN + ""),
    INFO(ChatColor.LIGHT_PURPLE + "Info >> ", ChatColor.LIGHT_PURPLE + ""),
    BROADCAST(ChatColor.GOLD + "ProjectM >> ", ChatColor.GRAY + "");

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
