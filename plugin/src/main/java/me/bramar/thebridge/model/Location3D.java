package me.bramar.thebridge.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;

@Getter
@AllArgsConstructor
public final class Location3D {
    private final double x, y, z;
    private final float yaw, pitch;
    public Location3D(Location loc) {
        this(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
    }
    public int getBlockX() {
        return Location.locToBlock(x);
    }
    public int getBlockY() {
        return Location.locToBlock(y);
    }
    public int getBlockZ() {
        return Location.locToBlock(z);
    }

    public Location toBukkit(World world) {
        return new Location(world, x, y, z, yaw, pitch);
    }
}
