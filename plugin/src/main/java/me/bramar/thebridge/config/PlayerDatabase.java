package me.bramar.thebridge.config;

import com.google.common.base.Charsets;
import lombok.Getter;
import me.bramar.thebridge.TheBridge;
import me.bramar.thebridge.util.ModuleLogger;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static me.bramar.thebridge.util.CompatibilityUtils.getSerializable;

public class PlayerDatabase {
    private final Map<UUID, PlayerData> playerData = new HashMap<>();
    private PlayerData defaultPlayerData;

    private final TheBridge plugin = TheBridge.getInstance();
    @Getter private final Logger logger = new ModuleLogger("PlayerDB");

    public @NotNull PlayerData getPlayerData(UUID uuid) {
        if(!playerData.containsKey(uuid)) {
            fetchPlayerData(uuid);
        }
        return playerData.get(uuid);
    }
    private void fetchPlayerData(UUID uuid) {
        // this is so it only gets the needed player data and not all
        File file = new File(plugin.getDataFolder(), "/playerdata/" + uuid + ".yml");
        if(!file.exists()) {
            try {
                PlayerData pd = defaultPlayerData.copyAs(uuid);
                YamlConfiguration config = new YamlConfiguration();
                config.set("data", pd);
                config.save(file);
                playerData.put(uuid, pd);
            }catch(IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }else {
            YamlConfiguration config = new YamlConfiguration();
            try {
                FileInputStream stream = new FileInputStream(file);
                config.load(new InputStreamReader(stream, Charsets.UTF_8));
            }catch(Exception e1) {
                e1.printStackTrace();
            }
            PlayerData data = getSerializable(config, "data", PlayerData.class);
            if(data != null)
                playerData.put(uuid, data);
        }
    }
    public PlayerDatabase() {
        load();
    }
    public void load() {
        File def = new File(plugin.getDataFolder(), "default-playerdata.yml");
        if(!def.exists()) plugin.saveResource("default-playerdata.yml", false);
        YamlConfiguration a = new YamlConfiguration();
        try {
            FileInputStream stream = new FileInputStream(def);
            InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
            char[] buf = new char[2048];
            StringBuilder builder = new StringBuilder();
            int len;
            while((len = reader.read(buf)) > 0) {
                if(len != 2048)
                    builder.append(Arrays.copyOfRange(buf, 0, len));
                else
                    builder.append(buf);
            }
            reader.close();
            stream.close();
            a.loadFromString(ChatColor.translateAlternateColorCodes('&', builder.toString()));
        }catch(Exception e1) {
            e1.printStackTrace();
        }
        defaultPlayerData = getSerializable(a, "data", PlayerData.class, null);

        File dataDir = new File(plugin.getDataFolder(), "/playerdata");
        if(!dataDir.exists()) {
            if(!dataDir.mkdirs()) {
                logger.warning("Failed to create directory for player data.");
            }
        }
        logger.info("Successfully loaded.");
    }

}
