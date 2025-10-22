package com.yalcinkaya.ctf.stages;

import com.yalcinkaya.core.util.BungeeUtil;
import com.yalcinkaya.ctf.CTF;
import com.yalcinkaya.ctf.listener.PostStageListener;
import com.yalcinkaya.ctf.net.MatchEndNotifier;
import com.yalcinkaya.ctf.team.Team;
import com.yalcinkaya.ctf.team.TeamColor;
import com.yalcinkaya.ctf.util.CTFUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;

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
