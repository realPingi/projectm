package com.yalcinkaya.ctf.stages;

import com.yalcinkaya.ctf.CTF;
import com.yalcinkaya.ctf.listener.PostStageListener;
import com.yalcinkaya.ctf.net.MatchEndNotifier;
import com.yalcinkaya.ctf.team.Team;
import com.yalcinkaya.ctf.team.TeamColor;
import com.yalcinkaya.ctf.util.BungeeUtil;
import com.yalcinkaya.ctf.util.CTFUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class PostStage extends CTFStage<PostStageListener> {

    private final int shutdown = 10;
    private Team winner;

    public PostStage(Team winner) {
        super(new PostStageListener());
        this.winner = winner;
    }

    @Override
    public void start() {
        super.start();
        setTimer(shutdown);
        setCountdown(true);
        Bukkit.getOnlinePlayers().forEach(player -> player.sendTitle(printWinMessage(), "", 10, 20 * (shutdown - 2), 10));

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
        ChatColor bluePrefix;
        ChatColor redPrefix;
        if (winner.getColor() == TeamColor.BLUE) {
            bluePrefix = ChatColor.GREEN;
            redPrefix = ChatColor.GRAY;
        } else {
            redPrefix = ChatColor.GREEN;
            bluePrefix = ChatColor.GRAY;
        }
        return ChatColor.BLUE + "Blue" + bluePrefix + " " + blueFlags + ChatColor.GRAY + " - " + redPrefix + redFlags + " " + ChatColor.RED + "Red";
    }
}
