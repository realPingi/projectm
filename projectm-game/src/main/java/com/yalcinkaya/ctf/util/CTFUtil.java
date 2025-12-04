package com.yalcinkaya.ctf.util;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.yalcinkaya.core.ProjectM;
import com.yalcinkaya.core.redis.QueueType;
import com.yalcinkaya.core.redis.RedisDataService;
import com.yalcinkaya.core.util.CoreUtil;
import com.yalcinkaya.core.util.ItemBuilder;
import com.yalcinkaya.core.util.MessageType;
import com.yalcinkaya.core.util.PotentialObject;
import com.yalcinkaya.ctf.CTF;
import com.yalcinkaya.ctf.CTFKit;
import com.yalcinkaya.ctf.CTFMap;
import com.yalcinkaya.ctf.flag.CaptureStatus;
import com.yalcinkaya.ctf.flag.Flag;
import com.yalcinkaya.ctf.flag.FlagLocation;
import com.yalcinkaya.ctf.hotbar.SpecHotbarGUI;
import com.yalcinkaya.ctf.kit.ClickItem;
import com.yalcinkaya.ctf.kit.ClickKit;
import com.yalcinkaya.ctf.kit.Kit;
import com.yalcinkaya.ctf.stages.CaptureStage;
import com.yalcinkaya.ctf.team.Team;
import com.yalcinkaya.ctf.team.TeamColor;
import com.yalcinkaya.ctf.user.CTFUser;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class CTFUtil {

    public static final Set<PotentialObject<Color>> blueColors = CoreUtil.mixColors(Color.fromRGB(0, 81, 255), Color.fromRGB(102, 151, 255), Color.fromRGB(0, 26, 82), 70, 50, 20);
    public static final Set<PotentialObject<Color>> redColors = CoreUtil.mixColors(Color.fromRGB(247, 87, 95), Color.fromRGB(247, 153, 158), Color.fromRGB(84, 13, 16), 70, 50, 20);

    public static CTFUser getUser(Player player) {
        return getUser(player.getUniqueId());
    }

    public static CTFUser getUser(UUID uuid) {
        return CTF.getInstance().getPlayerListener().getUserManager().getUser(uuid);
    }

    public static Player getPlayer(CTFUser user) {
        return Bukkit.getPlayer(user.getUuid());
    }

    public static Player getPlayer(UUID uuid) {
        return Bukkit.getPlayer(uuid);
    }

    public static Set<PotentialObject<Color>> getColorMix(CTFUser user) {
        return user.getTeam().getColor() == TeamColor.BLUE ? blueColors : redColors;
    }

    public static void sendMessage(Player player, MessageType type, String... strings) {
        getUser(player).sendMessage(CoreUtil.getMessage(type, strings));
    }

    public static void equipPlayer(Player player) {
        CTFUser user = CTFUtil.getUser(player.getUniqueId());
        Kit kit = user.getKit();
        player.getInventory().clear();
        player.getInventory().setBoots(ItemBuilder.of(Material.IRON_BOOTS).unbreakable(true).build());
        player.getInventory().setLeggings(ItemBuilder.of(Material.IRON_LEGGINGS).unbreakable(true).build());
        player.getInventory().setChestplate(ItemBuilder.of(Material.IRON_CHESTPLATE).unbreakable(true).build());
        player.getInventory().setHelmet(ItemBuilder.of(Material.IRON_HELMET).unbreakable(true).build());
        player.getInventory().setItem(0, ItemBuilder.of(Material.IRON_SWORD).unbreakable(true).build());
        player.getInventory().setItem(1, new ItemStack(Material.GOLDEN_APPLE, 8));
        player.getInventory().setItem(6, new ItemStack(Material.WATER_BUCKET));
        player.getInventory().setItem(7, ItemBuilder.of(Material.IRON_AXE).unbreakable(true).build());
        player.getInventory().setItem(8, new ItemStack(Material.OAK_PLANKS, 64));
        player.getInventory().setItem(17, new ItemStack(Material.OAK_PLANKS, 64));
        player.getInventory().setItem(26, new ItemStack(Material.OAK_PLANKS, 64));
        player.getInventory().setItem(35, new ItemStack(Material.OAK_PLANKS, 64));
        if (kit instanceof ClickKit) {
            ClickKit clickKit = (ClickKit) kit;
            player.getInventory().addItem(Arrays.stream(clickKit.getClickItems()).map(ClickItem::getItem).toArray(ItemStack[]::new));
        }
        if (kit != null && kit.getStartItems() != null) {
            player.getInventory().addItem(kit.getStartItems());
        }
    }

    public static void clearPlayer(Player player) {

        int health = 20;
        if (CTF.getInstance().getCurrentStage() instanceof CaptureStage) {
            health = 40;
        }

        setFly(player, false);
        player.setFlySpeed(0.5F);
        player.setGameMode(GameMode.SURVIVAL);
        player.setInvisible(false);
        player.setExp(0);
        player.setLevel(0);
        player.setFoodLevel(20);
        player.setFallDistance(0);
        player.setMaxHealth(health);
        player.setHealth(health);
        player.getInventory().clear();
    }

    public static void teleportPlayerMidMap(CTFUser user) {
        CTF ctf = CTF.getInstance();
        Player player = getPlayer(user);
        Location blueSpawn = ctf.getMap().getBlueSpawn();
        Location redSpawn = ctf.getMap().getRedSpawn();
        Location midPoint = blueSpawn.clone().add(redSpawn.clone().subtract(blueSpawn).multiply(0.5));
        midPoint.setY(75);
        player.teleport(midPoint);
        clearPlayer(player);
        setFly(player, true);
    }

    public static void setFly(Player player, boolean fly) {
        player.setAllowFlight(fly);
        player.setFlying(fly);
    }

    public static boolean isSelected(Team team, CTFKit ctfKit) {
        return team.getMembers().stream().map(CTFUser::getKit).anyMatch(kit -> kit != null && CTFKit.getFromKit(kit) == ctfKit);
    }

    public static void setTeam(CTFUser user, Team team) {
        removeTeam(user);
        team.addMember(user);

        if (getPlayer(user) != null && getPlayer(user).isOnline()) {
            updateNametag(user);
        }
    }

    public static void removeTeam(CTFUser user) {
        CTF ctf = CTF.getInstance();
        ctf.getBlue().removeMember(user);
        ctf.getRed().removeMember(user);
        user.setKit(null);
        user.setEnergy(0);
        if (user.isCapturing()) {
            restoreFlag(user);
        }
    }

    public static void updateNametag(CTFUser user) {
        NamedTextColor color = user.getTeam() == null ? NamedTextColor.GRAY : (user.getTeam().getColor() == TeamColor.BLUE ? NamedTextColor.BLUE : NamedTextColor.RED);
        ProjectM.getInstance().getNametagManager().setPlayerNametag(getPlayer(user), color.toString(), color);
    }

    public static void modifyEnergy(CTFUser user, double energy) {

        if (user.isCapturing() || user.isSpectating()) {
            return;
        }

        double modifiedEnergy = user.getEnergy() + energy;
        if (modifiedEnergy > 100) {
            modifiedEnergy = 100;
        } else if (modifiedEnergy < 0) {
            modifiedEnergy = 0;
        }
        user.setEnergy(modifiedEnergy);
    }

    public static void broadcastMessageForAll(MessageType messageType, String... strings) {
        Bukkit.getOnlinePlayers().forEach(player -> getUser(player).sendMessage(CoreUtil.getMessage(messageType, strings)));
    }

    public static void broadcastMessageForTeam(Team team, MessageType messageType, String... strings) {
        team.getMembers().forEach(user -> user.sendMessage(CoreUtil.getMessage(messageType, strings)));
    }

    public static void playSoundForAll(Sound sound) {
        Bukkit.getOnlinePlayers().forEach(player -> getUser(player).playSound(sound));
    }

    public static void playSoundForTeam(Team team, Sound sound) {
        team.getMembers().forEach(user -> user.playSound(sound));
    }

    public static void pickUpFlag(CTFUser user, Flag flag) {
        modifyEnergy(user, -100);
        getPlayer(user).getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 2));
        user.setFlag(flag);
        setCaptureStatus(flag, CaptureStatus.DANGER);
        getPlayer(user).setGlowing(true);
        playSoundForAll(Sound.BLOCK_PISTON_CONTRACT);
        broadcastMessageForAll(MessageType.BROADCAST, "", getColoredName(user), " picked up a flag!");
    }

    public static void restoreFlag(CTFUser user) {
        Flag flag = user.getFlag();
        setCaptureStatus(flag, CaptureStatus.SAFE);
        user.setFlag(null);
        getPlayer(user).setGlowing(false);
        playSoundForAll(Sound.BLOCK_FENCE_GATE_CLOSE);
        broadcastMessageForAll(MessageType.BROADCAST, "", getColoredName(user), " lost the flag!");
    }

    public static void secureFlag(CTFUser user) {
        Flag flag = user.getFlag();
        flag.setStatus(CaptureStatus.CAPTURED);
        user.setFlag(null);
        getPlayer(user).setGlowing(false);
        getPlayer(user).setMaxHealth(40);
        playSoundForAll(Sound.ENTITY_ENDER_DRAGON_FLAP);
        broadcastMessageForAll(MessageType.BROADCAST, "", getColoredName(user), " successfully captured a flag!");
    }

    public static void setCaptureStatus(Flag flag, CaptureStatus status) {
        flag.setStatus(status);
        updateFlagPillar(flag, status == CaptureStatus.SAFE);
    }

    public static String getColoredName(CTFUser user) {
        String teamColor = user.getTeam() == null ? "<gray>" : (user.getTeam().getColor() == TeamColor.BLUE ? "<blue>" : "<red>");
        return teamColor + getPlayer(user).getName() + "<gray>";
    }

    public static void updateFlagPillar(Flag flag, boolean restore) {
        Material pillarMaterial = restore ? (flag.getTeam() == TeamColor.BLUE ? Material.BLUE_GLAZED_TERRACOTTA : Material.RED_GLAZED_TERRACOTTA) : Material.GRAY_GLAZED_TERRACOTTA;
        Block pillarBottom = flag.getHome().getBlock();
        Block pillarTop = pillarBottom.getRelative(BlockFace.UP);
        pillarBottom.setType(pillarMaterial);
        pillarTop.setType(pillarMaterial);

    }

    public static boolean isInFlagRange(Flag flag, Player player) {
        return flag.getHome().distance(player.getLocation()) < 2;
    }

    public static boolean isInSpawn(CTFUser user) {
        String mapId = CTF.getInstance().getMapId();
        Player player = getPlayer(user);
        Location location = player.getLocation();
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(player.getWorld()));
        String id = user.getTeam().getColor() == TeamColor.BLUE ? mapId + "_blue_spawn" : mapId + "_red_spawn";
        ProtectedRegion spawn = regions.getRegion(id);
        return isInRegion(spawn, location);
    }

    public static boolean isInRegion(ProtectedRegion region, Location location) {
        return region != null && region.contains(BlockVector3.at(location.getX(), location.getY(), location.getZ()));
    }

    public static String printFlagStatus(Flag flag) {
        switch (flag.getStatus()) {
            case SAFE:
                return ChatColor.WHITE + "\u2b1c";
            case DANGER:
                return ChatColor.YELLOW + "\u2592";
            case CAPTURED:
                return ChatColor.GREEN + "\u2b1b";
            default:
                return "";
        }
    }

    public static String printFlagPosition(Flag flag) {
        return CoreUtil.camelizeString(flag.getLocation().toString());
    }

    public static ItemStack createIcon(String name, Material material) {
        return ItemBuilder.of(material).name("<italic:false><light_purple>" + name).build();
    }

    public static ItemStack createIcon(String name, Material material, String desc) {
        return ItemBuilder.of(material).name("<italic:false><light_purple>" + name).wrapLore(desc, "<italic:false><gray>").build();
    }

    public static void loadMap() {
        String mapId = CTF.getInstance().getMapId();
        CTF.getInstance().setMap(CTFMap.getFromId(mapId).getMap());
        Path p = Paths.get("maps", mapId + ".schem");
        loadSchematic(p.toString());
    }

    public static void loadSchematic(String schematicPath) {

        File schematic = new File(schematicPath);
        Clipboard clipboard;

        ClipboardFormat format = ClipboardFormats.findByFile(schematic);

        try (ClipboardReader reader = format.getReader(new FileInputStream(schematic))) {

            clipboard = reader.read();

            EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder().world(BukkitAdapter.adapt(Bukkit.getWorld(CTF.ctfWorld))).build();
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(BlockVector3.at(0, 40, 0))
                    .build();

            try {
                Operations.complete(operation);
                editSession.close();
            } catch (WorldEditException e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isInFlagRegion(String team, Location location) {
        String mapId = CTF.getInstance().getMapId();
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(location.getWorld()));
        List<ProtectedRegion> flagRegions = new ArrayList<>();
        for (FlagLocation flagLocation : FlagLocation.values()) {
            String id = mapId + "_" + team.toLowerCase() + "_" + "flag" + "_" + flagLocation.toString().toLowerCase();
            ProtectedRegion flagRegion = regions.getRegion(id);
            flagRegions.add(flagRegion);
        }
        return flagRegions.stream().anyMatch(region -> CTFUtil.isInRegion(region, location));
    }

    public static boolean isInFlagRegion(Location location) {
        return isInFlagRegion("blue", location) || isInFlagRegion("red", location);
    }

    public static boolean isInSpawnRegion(Location location) {
        String mapId = CTF.getInstance().getMapId();
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(location.getWorld()));
        List<ProtectedRegion> spawnRegions = new ArrayList<>();
        spawnRegions.add(regions.getRegion(mapId + "_blue_spawn"));
        spawnRegions.add(regions.getRegion(mapId + "_red_spawn"));
        return spawnRegions.stream().anyMatch(region -> CTFUtil.isInRegion(region, location));
    }

    public static void rewardKill(UUID uuid) {
        getPlayer(uuid).getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 2));
        CTFUser user = CTFUtil.getUser(uuid);
        user.getTeam().getMembers().forEach(u -> modifyEnergy(u, 20));
        playSoundForTeam(user.getTeam(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
    }

    public static List<Block> hollowRing(Location start, Vector vector, int length, int width) {
        List<Block> blocks = new ArrayList<>();
        blocks.addAll(centeredPlane(start, vector, length + 2, width + 2));
        blocks.removeAll(centeredPlane(start, vector, length, width));
        return blocks;
    }

    public static List<Block> centeredPlane(Location start, Vector normal, int length, int width) {
        double x = normal.getX();
        double y = normal.getY();
        double z = normal.getZ();
        Vector v1 = new Vector(-y, x, 0);
        Vector v2 = new Vector(x * z, y * z, -(x * x + y * y));
        return centeredPlane(start, v1, length, v2, width);
    }

    public static List<Block> centeredPlane(Location start, Vector v1, int length, Vector v2, int width) {
        List<Block> blocks = new ArrayList<>();
        blocks.addAll(plane(start, v2, length, v1, width));
        blocks.addAll(plane(start, v2, length, v1.clone().multiply(-1), width));
        blocks.addAll(plane(start, v2.clone().multiply(-1), length, v1, width));
        blocks.addAll(plane(start, v2.clone().multiply(-1), length, v1.clone().multiply(-1), width));
        return blocks;
    }

    public static List<Block> plane(Location start, Vector v1, int length, Vector v2, int width) {
        List<Block> plane = new ArrayList<>();
        BlockIterator lengthIterator = new BlockIterator(start.getWorld(), start.toVector(), v1, 0, length);
        while (lengthIterator.hasNext()) {
            Block node = lengthIterator.next();
            BlockIterator widthIterator = new BlockIterator(node.getWorld(), node.getLocation().toVector(), v2, 0, width);
            while (widthIterator.hasNext()) {
                plane.add(widthIterator.next());
            }
        }
        return plane;
    }

    public static void setupSpectator(CTFUser user) {
        CTFUtil.teleportPlayerMidMap(user);
        Player player = getPlayer(user);
        player.setInvisible(true);
        CTF.getInstance().getHotbarManager().getSpecHotbarGUI().supply(player);
    }

    public static Set<CTFUser> getNearbyUsers(Location location, double radius) {
        Set<CTFUser> nearbyUsers = new HashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            CTFUser u = CTFUtil.getUser(p.getUniqueId());
            if (u == null || u.isSpectating()) {
                continue;
            }
            if (p.getLocation().distance(location) < radius) {
                nearbyUsers.add(u);
            }
        }
        return nearbyUsers;
    }

    public static Set<CTFUser> getNearbyMates(CTFUser user, Location location, double radius) {
        Set<CTFUser> nearbyUsers = new HashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            CTFUser u = CTFUtil.getUser(p.getUniqueId());
            if (u == null || u.getTeam() == null || u.isSpectating()) {
                continue;
            }
            if (!u.getTeam().equals(user.getTeam())) {
                continue;
            }
            if (p.getLocation().distance(location) < radius) {
                nearbyUsers.add(u);
            }
        }
        return nearbyUsers;
    }

    public static Set<CTFUser> getNearbyOpponents(CTFUser user, Location location, double radius) {
        Set<CTFUser> nearbyUsers = new HashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            CTFUser u = CTFUtil.getUser(p.getUniqueId());
            if (u == null || u.getTeam() == null || u.isSpectating()) {
                continue;
            }
            if (u.getTeam().equals(user.getTeam())) {
                continue;
            }
            if (p.getLocation().distance(location) < radius) {
                nearbyUsers.add(u);
            }
        }
        return nearbyUsers;
    }

    public static Team getEnemyTeam(CTFUser user) {
        return user.getTeam().getColor() == TeamColor.BLUE ? CTF.getInstance().getBlue() : CTF.getInstance().getRed();
    }

    public static List<Flag> getCapturedFlags(Team team) {
        return CTF.getInstance().getMap().getFlags().stream().filter(flag -> flag.getTeam() == team.getColor()).filter(flag -> flag.getStatus() == CaptureStatus.CAPTURED).toList();
    }

    public static void calcEloChanges(Team winner, Team loser) {
        QueueType queueType = CTF.getInstance().getQueueType();

        if (queueType == QueueType.CUSTOM) return;

        Bukkit.getScheduler().runTaskAsynchronously(CTF.getInstance(), () -> {
            RedisDataService redisDataService = ProjectM.getInstance().getRedisDataService();
            EloCalculator eloCalculator = new EloCalculator(queueType);
            winner.getMembers().forEach(member -> redisDataService.addElo(member.getUuid().toString(), queueType, eloCalculator.getEloWin(member, loser)));
            loser.getMembers().forEach(member -> redisDataService.addElo(member.getUuid().toString(), queueType, eloCalculator.getEloLoss(member, winner)));
        });
    }

}
