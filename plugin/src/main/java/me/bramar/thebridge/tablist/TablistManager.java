package me.bramar.thebridge.tablist;

import com.google.common.base.Charsets;
import lombok.Getter;
import me.bramar.thebridge.TheBridge;
import me.bramar.thebridge.util.ModuleLogger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

@Getter
public class TablistManager {
    private final Logger logger = new ModuleLogger("TabList");
    private final TheBridge plugin = TheBridge.getInstance();
    private final Map<Integer, String> header = new HashMap<>();
    private final Map<Integer, String> footer = new HashMap<>();
    private final Map<UUID, TablistHeaderFooterTask> hfTasks = new HashMap<>();
    private final Map<UUID, TablistTask> tablistTasks = new HashMap<>();
    private int totalTick = 0;
    private boolean headerAvailable = false; // hasHeader
    private boolean footerAvailable = false; // hasFooter
    private boolean enabled = false;
    private int delayAfterTurn;
    private boolean hideSkin = false;
    private boolean showHealth = false;
    private String healthDisplayName = "HP";
    private String nameFormat;
    private boolean fakePing;
    private int fakePingMode; // constant = 0, randomize = 1
    private int[] fakePingValue;
    private boolean showDisconnectedAsSpectator;

    public TablistManager() {
        load();
    }
    public void load() {
        File file = new File(plugin.getDataFolder(), "tablist.yml");
        if(!file.exists())
            plugin.saveResource("tablist.yml", false);
        YamlConfiguration config = new YamlConfiguration();
        try {
            FileInputStream stream = new FileInputStream(file);
            config.load(new InputStreamReader(stream, Charsets.UTF_8));
        }catch(Exception e1) {
            e1.printStackTrace();
        }
        enabled = config.getBoolean("enabled", false);
        delayAfterTurn = config.getInt("delay-after-turn", 50);
        hideSkin = config.getBoolean("hide-skin", false);
        showHealth = config.getBoolean("show-health", true);
        healthDisplayName = config.getString("health-displayname", "HP");
        showDisconnectedAsSpectator = config.getBoolean("show-disconnected-as-spectator", true);
        fakePing = config.getBoolean("fake-ping.enabled", false);
        String fakePingMode = config.getString("fake-ping.mode", "").toLowerCase();
        switch(fakePingMode) {
            case "constant":
                this.fakePingMode = 0;
                break;
            case "randomize":
                this.fakePingMode = 1;
                break;
            default:
                break;
        }
        String fakePingValue = config.getString("fake-ping.value", "50").toLowerCase();
        try {
            if(this.fakePingMode == 1) {
                String[] split = fakePingValue.split("-");
                this.fakePingValue = new int[] {
                        Integer.parseInt(split[0]),
                        Integer.parseInt(split[1])
                };
            }else {
                this.fakePingValue = new int[] {Integer.parseInt(fakePingValue)};
            }
        }catch(NumberFormatException | ArrayIndexOutOfBoundsException e1) {
            this.fakePing = false;
        }
        nameFormat = config.getString("name-format", "%tb_team_color%{player}");
        AtomicInteger atomicInt = new AtomicInteger(0);
        this.headerAvailable = loadHF(header, config.getConfigurationSection("header"), atomicInt);
        this.footerAvailable = loadHF(footer, config.getConfigurationSection("footer"), atomicInt);
        // atomicInt is the highest tick across header and footer
        totalTick = atomicInt.get() + delayAfterTurn;
    }
    public boolean loadHF(Map<Integer, String> map, ConfigurationSection config, AtomicInteger highestTick) {
        if(config == null)
            return false;
        boolean added = false;
        for(String key : config.getKeys(false)) {
            if(config.isList(key)) {
                int tick;
                try {
                    tick = Integer.parseInt(key);
                }catch(NumberFormatException e1) {
                    continue;
                }
                List<String> list = config.getStringList(key);
                if(list == null) continue;
                StringBuilder stringBuilder = new StringBuilder();
                for(int i = 0; i < list.size(); i++) {
                    stringBuilder.append(list.get(i));
                    if(i != (list.size() - 1))
                        stringBuilder.append('\n');
                }
                if(tick > highestTick.get())
                    highestTick.set(tick);
                map.put(tick, stringBuilder.toString());
                added = true;
            }
        }
        return added;
    }
    public void showTablist(UUID uuid, List<UUID> players, Map<UUID, PlayerInfoData> playersData) {
        if(enabled) {
            if(!tablistTasks.containsKey(uuid)) {
                TablistTask task = new TablistTask(this, uuid, players, playersData);
                task.register();
                task.initPackets();
                tablistTasks.put(uuid, task);
            }
            if(!hfTasks.containsKey(uuid)) {
                TablistHeaderFooterTask hfTask = new TablistHeaderFooterTask(this, uuid);
                hfTask.runTaskTimer(plugin, 1, 1);
                hfTasks.put(uuid, hfTask);
            }
        }
    }
    public void hideTablist(UUID uuid) {
        if(enabled) {
            TablistTask task = tablistTasks.get(uuid);
            if(task != null) {
                task.unregister();
                task.resetTablist();
                tablistTasks.remove(uuid);
            }
            TablistHeaderFooterTask hfTask = hfTasks.get(uuid);
            if(hfTask != null) {
                hfTask.cancel();
                hfTask.resetTablist();
                hfTasks.remove(uuid);
            }
        }
    }
}
