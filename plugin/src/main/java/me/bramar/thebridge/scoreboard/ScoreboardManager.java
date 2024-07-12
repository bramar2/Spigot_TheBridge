package me.bramar.thebridge.scoreboard;

import com.google.common.base.Charsets;
import lombok.Getter;
import me.bramar.thebridge.TheBridge;
import me.bramar.thebridge.util.ModuleLogger;
import me.bramar.thebridge.util.ScoreboardWrapper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;


@Getter
public class ScoreboardManager {
    private boolean enabled;
    private boolean spectatorEnabled;
    private int delayAfterTurn;
    private int totalTick = 0;
    private int spectatorTotalTick = 0;
    private ScoreboardTick first = null;
    private ScoreboardTick spectatorFirst = null;
    private final Map<Integer, ScoreboardTick> scoreboard = new HashMap<>();
    private final Map<Integer, ScoreboardTick> spectatorScoreboard = new HashMap<>();
    private final Map<UUID, ScoreboardTask> scoreboardTasks = new HashMap<>();

    private final Logger logger = new ModuleLogger("Scoreboard");
    private final TheBridge plugin = TheBridge.getInstance();

    public ScoreboardManager() {
        load();
    }
    public void showScoreboard(Player p) {
        if(enabled) {
            UUID uuid = p.getUniqueId();
            if(!scoreboardTasks.containsKey(uuid)) {
                ScoreboardTask task = new ScoreboardTask(p, first.getTitle(), false);
                int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, task, 1, 1);
                task.setTaskId(taskId);
                scoreboardTasks.put(uuid, task);
            }
            p.setScoreboard(scoreboardTasks.get(uuid).getScoreboard());
        }
    }
    public void showSpectatorScoreboard(Player p) {
        if(spectatorEnabled) {
            UUID uuid = p.getUniqueId();
            if(!scoreboardTasks.containsKey(uuid)) {
                ScoreboardTask task = new ScoreboardTask(p, first.getTitle(), true);
                int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, task, 1, 1);
                task.setTaskId(taskId);
                scoreboardTasks.put(uuid, task);
            }
            p.setScoreboard(scoreboardTasks.get(uuid).getScoreboard());
        }
    }
    public void hideScoreboard(UUID uuid, Player p) {
        if(enabled) {
            if(scoreboardTasks.containsKey(uuid)) {
                Bukkit.getScheduler().cancelTask(scoreboardTasks.get(uuid).getTaskId());
                scoreboardTasks.remove(uuid);
            }
            if(p != null)
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }
    public void load() {
        File file = new File(plugin.getDataFolder(), "scoreboard.yml");
        if(!file.exists())
            plugin.saveResource("scoreboard.yml", false);
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            FileInputStream stream = new FileInputStream(file);
            yaml.load(new InputStreamReader(stream, Charsets.UTF_8));
        }catch(Exception e1) {
            e1.printStackTrace();
        }


        enabled = yaml.getBoolean("enabled", true);
        spectatorEnabled = yaml.getBoolean("spectator-scoreboard-enabled", true);
        loadScoreboard(yaml.getConfigurationSection("ticks"), false);
        loadScoreboard(yaml.getConfigurationSection("spectator-ticks"), true);

        if(enabled && scoreboard.isEmpty()) {
            enabled = false;
            logger.info("Scoreboard is empty, disabling it.");
        }
        if(spectatorEnabled && spectatorScoreboard.isEmpty()) {
            spectatorEnabled = false;
            logger.info("Spectator's scoreboard is empty, disabling it.");
        }
        // set %same% with previous
        setSamePlaceholder(false);
        setSamePlaceholder(true);

        delayAfterTurn = yaml.getInt("delay-after-turn", 50);
        totalTick += delayAfterTurn;
        logger.info("Loaded scoreboard.");
    }
    public void setSamePlaceholder(boolean spectator) {
        Map<Integer, ScoreboardTick> scoreboard = spectator ? this.spectatorScoreboard : this.scoreboard;
        String[] previousContent = new String[ScoreboardWrapper.MAX_LINES];
        String previousTitle = null;
        List<Integer> tickList = new ArrayList<>(scoreboard.keySet());
        Collections.sort(tickList);
        for(int tickN : tickList) {
            ScoreboardTick tick = scoreboard.get(tickN);
            List<String> content = tick.getContent();
            for(int i = 0; i < content.size(); i++) {
                String line = content.get(i);
                if(line.equalsIgnoreCase("%same%")) {
                    if(previousContent[i] != null)
                        content.set(i, previousContent[i]);
                }else {
                    previousContent[i] = line;
                }
            }
            String title = tick.getTitle();
            if(title.equalsIgnoreCase("%same%")) {
                if(previousTitle != null) {
                    tick.setTitle(previousTitle);
                    previousTitle = title;
                }
            }else {
                previousTitle = title;
            }
        }
    }
    public void loadScoreboard(ConfigurationSection ticks, boolean spectator) {
        int firstIndex = -1;
        Set<Integer> d = new HashSet<>(); // to filter duplicates
        for(String key : ticks.getKeys(false)) {
            if(ticks.isList(key)) {
                try {
                    List<String> list = ticks.getStringList(key);
                    if(list == null || list.size() < 3) continue;

                    int i = Integer.parseInt(key);
                    if(i < 0) continue;
                    if(!d.add(i)) continue; // duplicate

                    if(i > (spectator ? spectatorTotalTick : totalTick)) {
                        if(spectator) spectatorTotalTick = i;
                        else totalTick = i;
                    }

                    String title = list.get(0);
                    List<String> lines = new ArrayList<>();
                    for(int j = 2; j < list.size(); j++) {
                        String k = list.get(j);
                        if(k.replace(" ", "").isEmpty()) {
                            k = "";
                            for(int l = 0; l < j - 1; l++) {
                                k += ChatColor.RESET;
                            }
                        }
                        lines.add(k);
                    }
                    boolean sameContent = lines.get(2).equalsIgnoreCase("%same_all%");
                    // Convert unicode literal strings into unicodes
//                    title = StringEscapeUtils.unescapeJava(title);
//                    lines = lines.stream().map(StringEscapeUtils::unescapeJava).collect(Collectors.toList());

                    ScoreboardTick tick = new ScoreboardTick(title, lines, sameContent);

                    if(i < firstIndex || firstIndex == -1) {
                        firstIndex = i;
                        if(spectator) spectatorFirst = tick;
                        else first = tick;
                    }

                    if(spectator) spectatorScoreboard.put(i, tick);
                    else scoreboard.put(i, tick);
                }catch(Exception ignored) {}
            }
        }
    }
}
