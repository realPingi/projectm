package com.yalcinkaya.ctf.kit;

import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import com.yalcinkaya.ctf.util.MessageType;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.ChatColor;

@Getter
public class Counter {

    private String name;
    private ChatColor color;
    @Setter
    private int count;
    private int maxCount;

    public Counter(String name, ChatColor color) {
        new Counter(name, color, 0);
    }

    public Counter(String name, ChatColor color, int maxCount) {
        this.name = name;
        this.color = color;
        this.maxCount = maxCount;
    }

    public boolean hasMinimum(int minimum) {
        return count >= minimum;
    }

    public void sendWarning(CTFUser user) {
        user.sendMessage(CTFUtil.getCTFMessage(MessageType.WARNING, ChatColor.GRAY + "Not enough " + name + "."));
    }

    public void increase() {
        if (isCapped() && count + 1 > maxCount) {
            return;
        }
        count++;
    }

    public void decrease() {
        if (count - 1 < 0) {
            return;
        }
        count--;
    }

    public void add(int mod) {
        if (count + mod < 0) {
            return;
        }
        if (isCapped() && count + mod > maxCount) {
            return;
        }
        count += mod;
    }

    public void reset() {
        count = 0;
    }

    public boolean isCapped() {
        return maxCount > 0;
    }
}
