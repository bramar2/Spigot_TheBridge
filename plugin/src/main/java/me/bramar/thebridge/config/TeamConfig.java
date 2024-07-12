package me.bramar.thebridge.config;

import lombok.AccessLevel;
import lombok.Getter;
import me.bramar.thebridge.TheBridge;
import org.bukkit.ChatColor;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

import static me.bramar.thebridge.util.CompatibilityUtils.setUnbreakable;

@Getter
public class TeamConfig implements ConfigurationSerializable {
    public static final int VERSION = 1;
    private String colorPrefix;
    private String name;
    private ItemStack block;
    private ItemStack[] armor;

    @Getter(AccessLevel.NONE)
    private final BridgeConfig bcConfig = TheBridge.getInstance().getBridgeConfig();

    private TeamConfig() {}


    public void giveArmorToPlayer(Player p) {
        ItemStack[] armor = this.armor.clone();
        for(int i = 0; i < armor.length; i++) {
            ItemStack item = armor[i];
            if(item == null)
                continue;
            ItemMeta meta = item.getItemMeta();
            setUnbreakable(meta, true);
            item.setItemMeta(meta);
            armor[i] = item;
        }
        p.getInventory().setArmorContents(armor);
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("version", VERSION);
        map.put("color-prefix", colorPrefix.replace('&', ChatColor.COLOR_CHAR));
        map.put("name", name);
        map.put("block", block);
        map.put("armor", Collections.singletonList(block));
        return map;
    }
    public static TeamConfig deserialize(Map<String, Object> map) {
        TeamConfig teamConfig = new TeamConfig();
        teamConfig.colorPrefix = ChatColor.translateAlternateColorCodes('&', (String) map.get("color-prefix"));
        teamConfig.name = (String) map.get("name");
        teamConfig.block = (ItemStack) map.get("block");
        teamConfig.armor = ((List<ItemStack>) map.get("armor")).toArray(new ItemStack[0]);
        return teamConfig;
    }

    @Override
    public String toString() {
        return "TeamConfig{" +
                "colorPrefix='" + colorPrefix + '\'' +
                ", name='" + name + '\'' +
                ", block=" + block +
                ", armor=" + Arrays.toString(armor) +
                '}';
    }
}
