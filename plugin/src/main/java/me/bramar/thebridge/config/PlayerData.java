package me.bramar.thebridge.config;

import lombok.AccessLevel;
import lombok.Getter;
import me.bramar.thebridge.TheBridge;
import me.bramar.thebridge.util.nms.NMSItemStack;
import me.bramar.thebridge.util.nms.NMSUtil;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static me.bramar.thebridge.util.CompatibilityUtils.getStorageContents;
import static me.bramar.thebridge.util.CompatibilityUtils.setStorageContents;

@Getter
public class PlayerData implements ConfigurationSerializable {
    public static final int VERSION = 1;
    private final UUID uuid;
    private ItemStack[] inventory;
    private boolean isDefault;
    private int arrowSlot = -99;
    @Getter(AccessLevel.NONE)
    private boolean ran = false;

    private final PlayerDatabase playerDatabase = TheBridge.getInstance().getPlayerDatabase();
    private final NMSUtil nms = TheBridge.getInstance().getNMSUtil();

    public PlayerData(UUID uuidKey) {
        this.uuid = uuidKey;
    }

    public void save() {
        File file = new File(TheBridge.getInstance().getDataFolder(), "playerdata/" + uuid + ".yml");
        if(!file.getParentFile().exists()) {
            if(!file.mkdirs()) {
                playerDatabase.getLogger().warning("Failed to create directory and save!");
                return;
            }
        }
        try {
            YamlConfiguration config = new YamlConfiguration();
            config.set("version", VERSION);
            config.set("data", this);
            config.save(file);
        }catch(Exception e1) {
            e1.printStackTrace();
            playerDatabase.getLogger().warning("Failed to create player data file!");
        }
    }

    public boolean hasArrowSlot() {
        return arrowSlot != -99;
    }

    public ItemStack[] formatInventory(int teamNumber) {
        TeamConfig teamSettings = TheBridge.getInstance().getBridgeConfig().getTeamSettings(teamNumber);
        ItemStack[] inventory = this.inventory.clone();
        for(int i = 0; i < inventory.length; i++) {
            ItemStack item = inventory[i];
            if(item == null)
                continue;
            NMSItemStack nmsItem = nms.getItem(inventory[i]);
            if(!nmsItem.hasTag())
                continue;
            String bp = nmsItem.getTag().getString("bridgeproperty");
            bp = bp == null ? "" : bp;
            if(bp.equalsIgnoreCase("block")) {
                inventory[i] = teamSettings.getBlock().clone();
            }
            else if(!hasArrowSlot() && bp.equalsIgnoreCase("arrow")) {
                if(!ran) {
                    this.inventory[i].setType(Material.ARROW); // turn type to arrow
                    this.inventory[i].setAmount(1);
                    MaterialData md = inventory[i].getData();
                    md.setData((byte) 0);
                    this.inventory[i].setData(md);
                    inventory[i] = this.inventory[i];
                }
                arrowSlot = i;
            }
        }
        ran = true;
        return inventory;
    }

    public void giveArrowOnPlayer(Player player) {
//            ItemStack[] playerInventory = player.getInventory().getStorageContents();
//            ItemStack[] configInventory = formatInventory(0); // team doesn't matter, its arrow
//            playerInventory[arrowSlot] = configInventory[arrowSlot];
//            player.getInventory().setStorageContents(playerInventory);
        if(!ran)
            formatInventory(0); // load

        ItemStack arrow = hasArrowSlot() ? inventory[arrowSlot] : new ItemStack(Material.ARROW);
        if(TheBridge.getInstance().getBridgeConfig().isArrowRefillUsingGive()) {
            player.getInventory().addItem(arrow);
        }else {
            ItemStack[] playerInventory = getStorageContents(player);
            playerInventory[arrowSlot] = arrow;
            setStorageContents(player, playerInventory);
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("uuid", uuid.toString());
        map.put("inventory", inventory);
        map.put("default", isDefault);
        return map;
    }
    public static PlayerData deserialize(Map<String, Object> map) {
        UUID uuid = UUID.fromString((String) map.get("uuid"));
        PlayerData p = new PlayerData(uuid);
        Object inv = map.get("inventory");
        if(inv.getClass().isArray())
            p.inventory = (ItemStack[]) inv;
        else
            p.inventory = ((Collection<ItemStack>) inv).toArray(new ItemStack[0]);
        p.isDefault = (boolean) map.get("default");
        return p;
    }

    public PlayerData copyAs(@Nullable UUID uuid) {
        PlayerData p = new PlayerData(uuid == null ? this.uuid : uuid);
        p.inventory = inventory;
        p.isDefault = isDefault;
        return p;
    }
}