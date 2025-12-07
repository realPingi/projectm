package com.yalcinkaya.ctf.kits.clickItems;

import com.yalcinkaya.core.util.CoreUtil;
import com.yalcinkaya.core.util.MessageType;
import com.yalcinkaya.ctf.kit.ClickItem;
import com.yalcinkaya.ctf.kit.Counter;
import com.yalcinkaya.ctf.kit.EnergyConsumer;
import com.yalcinkaya.ctf.user.CTFUser;
import com.yalcinkaya.ctf.util.CTFUtil;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

public class Reflect extends ClickItem implements EnergyConsumer {

    private final ItemStack item = CTFUtil.createIcon("Reflect", Material.DIAMOND);
    private final int chargesNeeded = 30;
    @Getter
    private final Counter charges = new Counter("Charges", "<blue>", chargesNeeded);
    @Getter
    @Setter
    private boolean rage;

    @Override
    public boolean tryClick(CTFUser activator) {

        if (rage) {
            activator.sendMessage(CoreUtil.getMessage(MessageType.WARNING, "Your counter is still active."));
            return false;
        }

        if (!charges.hasMinimum(chargesNeeded)) {
            charges.sendWarning(activator);
            return false;
        }

        rage = true;
        charges.reset();
        activator.sendMessage(CoreUtil.getMessage(MessageType.SUCCESS, "Your counter is now active."));
        return true;
    }

    @Override
    public String getName() {
        return "Reflect";
    }

    @Override
    public ItemStack getItem() {
        return item;
    }

    @Override
    public int getCooldownSeconds() {
        return 40;
    }

    @Override
    public Sound getSound() {
        return Sound.BLOCK_ANVIL_PLACE;
    }

    @Override
    public double getEnergy() {
        return 40;
    }
}
