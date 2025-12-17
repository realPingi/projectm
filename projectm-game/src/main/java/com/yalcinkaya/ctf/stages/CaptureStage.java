package com.yalcinkaya.ctf.stages;

import com.yalcinkaya.core.util.MathUtil;
import com.yalcinkaya.ctf.CTF;
import com.yalcinkaya.ctf.flag.CaptureStatus;
import com.yalcinkaya.ctf.flag.Flag;
import com.yalcinkaya.ctf.kit.*;
import com.yalcinkaya.ctf.listener.CaptureStageListener;
import com.yalcinkaya.ctf.team.TeamColor;
import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import fr.mrmicky.fastboard.FastBoard;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CaptureStage extends CTFStage<CaptureStageListener> {

    public CaptureStage() {
        super(new CaptureStageListener());
    }

    @Override
    public void start() {
        super.start();
        for (Player player : Bukkit.getOnlinePlayers()) {
            CTFUser user = CTFUtil.getUser(player);
            if (user.getTeam() != null) {
                stageListener.setupPlayer(player);
            } else {
                user.setSpectating(true);
                CTFUtil.setupSpectator(user);
            }
        }
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
                    ChatColor.GRAY + "Game Time: " + ChatColor.GOLD + MathUtil.getReadableSeconds(getTime()),
                    ChatColor.GRAY + "Kit: " + ChatColor.GOLD + kitName,
                    ChatColor.GRAY + "Energy: " + ChatColor.GOLD + MathUtil.roundDouble(source.getEnergy()),
                    " ",
                    ChatColor.BLUE + "Blue:");
            redFlags.forEach(flag -> lines.add(CTFUtil.printFlagStatus(flag) + " " + CTFUtil.printFlagPosition(flag)));
            lines.add("  ");
            lines.add(ChatColor.RED + "Red:");
            blueFlags.forEach(flag -> lines.add(CTFUtil.printFlagStatus(flag) + " " + CTFUtil.printFlagPosition(flag)));

            board.updateLines(lines);

            if (ctfUser.isCapturing()) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 2, 0));
                if (CTFUtil.isInSpawn(ctfUser)) {
                    CTFUtil.secureFlag(ctfUser);
                }
            }

            if (getTimer() % 20 == 0) {
                CTFUtil.modifyEnergy(ctfUser, 0.2);
                CTFUtil.updateNametag(ctfUser);
                CTF.getInstance().getPlayerListener().getUserManager().getSpecs().forEach(spec -> player.hidePlayer(CTF.getInstance(), CTFUtil.getPlayer(spec)));
                CTF.getInstance().getPlayerListener().getUserManager().getIngame().forEach(ingame -> player.showPlayer(CTF.getInstance(), CTFUtil.getPlayer(ingame)));
            }

            if (CTFUtil.isInFlagRegion(player.getLocation()) && getTimer() % 20 == 0) {
                player.damage(2);
            }

            Kit kit = source.getKit();
            List<String> actionBarComponents = new ArrayList<>();
            if (kit instanceof ClickKit clickKit) {
                for (ClickItem clickItem : clickKit.getClickItems()) {
                    String cooldownPrefix = getCooldownPrefix(clickItem.getCooldown());
                    String cooldownNotification = clickItem.getCurrentCooldown() == 0 ?
                            (!clickItem.hasEnergy(source) ? "<red>Low Energy" : "Ready") : clickItem.getCurrentCooldown() + "";
                    actionBarComponents.add("<light_purple>" + clickItem.getName() + "<gray> (" + cooldownPrefix + cooldownNotification + "<gray>)");
                }
            }
            if (kit instanceof MultiCooldown multiCooldown) {
                for (Cooldown cooldown : multiCooldown.getCooldowns()) {
                    String cooldownPrefix = getCooldownPrefix(cooldown);
                    EnergyConsumer energyConsumer = cooldown.getEnergyConsumer();
                    String cooldownNotification = cooldown.isActive() ? cooldown.getSecondsLeft() + "" :
                            (energyConsumer != null && !energyConsumer.hasEnergy(source) ? "<red>Low Energy" : "Ready");
                    actionBarComponents.add("<light_purple>" + cooldown.getId() + "<gray> (" + cooldownPrefix + cooldownNotification + "<gray>)");
                }
            }
            if (kit instanceof MultiCounter multiCounter) {
                for (Counter counter : multiCounter.getCounters()) {
                    String counterPrefix = counter.getCount() == 0 ? "<red>" : "<green>";
                    actionBarComponents.add(counter.getColor() + counter.getName() + "<gray> (" + counterPrefix + counter.getCount() + "<gray>)");
                }
            }
            Audience.audience(player).sendActionBar(MiniMessage.miniMessage().deserialize(String.join("<gray> - ", actionBarComponents)));
        }

        if (ctf.getMap().getFlags().stream().filter(flag -> flag.getTeam() == TeamColor.BLUE).allMatch(flag -> flag.getStatus() == CaptureStatus.CAPTURED)) {
            advance(new PostStage(ctf.getRed(), ctf.getBlue(), getTime()));
        } else if (ctf.getMap().getFlags().stream().filter(flag -> flag.getTeam() == TeamColor.RED).allMatch(flag -> flag.getStatus() == CaptureStatus.CAPTURED)) {
            advance(new PostStage(ctf.getBlue(), ctf.getRed(), getTime()));
        }
    }

    private String getCooldownPrefix(Cooldown cooldown) {
        int secondsLeft = cooldown.getSecondsLeft();
        if (secondsLeft == 0) {
            return "<green>";
        } else if (secondsLeft <= 15) {
            return "<yellow>";
        } else {
            return "<red>";
        }
    }
}
