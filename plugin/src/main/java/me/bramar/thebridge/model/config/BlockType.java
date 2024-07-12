package me.bramar.thebridge.model.config;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Material;

@Getter @AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BlockType {
    private final Material material;
    private final int id;
    private final int data;

    public static BlockType create(String combined) {
        String[] split = combined.split(":");
        int data = -1;
        if(split.length != 1) {
            try {
                int data0 = Integer.parseInt(split[1]);
                if(data0 >= 0 && data0 <= 16)
                    data = data0;
            }catch(Exception ignored) {}
        }
        Material material = null;
        try {
            material = Material.valueOf(split[0].toUpperCase());
        }catch(Exception ignored) {}
        if(material == null)
            try {
                material = Material.getMaterial(Integer.parseInt(split[0]));
            }catch(Exception ignored) {}

        if(material == null)
            return null;

        return new BlockType(material, material.getId(), data);
    }
    public boolean correct(Material mat, int data) {
        return correct(mat.getId(), data);
    }
    public boolean correct(int id, int data) {
        return id == this.id && (this.data == -1 || data == this.data);
    }

    @Override
    public String toString() {
        return material + ":" + data;
    }
}
