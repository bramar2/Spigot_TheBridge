package me.bramar.thebridge.model;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.HashMap;
import java.util.Map;

@Getter
public class BoundingBox implements ConfigurationSerializable {
    private final int minX, maxX;
    private final int minZ, maxZ;

    private Integer minY = null, maxY = null;

    public BoundingBox(int minX, Integer minY, int minZ, int maxX, Integer maxY, int maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;

        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;

    }

    public BoundingBox(Location pos1, Location pos2) {
        this(pos1, pos2, true);
    }

    public BoundingBox(Location loc1, Location loc2, boolean includeY) {
        if(loc1 == null || loc2 == null)
            throw new NullPointerException("loc1 and loc2 must be not-null");
        minX = Math.min(loc1.getBlockX(), loc2.getBlockX());
        maxX = Math.max(loc1.getBlockX(), loc2.getBlockX());
        minZ = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        maxZ = Math.max(loc1.getBlockZ(), loc2.getBlockZ());

        if(includeY) {
            minY = Math.min(loc1.getBlockY(), loc2.getBlockY());
            maxY = Math.max(loc1.getBlockY(), loc2.getBlockY());
        }
    }

    public boolean contains(Location loc) {
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        // Repeated for Y and Z
        if(!(x == minX || x == maxX)) {
            // If equals minX or maxX, it's in box
            if(!(minX < x && x < maxX)) {
                // If x is above minX, and below maxX, it passes
                // OR if minX < x < maxX (in range)
                // else returns
                return false;
            }
        }
        if(!(z == minZ || z == maxZ)) {
            if(!(minZ < z && z < maxZ)) {
                return false;
            }
        }
        if(minY != null && maxY != null) {
            if(!(y == minY || y == maxY)) {
                if(!(minY < y && y < maxY)) {
                    return false;
                }
            }
        }
        return true;
    }
    public boolean inRangeX(int x) {
        return x >= minX && x <= maxX;
    }
    public boolean inRangeY(int y) {
        if(this.minY == null || this.maxY == null)
            return true;
        return y >= minY && y <= maxY;
    }
    public boolean inRangeZ(int z) {
        return z >= minZ || z <= maxZ;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> minMap = new HashMap<>();
        Map<String, Object> maxMap = new HashMap<>();
        Map<String, Object> map = new HashMap<>();

        minMap.put("x", minX);
        minMap.put("y", minY);
        minMap.put("z", minZ);

        maxMap.put("x", maxX);
        maxMap.put("y", maxY);
        maxMap.put("z", maxZ);

        map.put("min", minMap);
        map.put("max", maxMap);
        return map;
    }
    public static BoundingBox deserialize(Map<String, Object> map) {
        Map<String, Object> minMap = (Map<String, Object>) map.get("min");
        Map<String, Object> maxMap = (Map<String, Object>) map.get("max");

        Integer minY = (Integer) minMap.get("y");
        Integer maxY = (Integer) maxMap.get("y");

        return new BoundingBox(
                (int) minMap.get("x"), minY, (int) minMap.get("z"),
                (int) maxMap.get("x"), maxY, (int) maxMap.get("z")
        );
    }
}