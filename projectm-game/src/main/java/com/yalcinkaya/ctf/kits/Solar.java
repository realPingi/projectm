package com.yalcinkaya.ctf.kits;

import com.yalcinkaya.ctf.kit.ClickItem;
import com.yalcinkaya.ctf.kit.ClickKit;
import com.yalcinkaya.ctf.kit.Kit;
import com.yalcinkaya.ctf.kits.clickItems.Blaze;

public class Solar extends Kit implements ClickKit {

    Blaze blaze = new Blaze();

    @Override
    public ClickItem[] getClickItems() {
        return new ClickItem[]{blaze};
    }

    @Override
    public String getName() {
        return "Solar";
    }
}
