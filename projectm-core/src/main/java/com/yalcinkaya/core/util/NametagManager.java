package com.yalcinkaya.core.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class NametagManager {

    private final Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

    public void setPlayerNametag(Player player, String teamName, NamedTextColor color, Component prefix, Component suffix) {
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }

        team.color(color);
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);

        team.prefix(prefix);
        team.suffix(suffix);

        Team currentTeam = scoreboard.getEntryTeam(player.getName());
        if (currentTeam != null && !currentTeam.equals(team)) {
            currentTeam.removeEntry(player.getName());
        }

        if (!team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
        }
    }

    public void resetNametag(Player player) {
        Team currentTeam = scoreboard.getEntryTeam(player.getName());
        if (currentTeam != null) {
            currentTeam.removeEntry(player.getName());
        }
    }

    public TextColor getNametagColor(Player player) {
        Team team = scoreboard.getEntryTeam(player.getName());
        if (team != null) {
            return team.color();
        }
        return TextColor.color(255, 255, 255);
    }

}
