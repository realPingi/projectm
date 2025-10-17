package com.yalcinkaya.ctf.kits;

import com.yalcinkaya.ctf.kit.ClickItem;
import com.yalcinkaya.ctf.kit.ClickKit;
import com.yalcinkaya.ctf.kit.Kit;
import com.yalcinkaya.ctf.kits.clickItems.Extract;

public class Silvy extends Kit implements ClickKit {

    private final Extract soulBeam = new Extract();

    @Override
    public ClickItem[] getClickItems() {
        return new ClickItem[]{soulBeam};
    }

    @Override
    public String getName() {
        return "Silvy";
    }
}
