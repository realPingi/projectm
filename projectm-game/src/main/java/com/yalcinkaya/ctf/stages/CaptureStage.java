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
import org.bukkit.event.HandlerList;
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
        if (ctf.getMap().getFlags().stream().filter(flag -> flag.getTeam() == TeamColor.BLUE).allMatch(flag -> flag.getStatus() == CaptureStatus.CAPTURED)) {
            HandlerList.unregisterAll(stageListener);
            advance(new PostStage(ctf.getRed()));
        } else if (ctf.getMap().getFlags().stream().filter(flag -> flag.getTeam() == TeamColor.RED).allMatch(flag -> flag.getStatus() == CaptureStatus.CAPTURED)) {
            HandlerList.unregisterAll(stageListener);
            advance(new PostStage(ctf.getBlue()));
        }

        for (Player player : Bukkit.getOnlinePlayers()) {

            CTFUser ctfUser = CTFUtil.getUser(player);

            String kitName = ctfUser.getKit() == null ? "None" : ctfUser.getKit().getName();
            List<Flag> blueFlags = ctf.getMap().getFlags().stream().filter(flag -> flag.getTeam() == TeamColor.BLUE).toList();
            List<Flag> redFlags = ctf.getMap().getFlags().stream().filter(flag -> flag.getTeam() == TeamColor.RED).toList();

            FastBoard board = ctfUser.getScoreboard();
            board.updateTitle(ChatColor.GOLD + "" + ChatColor.BOLD + "ProjectM");

            List<String> lines = new ArrayList<>();
            Collections.addAll(lines, "",
                    ChatColor.GRAY + "Game Time: " + ChatColor.GOLD + MathUtil.getReadableSeconds(getTimer()),
                    ChatColor.GRAY + "Kit: " + ChatColor.GOLD + kitName,
                    ChatColor.GRAY + "Energy: " + ChatColor.GOLD + MathUtil.roundDouble(ctfUser.getEnergy()),
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

            if (getTick() % 20 == 0) {
                CTFUtil.modifyEnergy(ctfUser, 0.2);
            }

            Kit kit = ctfUser.getKit();
            List<String> actionBarComponents = new ArrayList<>();
            if (kit instanceof ClickKit clickKit) {
                for (ClickItem clickItem : clickKit.getClickItems()) {
                    String cooldownPrefix = getCooldownPrefix(clickItem.getCooldown());
                    String cooldownNotification = clickItem.getCurrentCooldown() == 0 ?
                            (!clickItem.hasEnergy(ctfUser) ? "<red>Low Energy" : "Ready") : clickItem.getCurrentCooldown() + "";
                    actionBarComponents.add("<light_purple>" + clickItem.getName() + "<gray> (" + cooldownPrefix + cooldownNotification + "<gray>)");
                }
            }
            if (kit instanceof MultiCooldown multiCooldown) {
                for (Cooldown cooldown : multiCooldown.getCooldowns()) {
                    String cooldownPrefix = getCooldownPrefix(cooldown);
                    EnergyConsumer energyConsumer = cooldown.getEnergyConsumer();
                    String cooldownNotification = cooldown.isActive() ? cooldown.getSecondsLeft() + "" :
                            (energyConsumer != null && !energyConsumer.hasEnergy(ctfUser) ? "<red>Low Energy" : "Ready");
                    actionBarComponents.add("<light_purple>" + cooldown.getId() + "<gray> (" + cooldownPrefix + cooldownNotification + "<gray>)");
                }
            }
            if (kit instanceof MultiCounter multiCounter) {
                for (Counter counter : multiCounter.getCounters()) {
                    String counterPrefix = counter.getCount() == 0 ? "<red>" : "<green>";
                    actionBarComponents.add(counter.getColor() + counter.getName() +  "<gray> (" + counterPrefix + counter.getCount() + "<gray>)");
                }
            }
            Audience.audience(player).sendActionBar(MiniMessage.miniMessage().deserialize(String.join("<gray> - ", actionBarComponents)));

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
