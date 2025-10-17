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
import com.yalcinkaya.ctf.CTF;
import com.yalcinkaya.ctf.CTFKit;
import com.yalcinkaya.ctf.CTFMap;
import com.yalcinkaya.ctf.flag.CaptureStatus;
import com.yalcinkaya.ctf.flag.Flag;
import com.yalcinkaya.ctf.flag.FlagLocation;
import com.yalcinkaya.ctf.kit.ClickItem;
import com.yalcinkaya.ctf.kit.ClickKit;
import com.yalcinkaya.ctf.kit.Kit;
import com.yalcinkaya.ctf.stages.CaptureStage;
import com.yalcinkaya.ctf.team.Team;
import com.yalcinkaya.ctf.team.TeamColor;
import com.yalcinkaya.ctf.user.CTFUser;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class CTFUtil {

    public static final Set<Vector> simpleDirections = new HashSet<>(getSimpleDirections());
    public static final String LINEBREAK = "\n"; // or "\r\n";

    public static final Set<PotentialObject<Color>> blueColors = CTFUtil.mixColors(Color.fromRGB(0, 81, 255), Color.fromRGB(102, 151, 255), Color.fromRGB(0, 26, 82), 70, 50, 20);
    public static final Set<PotentialObject<Color>> redColors = CTFUtil.mixColors(Color.fromRGB(247, 87, 95), Color.fromRGB(247, 153, 158), Color.fromRGB(84, 13, 16), 70, 50, 20);

    public static Set<PotentialObject<Color>> getColorMix(CTFUser user) {
        return user.getTeam().getColor() == TeamColor.BLUE ? blueColors : redColors;
    }

    public static void equipPlayer(Player player) {
        CTFUser user = CTFUtil.getUser(player.getUniqueId());
        Kit kit = user.getKit();
        player.getInventory().clear();
        player.getInventory().setBoots(new ItemBuilder(Material.IRON_BOOTS).unbreakable().build());
        player.getInventory().setLeggings(new ItemBuilder(Material.IRON_LEGGINGS).unbreakable().build());
        player.getInventory().setChestplate(new ItemBuilder(Material.IRON_CHESTPLATE).unbreakable().build());
        player.getInventory().setHelmet(new ItemBuilder(Material.IRON_HELMET).unbreakable().build());
        player.getInventory().setItem(0, new ItemBuilder(Material.IRON_SWORD).unbreakable().build());
        player.getInventory().setItem(1, new ItemStack(Material.GOLDEN_APPLE, 8));
        player.getInventory().setItem(6, new ItemStack(Material.WATER_BUCKET));
        player.getInventory().setItem(7, new ItemBuilder(Material.IRON_AXE).unbreakable().build());
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

    public static void setTeam(CTFUser user, boolean blue) {
        CTF ctf = CTF.getInstance();
        if (blue) {
            ctf.getRed().getMembers().remove(user);
            ctf.getBlue().getMembers().add(user);
            user.setTeam(ctf.getBlue());
        } else {
            ctf.getBlue().getMembers().remove(user);
            ctf.getRed().getMembers().add(user);
            user.setTeam(ctf.getRed());
        }
        if (getPlayer(user) != null && getPlayer(user).isOnline()) {
            updateNametag(user);
        }
    }

    public static void updateNametag(CTFUser user) {

        if (!Bukkit.getPluginManager().isPluginEnabled("TAB")) {
            return;
        }

        ChatColor color = user.getTeam() == null ? ChatColor.GRAY : (user.getTeam().getColor() == TeamColor.BLUE ? ChatColor.BLUE : ChatColor.RED);
        String bukkitCode = color.toString();
        bukkitCode.replace(ChatColor.COLOR_CHAR, '&');
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab player " + CTFUtil.getPlayer(user).getName() + " tagprefix " + bukkitCode);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab player " + CTFUtil.getPlayer(user).getName() + " tabprefix " + bukkitCode);
    }

    public static void modifyEnergy(CTFUser user, double energy) {

        if (user.isCapturing()) {
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

    public static String getCTFMessage(MessageType messageType, String... strings) {
        return getColoredString(messageType.getPrefix(), messageType.getFormat(), strings);
    }

    public static String getColoredString(String prefix, String defaultString, String... strings) {
        String coloredString = prefix + defaultString + "";
        for (String string : strings) {
            coloredString += string + defaultString;
        }
        return coloredString;
    }

    public static void broadcastMessageForAll(MessageType messageType, String... strings) {
        broadcastMessageForTeam(CTF.getInstance().getBlue(), messageType, strings);
        broadcastMessageForTeam(CTF.getInstance().getRed(), messageType, strings);
    }

    public static void broadcastMessageForTeam(Team team, MessageType messageType, String... strings) {
        team.getMembers().forEach(user -> user.sendMessage(getCTFMessage(messageType, strings)));
    }

    public static void playSoundForAll(Sound sound) {
        playSoundForTeam(CTF.getInstance().getBlue(), sound);
        playSoundForTeam(CTF.getInstance().getRed(), sound);
    }

    public static void playSoundForTeam(Team team, Sound sound) {
        team.getMembers().forEach(user -> user.playSound(sound));
    }

    public static void pickUpFlag(CTFUser user, Flag flag) {
        modifyEnergy(user, -100);
        getPlayer(user).getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 2));
        user.setFlag(flag);
        setCaptureStatus(flag, CaptureStatus.DANGER);
        displayFlag(user);
        playSoundForAll(Sound.BLOCK_PISTON_CONTRACT);
        broadcastMessageForAll(MessageType.BROADCAST, getColoredName(user), " picked up a flag!");
    }

    public static void restoreFlag(CTFUser user) {
        Flag flag = user.getFlag();
        setCaptureStatus(flag, CaptureStatus.SAFE);
        user.setFlag(null);
        user.getFlagDisplay().cancel();
        playSoundForAll(Sound.BLOCK_FENCE_GATE_CLOSE);
        broadcastMessageForAll(MessageType.BROADCAST, getColoredName(user), " lost the flag!");
    }

    public static void secureFlag(CTFUser user) {
        Flag flag = user.getFlag();
        flag.setStatus(CaptureStatus.CAPTURED);
        user.setFlag(null);
        user.getFlagDisplay().cancel();
        getPlayer(user).setMaxHealth(40);
        playSoundForAll(Sound.ENTITY_ENDER_DRAGON_FLAP);
        broadcastMessageForAll(MessageType.BROADCAST, getColoredName(user), " successfully captured a flag!");
    }

    public static void setCaptureStatus(Flag flag, CaptureStatus status) {
        flag.setStatus(status);
        updateFlagPillar(flag, status == CaptureStatus.SAFE);
    }

    public static String getColoredName(CTFUser user) {
        ChatColor teamColor = user.getTeam() == null ? ChatColor.GRAY : (user.getTeam().getColor() == TeamColor.BLUE ? ChatColor.BLUE : ChatColor.RED);
        return teamColor + getPlayer(user).getName() + ChatColor.GRAY;
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
        String mapId = System.getenv("MAP_ID");
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
        return CTFUtil.camelizeString(flag.getLocation().toString());
    }

    public static String camelizeString(String string) {
        return string.substring(0, 1).toUpperCase() + string.substring(1).toLowerCase();
    }

    public static void displayFlag(CTFUser user) {
        ItemStack flagBanner = user.getTeam().getColor() == TeamColor.BLUE ? new ItemStack(Material.RED_BANNER) : new ItemStack(Material.BLUE_BANNER);
        SmartArmorStand smartArmorStand = new SmartArmorStand(getPlayer(user), flagBanner);
        smartArmorStand.follow();
        user.setFlagDisplay(smartArmorStand);
    }

    public static ItemStack createIcon(String name, Material material) {
        return new ItemBuilder(material).name(MessageType.INFO.getFormat() + name).build();
    }

    public static ItemStack createIcon(String name, Material material, String desc) {
        return new ItemBuilder(material).name(MessageType.INFO.getFormat() + name).lore(desc, ChatColor.GRAY).build();
    }


    public static Player getPlayer(CTFUser user) {
        return Bukkit.getPlayer(user.getUuid());
    }

    public static Player getPlayer(UUID uuid) {
        return Bukkit.getPlayer(uuid);
    }

    public static void loadMap() {
        String mapId = System.getenv("MAP_ID");
        CTF.getInstance().setMap(CTFMap.getFromId(mapId).getMap());
        loadSchematic("/maps/" + mapId + ".schem");
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
        String mapId = System.getenv("MAP_ID");
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
        String mapId = System.getenv("MAP_ID");
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(location.getWorld()));
        List<ProtectedRegion> spawnRegions = new ArrayList<>();
        spawnRegions.add(regions.getRegion(mapId + "_blue_spawn"));
        spawnRegions.add(regions.getRegion(mapId + "_red_spawn"));
        return spawnRegions.stream().anyMatch(region -> CTFUtil.isInRegion(region, location));
    }

    public static Location getCenter(Location location) {
        double adjustX = location.getX() < 0 ? -0.5 : 0.5;
        double adjustZ = location.getZ() > 0 ? -0.5 : 0.5;
        return location.clone().add(adjustX, 0, adjustZ);
    }

    public static void rewardKill(UUID uuid) {
        getPlayer(uuid).getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 2));
        CTFUser user = CTFUtil.getUser(uuid);
        user.getTeam().getMembers().forEach(u -> modifyEnergy(u, 20));
        playSoundForTeam(user.getTeam(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
    }

    public static void drawLine(Location point1, Location point2, double space, Color color, int size) {
        double distance = point1.distance(point2);
        Vector p1 = point1.toVector();
        Vector p2 = point2.toVector();
        Vector vector = p2.clone().subtract(p1).normalize().multiply(space);
        double length = 0;
        for (; length < distance; p1.add(vector)) {
            point1.getWorld().spawnParticle(Particle.REDSTONE, p1.getX(), p1.getY(), p1.getZ(), 1, new Particle.DustOptions(color, size));
            length += space;
        }
    }

    public static List<Block> hollowTunnel(Location start, Vector vector, int length, int width, int depth) {
        List<Block> blocks = new ArrayList<>();
        BlockIterator depthIterator = new BlockIterator(start.getWorld(), start.toVector(), vector, 0, length);
        while (depthIterator.hasNext()) {
            Block node = depthIterator.next();
            blocks.addAll(hollowRing(node.getLocation(), vector, length, width));
        }
        return blocks;
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

    public static List<Block> plane(Location start, Vector normal, int length, int width) {
        Vector v1 = getOrthogonalVector(normal, 0);
        Vector v2 = getOrthogonalVector(normal, 1);
        return plane(start, v1, length, v2, width);
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

    public static Vector simplifyDirection(Vector direction) {
        return simpleDirections.stream().min((a, b) -> Float.compare(a.angle(direction), b.angle(direction))).get();
    }

    private static Set<Vector> getSimpleDirections() {
        Set<Vector> vectors = new HashSet<>();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {

                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }

                    vectors.add(new Vector(x, y, z));
                }
            }
        }
        return vectors;
    }

    public static Vector getOrthogonalVector(Vector vector, int index) {
        return getOrthogonalVectors(vector).get(index);
    }

    public static List<Vector> getOrthogonalVectors(Vector vector) {
        List<Vector> vectors = new ArrayList<>();
        double x = vector.getX();
        double y = vector.getY();
        double z = vector.getZ();
        Vector v1 = new Vector(-y, x, 0);
        Vector v2 = new Vector(x * z, y * z, -(x * x + y * y));
        vectors.add(v1);
        vectors.add(v2);
        return vectors;
    }

    public static void setupSpectator(CTFUser user) {
        CTFUtil.teleportPlayerMidMap(user);
        getPlayer(user).setInvisible(true);
        setFly(getPlayer(user), true);
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

    public static <T> T getRandom(Collection<PotentialObject<T>> potentialObjects) {

        if (potentialObjects.isEmpty()) {
            return null;
        }

        LinkedList<PotentialObject<T>> linkedObjects = new LinkedList<>();
        for (PotentialObject<T> potentialMaterial : potentialObjects) {
            int lastPosition = linkedObjects.isEmpty() ? 0 : linkedObjects.getLast().getPosition();
            potentialMaterial.setPosition(lastPosition + potentialMaterial.getProbability());
            linkedObjects.add(potentialMaterial);
        }
        int randomPosition = ThreadLocalRandom.current().nextInt(0, linkedObjects.getLast().getPosition() + 1);
        T winner = linkedObjects.stream().filter(o -> randomPosition <= o.getPosition()).min(Comparator.comparingInt(PotentialObject::getPosition)).get().getContent();
        return winner;
    }

    public static Team getEnemyTeam(CTFUser user) {
        return user.getTeam().getColor() == TeamColor.BLUE ? CTF.getInstance().getBlue() : CTF.getInstance().getRed();
    }

    public static Zombie spawnAlibiPlayer(CTFUser user) {
        Player player = getPlayer(user);
        Zombie fakePlayer = player.getWorld().spawn(player.getLocation(), Zombie.class);
        fakePlayer.setInvulnerable(true);
        fakePlayer.setAI(false);
        fakePlayer.getEquipment().setArmorContents(player.getEquipment().getArmorContents());
        return fakePlayer;
    }

    public static void createExplosion(Location center, double radius, int power, Color... colors) {
        Firework firework = (Firework) center.getWorld().spawnEntity(center, EntityType.FIREWORK);
        FireworkMeta fireworkMeta = firework.getFireworkMeta();
        fireworkMeta.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BALL_LARGE).withColor(colors).build());
        fireworkMeta.setPower(0);
        firework.setFireworkMeta(fireworkMeta);
        firework.detonate();
        center.getWorld().playSound(center, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 2, 0);
        getNearbyUsers(center, radius).stream().forEach(user -> {
            Player player = getPlayer(user);
            Vector vector = player.getLocation().toVector().subtract(center.toVector());
            Vector push = vector.normalize().multiply(2).setY(0.5);
            player.damage(power);
            player.setVelocity(push);
        });
    }

    public static String wrap(String string, int lineLength) {
        StringBuilder b = new StringBuilder();
        for (String line : string.split(Pattern.quote(LINEBREAK))) {
            b.append(wrapLine(line, lineLength));
        }
        return b.toString();
    }

    public static CTFUser getUser(Player player) {
        return getUser(player.getUniqueId());
    }

    public static CTFUser getUser(UUID uuid) {
        return CTF.getInstance().getPlayerListener().getUserManager().getUser(uuid);
    }

    private static String wrapLine(String line, int lineLength) {
        if (line.length() == 0) return LINEBREAK;
        if (line.length() <= lineLength) return line + LINEBREAK;
        String[] words = line.split(" ");
        StringBuilder allLines = new StringBuilder();
        StringBuilder trimmedLine = new StringBuilder();
        for (String word : words) {
            if (trimmedLine.length() + 1 + word.length() <= lineLength) {
                trimmedLine.append(word).append(" ");
            } else {
                allLines.append(trimmedLine).append(LINEBREAK);
                trimmedLine = new StringBuilder();
                trimmedLine.append(word).append(" ");
            }
        }
        if (trimmedLine.length() > 0) {
            allLines.append(trimmedLine);
        }
        allLines.append(LINEBREAK);
        return allLines.toString();
    }

    public static List<Flag> getCapturedFlags(Team team) {
        return CTF.getInstance().getMap().getFlags().stream().filter(flag -> flag.getTeam() == team.getColor()).filter(flag -> flag.getStatus() == CaptureStatus.CAPTURED).toList();
    }

    public static Block getHighestBlock(int x, int z, World world) {
        Block highest = null;
        for (int y = 255; y >= 0; y--) {
            Block block = world.getBlockAt(x, y, z);
            if (block.getType().isSolid()) {
                highest = block;
                break;
            }
        }
        return highest;
    }

    public static Set<PotentialObject<Color>> mixColors(Color primary, Color secondary, Color tertiary, int p1, int p2, int p3) {
        return new HashSet<>(Arrays.asList(new PotentialObject<>(primary, p1), new PotentialObject<>(secondary, p2), new PotentialObject<>(tertiary, p3)));
    }

}
