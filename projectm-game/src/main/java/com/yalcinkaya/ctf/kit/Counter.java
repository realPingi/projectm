package com.yalcinkaya.ctf.kit;

import com.yalcinkaya.core.util.CoreUtil;
import com.yalcinkaya.core.util.MessageType;
import com.yalcinkaya.ctf.user.CTFUser;
import lombok.Getter;
import lombok.Setter;

@Getter
public class Counter {

    private String name;
    private String color;
    @Setter
    private int count;
    private int maxCount;

    public Counter(String name, String color) {
        new Counter(name, color, 0);
    }

    public Counter(String name, String color, int maxCount) {
        this.name = name;
        this.color = color;
        this.maxCount = maxCount;
    }

    public boolean hasMinimum(int minimum) {
        return count >= minimum;
    }

    public void sendWarning(CTFUser user) {
        user.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Not enough ", name, "."));
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
