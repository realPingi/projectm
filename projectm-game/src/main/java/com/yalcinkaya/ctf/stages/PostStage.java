package com.yalcinkaya.ctf.stages;

import com.yalcinkaya.core.util.BungeeUtil;
import com.yalcinkaya.core.util.MathUtil;
import com.yalcinkaya.ctf.CTF;
import com.yalcinkaya.ctf.flag.Flag;
import com.yalcinkaya.ctf.listener.PostStageListener;
import com.yalcinkaya.ctf.net.MatchEndNotifier;
import com.yalcinkaya.ctf.team.Team;
import com.yalcinkaya.ctf.team.TeamColor;
import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import fr.mrmicky.fastboard.FastBoard;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PostStage extends CTFStage<PostStageListener> {

    private final int shutdown = 10;
    private int matchDuration;
    private Team winner;
    private Team loser;

    public PostStage(Team winner, Team loser, int matchDuration) {
        super(new PostStageListener());
        this.matchDuration = matchDuration;
        this.winner = winner;
        this.loser = loser;
    }

    @Override
    public void idle() {
        CTF ctf = CTF.getInstance();

        for (Player player : Bukkit.getOnlinePlayers()) {

            CTFUser ctfUser = CTFUtil.getUser(player);
            CTFUser source = ctfUser.getScoreboardSource() == null ? ctfUser : ctfUser.getScoreboardSource();

            String kitName = source.getKit() == null ? "None" : source.getKit().getName();
            List<Flag> blueFlags = ctf.getMap().getFlags().stream().filter(flag -> flag.getTeam() == TeamColor.BLUE).toList();
            List<Flag> redFlags = ctf.getMap().getFlags().stream().filter(flag -> flag.getTeam() == TeamColor.RED).toList();

            FastBoard board = ctfUser.getScoreboard();
            board.updateTitle(ChatColor.GOLD + "" + ChatColor.BOLD + "ProjectM");

            List<String> lines = new ArrayList<>();
            Collections.addAll(lines, "",
                    ChatColor.GRAY + "Game Time: " + ChatColor.GOLD + MathUtil.getReadableSeconds(matchDuration),
                    ChatColor.GRAY + "Kit: " + ChatColor.GOLD + kitName,
                    ChatColor.GRAY + "Energy: " + ChatColor.GOLD + MathUtil.roundDouble(source.getEnergy()),
                    " ",
                    ChatColor.BLUE + "Blue:");
            redFlags.forEach(flag -> lines.add(CTFUtil.printFlagStatus(flag) + " " + CTFUtil.printFlagPosition(flag)));
            lines.add("  ");
            lines.add(ChatColor.RED + "Red:");
            blueFlags.forEach(flag -> lines.add(CTFUtil.printFlagStatus(flag) + " " + CTFUtil.printFlagPosition(flag)));

            board.updateLines(lines);
        }
    }

    @Override
    public void start() {
        super.start();

        setTimer(shutdown * 20);
        setCountdown(true);

        CTFUtil.calcEloChanges(winner, loser);

        Bukkit.getOnlinePlayers().forEach(player -> Audience.audience(player)
                .showTitle(Title.title(MiniMessage.miniMessage().deserialize(printWinMessage()),
                        MiniMessage.miniMessage().deserialize(""),
                        Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(shutdown - 2), Duration.ofSeconds(1)))));

        Bukkit.getScheduler().runTaskLater(CTF.getInstance(), () -> {

            for (Player player : Bukkit.getOnlinePlayers()) {
                BungeeUtil.sendPlayerToLobby(CTF.getInstance(), player);
            }

            Bukkit.getScheduler().runTaskAsynchronously(CTF.getInstance(), () -> {
                new MatchEndNotifier().closeMatch(CTF.getInstance().getMatchId());
                Bukkit.getScheduler().runTaskLater(CTF.getInstance(), Bukkit::shutdown, 20);
            });

        }, 20 * shutdown);
    }

    private String printWinMessage() {
        int blueFlags = CTFUtil.getCapturedFlags(CTF.getInstance().getRed()).size();
        int redFlags = CTFUtil.getCapturedFlags(CTF.getInstance().getBlue()).size();
        String bluePrefix;
        String redPrefix;
        if (winner.getColor() == TeamColor.BLUE) {
            bluePrefix = "<green>";
            redPrefix = "<gray>";
        } else {
            redPrefix = "<green>";
            bluePrefix = "<gray>";
        }
        return "<blue>" + "Blue" + bluePrefix + " " + blueFlags + "<gray>" + " - " + redPrefix + redFlags + " " + "<red>" + "Red";
    }
}
