package com.yalcinkaya.ctf.stages;

import com.yalcinkaya.ctf.CTF;
import com.yalcinkaya.ctf.flag.CaptureStatus;
import com.yalcinkaya.ctf.flag.Flag;
import com.yalcinkaya.ctf.kit.*;
import com.yalcinkaya.ctf.listener.CaptureStageListener;
import com.yalcinkaya.ctf.team.TeamColor;
import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import com.yalcinkaya.util.MathUtil;
import fr.mrmicky.fastboard.FastBoard;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
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
            board.updateTitle(ChatColor.BOLD + "" + ChatColor.GOLD + "ProjectM");

            List<String> lines = new ArrayList<>();
            Collections.addAll(lines, "",
                    ChatColor.GRAY + "Game Time: " + ChatColor.GOLD + MathUtil.getReadableSeconds(getTimer()),
                    ChatColor.GRAY + "Kit: " + ChatColor.GOLD + kitName,
                    ChatColor.GRAY + "Energy: " + ChatColor.GOLD + MathUtil.roundDouble(ctfUser.getEnergy()),
                    " ",
                    ChatColor.BLUE + "Blue:");
            redFlags.forEach(flag -> lines.add(ChatColor.GRAY + CTFUtil.printFlagStatus(flag) + " " + CTFUtil.printFlagPosition(flag)));
            lines.add("  ");
            lines.add(ChatColor.RED + "Red:");
            blueFlags.forEach(flag -> lines.add(ChatColor.GRAY + CTFUtil.printFlagStatus(flag) + " " + CTFUtil.printFlagPosition(flag)));

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
                    ChatColor cooldownPrefix = getCooldownPrefix(clickItem.getCooldown());
                    String cooldownNotification = clickItem.getCurrentCooldown() == 0 ?
                            (!clickItem.hasEnergy(ctfUser) ? ChatColor.RED + "Low Energy" : "Ready") : clickItem.getCurrentCooldown() + "";
                    actionBarComponents.add(ChatColor.LIGHT_PURPLE + clickItem.getName() + ChatColor.GRAY + " (" + cooldownPrefix + cooldownNotification + ChatColor.GRAY + ")");
                }
            }
            if (kit instanceof MultiCooldown multiCooldown) {
                for (Cooldown cooldown : multiCooldown.getCooldowns()) {
                    ChatColor cooldownPrefix = getCooldownPrefix(cooldown);
                    EnergyConsumer energyConsumer = cooldown.getEnergyConsumer();
                    String cooldownNotification = cooldown.isActive() ? cooldown.getSecondsLeft() + "" :
                            (energyConsumer != null && !energyConsumer.hasEnergy(ctfUser) ? ChatColor.RED + "Low Energy" : "Ready");
                    actionBarComponents.add(ChatColor.LIGHT_PURPLE + cooldown.getId() + ChatColor.GRAY + " (" + cooldownPrefix + cooldownNotification + ChatColor.GRAY + ")");
                }
            }
            if (kit instanceof MultiCounter multiCounter) {
                for (Counter counter : multiCounter.getCounters()) {
                    ChatColor counterPrefix = counter.getCount() == 0 ? ChatColor.RED : ChatColor.GREEN;
                    actionBarComponents.add(counter.getColor() + counter.getName() + ChatColor.GRAY + " (" + counterPrefix + counter.getCount() + ChatColor.GRAY + ")");
                }
            }
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(String.join(ChatColor.GRAY + " - ", actionBarComponents)));

        }
    }

    private ChatColor getCooldownPrefix(Cooldown cooldown) {
        int secondsLeft = cooldown.getSecondsLeft();
        if (secondsLeft == 0) {
            return ChatColor.GREEN;
        } else if (secondsLeft <= 15) {
            return ChatColor.YELLOW;
        } else {
            return ChatColor.RED;
        }
    }
}
