package com.yalcinkaya.ctf.util;

import com.yalcinkaya.ctf.CTF;
import com.yalcinkaya.ctf.listener.PlayerListener;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

@Getter
public class TempBlock {

    private Block block;
    private Material original;
    private Material temporary;
    private boolean unbreakable;

    public TempBlock(Block block, Material temporary, boolean unbreakable, double duration) {
        this.block = block;
        PlayerListener listener = CTF.getInstance().getPlayerListener();
        if (listener.isTempBlock(block)) {
            this.original = listener.getFromBlock(block).getOriginal();
        } else {
            this.original = block.getType();
        }
        this.temporary = temporary;
        this.unbreakable = unbreakable;

        if (CTFUtil.isInFlagRegion(block.getLocation()) || CTFUtil.isInSpawnRegion(block.getLocation())) {
            return;
        }

        block.setType(temporary);
        listener.getTempBlocks().add(this);

        TempBlock removeLater = this;

        new BukkitRunnable() {

            @Override
            public void run() {
                block.setType(original);
                listener.getTempBlocks().remove(removeLater);
            }

        }.runTaskLater(CTF.getInstance(), (long) duration * 20);
    }
}
