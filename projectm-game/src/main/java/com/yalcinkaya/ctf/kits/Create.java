package com.yalcinkaya.ctf.kits;

import com.yalcinkaya.ctf.kit.ClickItem;
import com.yalcinkaya.ctf.kit.ClickKit;
import com.yalcinkaya.ctf.kit.Kit;
import com.yalcinkaya.ctf.kits.clickItems.Tunnel;
import com.yalcinkaya.ctf.kits.clickItems.Wall;
import com.yalcinkaya.ctf.util.PotentialObject;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Create extends Kit implements ClickKit {

    public static final Set<PotentialObject<Material>> materials = new HashSet<>(Arrays.asList(new PotentialObject<Material>(Material.PACKED_MUD, 20), new PotentialObject<Material>(Material.OAK_LEAVES, 15), new PotentialObject<Material>(Material.MOSS_BLOCK, 15), new PotentialObject<Material>(Material.MANGROVE_ROOTS, 20), new PotentialObject<Material>(Material.MUDDY_MANGROVE_ROOTS, 30)));

    private final Wall wall = new Wall();
    private final Tunnel tunnel = new Tunnel();

    @Override
    public ClickItem[] getClickItems() {
        return new ClickItem[]{wall, tunnel};
    }

    @Override
    public String getName() {
        return "K:Re/8";
    }

}
