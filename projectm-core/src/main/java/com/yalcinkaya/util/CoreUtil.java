package com.yalcinkaya.util;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;


public class CoreUtil {

    public static final Set<Vector> simpleDirections = new HashSet<>(getSimpleDirections());

    public static final String LINEBREAK = "\n"; // or "\r\n";

    public static String getMessage(MessageType messageType, String... strings) {
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
        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(CoreUtil.getMessage(messageType, strings)));
    }

    public static String wrap(String string, int lineLength) {
        StringBuilder b = new StringBuilder();
        for (String line : string.split(Pattern.quote(LINEBREAK))) {
            b.append(wrapLine(line, lineLength));
        }
        return b.toString();
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

    public static int slotFromRowCol(int row, int col) {
        return row * 9 + col;
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

    public static Location getCenter(Location location) {
        double adjustX = location.getX() < 0 ? -0.5 : 0.5;
        double adjustZ = location.getZ() > 0 ? -0.5 : 0.5;
        return location.clone().add(adjustX, 0, adjustZ);
    }

    public static String camelizeString(String string) {
        return string.substring(0, 1).toUpperCase() + string.substring(1).toLowerCase();
    }
    
}
