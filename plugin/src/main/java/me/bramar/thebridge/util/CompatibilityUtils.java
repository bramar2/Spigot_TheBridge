package me.bramar.thebridge.util;

import com.google.common.base.Preconditions;
import me.bramar.thebridge.TheBridge;
import org.apache.commons.lang.Validate;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

// Class is designed to replace unavailable methods (in older versions)
public class CompatibilityUtils {
    private static Object configGetDefault(MemorySection config, String path) {
        Validate.notNull(path, "Path cannot be null");
        Configuration root = config.getRoot();
        Configuration defaults = root == null ? null : root.getDefaults();
        return defaults == null ? null : defaults.get(createPath(config, path));
    }
    public static String createPath(ConfigurationSection section, String key) {
        return createPath(section, key, section == null ? null : section.getRoot());
    }
    public static int mcVersion() {
        return Integer.parseInt(TheBridge.getInstance().getNMSVersion().split("_")[1]);
    }

    public static String createPath(ConfigurationSection section, String key, ConfigurationSection relativeTo) {
        Validate.notNull(section, "Cannot create path without a section");
        Configuration root = section.getRoot();
        if (root == null) {
            throw new IllegalStateException("Cannot create path without a root");
        } else {
            char separator = root.options().pathSeparator();
            StringBuilder builder = new StringBuilder();
            if (section != null) {
                for(ConfigurationSection parent = section; parent != null && parent != relativeTo; parent = parent.getParent()) {
                    if (builder.length() > 0) {
                        builder.insert(0, separator);
                    }

                    builder.insert(0, parent.getName());
                }
            }

            if (key != null && key.length() > 0) {
                if (builder.length() > 0) {
                    builder.append(separator);
                }

                builder.append(key);
            }
            return builder.toString();
        }
    }
    public static <T extends ConfigurationSerializable> T getSerializable(MemorySection config, String path, Class<T> clazz) {
        // "v1_12_R3" --> "12"
        int mcVersion = mcVersion();
        if(mcVersion < 12) {
            Validate.notNull(clazz, "ConfigurationSerializable class cannot be null");
            Object def = configGetDefault(config, path);
            return getSerializable(config, path, clazz, clazz.isInstance(def) ? clazz.cast(def) : null);
        }else {
            return config.getSerializable(path, clazz);
        }
    }
    public static <T extends ConfigurationSerializable> T getSerializable(MemorySection config, String path, Class<T> clazz, T def) {
        int mcVersion = mcVersion();
        if(mcVersion < 12) {
            Validate.notNull(clazz, "ConfigurationSerializable class cannot be null");
            Object val = config.get(path);
            return clazz.isInstance(val) ? clazz.cast(val) : def;
        }else {
            return config.getSerializable(path, clazz, def);
        }
    }
    private static void invSetSlots(PlayerInventory playerInventory, ItemStack[] items, int baseSlot, int length) {
        if(items == null) {
            items = new ItemStack[length];
        }
        if(!(items.length <= length))
            throw new IllegalArgumentException("items.length must be < " + length);

        for(int i = 0; i < length; ++i) {
            if(i >= items.length) {
                playerInventory.setItem(baseSlot + i, null);
            }else {
                playerInventory.setItem(baseSlot + i, items[i]);
            }
        }

    }
    public static void setStorageContents(Player p, ItemStack[] content) {
        if(mcVersion() >= 9) {
            p.getInventory().setStorageContents(content);
        }else {
            invSetSlots(p.getInventory(), content, 0, p.getInventory().getSize());
        }
    }
    public static ItemStack[] getStorageContents(Player p) {
        if(mcVersion() >= 9) {
            return p.getInventory().getStorageContents();
        }else {
            return TheBridge.getInstance().getNMSUtil().legacyGetStorageContents(p);
        }
    }
    public static void setUnbreakable(ItemMeta meta, boolean flag) {
        if(mcVersion() >= 11) {
            meta.setUnbreakable(flag);
        }else {
            meta.spigot().setUnbreakable(true);
        }
    }
    private static Arrow.PickupStatus pickupEnum(int flag) {
        switch(flag) {
            case 0:
                return Arrow.PickupStatus.DISALLOWED;
            case 1:
                return Arrow.PickupStatus.ALLOWED;
            case 2:
                return Arrow.PickupStatus.CREATIVE_ONLY;
            default:
                throw new IllegalArgumentException("value can only be 0 - 2");
        }
    }
    public static void setPickupStatus(Arrow arrow, int flag) {
        if(mcVersion() >= 12) {
            arrow.setPickupStatus(pickupEnum(flag));
        }else {
            TheBridge.getInstance().getNMSUtil().legacySetPickupStatus(arrow, flag);
        }
    }

    public static boolean isModernSendTitle() {
        return mcVersion() >= 11;
    }

    public static boolean arrowPickupAvailable() {
        try {
            Class.forName("org.bukkit.event.player.PlayerPickupArrowEvent");
            return true;
        }catch(Exception ignored) {}
        return false;
    }

    public static boolean entityItemPickupAvailable() {
        try {
            Class.forName("org.bukkit.event.entity.EntityPickupItemEvent");
            return true;
        }catch(Exception ignored) {}
        return false;
    }

    public static boolean hasSoundCategory() {
        try {
            Class.forName("org.bukkit.SoundCategory");
            return true;
        }catch(Exception ignored) {}
        return false;
    }
}
