package com.yalcinkaya.ctf;

import com.yalcinkaya.ctf.map.Basic;
import com.yalcinkaya.ctf.map.Dust;
import com.yalcinkaya.ctf.map.Map;
import com.yalcinkaya.ctf.util.CTFUtil;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.function.Supplier;

@Getter
public enum CTFMap {

    BASIC("basic", Basic::new, CTFUtil.createIcon("Basic", Material.QUARTZ_BLOCK)),
    DUST("dust", Dust::new, CTFUtil.createIcon("Dust", Material.END_STONE));

    private String id;
    private Supplier<Map> supplier;
    private ItemStack icon;

    CTFMap(String id, Supplier<Map> supplier, ItemStack icon) {
        this.id = id;
        this.supplier = supplier;
        this.icon = icon;
    }

    public static ItemStack[] getIcons() {
        return Arrays.stream(values()).map(CTFMap::getIcon).toArray(ItemStack[]::new);
    }

    public static CTFMap getFromIcon(ItemStack icon) {
        return Arrays.stream(values()).filter(ctfMap -> icon != null && ctfMap.getIcon().getType() == icon.getType()).findFirst().orElse(null);
    }

    public static CTFMap getFromId(String id) {
        return Arrays.stream(values()).filter(ctfMap -> ctfMap.getId().equals(id)).findFirst().orElse(null);
    }

    public Map getMap() {
        return supplier.get();
    }

}
