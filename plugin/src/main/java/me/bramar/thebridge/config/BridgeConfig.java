package me.bramar.thebridge.config;

import com.google.common.base.Charsets;
import lombok.Getter;
import me.bramar.thebridge.TheBridge;
import me.bramar.thebridge.model.config.BlockType;
import me.bramar.thebridge.model.config.ColorFormat;
import me.bramar.thebridge.model.config.SoundType;
import me.bramar.thebridge.model.config.TitleTime;
import me.bramar.thebridge.util.ModuleLogger;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static me.bramar.thebridge.util.CompatibilityUtils.getSerializable;

@Getter
public class BridgeConfig {
    public static final int CONFIG_VERSION = 1;
    public static final int SETTINGS_VERSION = 1;
    private final TheBridge plugin = TheBridge.getInstance();
    private boolean absorptionOnFirstGap;
    private TeamConfig teamOneSettings;
    private TeamConfig teamTwoSettings;
    private Location spawnLocation;
    private List<BlockType> allowBreak;
    private boolean onlyBreakPlacedBlocks;
    private int placeBoxShrink;

    private int gameTime;
    private int bowCooldown;
    private int goalCount;
    private boolean friendlyFire;
    private boolean bowBoosting;
    private long combatTagDamagerExpiry;
    private long combatTagCauseExpiry;
    private @Nullable Integer arrowKbStrength;

    private boolean noDrop;
    private boolean noBlockLootDrop;
    private boolean noPickup;
    private boolean noSlotChangeArmor;
    private boolean noSlotChangeAll;
    private boolean noSlotChangeArrow;
    private boolean arrowRefillUsingGive;
    private boolean actionbarArrowCooldown;

    private GameMode gamemodeInGame;
    private GameMode gamemodeInBox;
    private GameMode gamemodeFinished;
    private GameMode gamemodeAfterGame;

    private boolean gameChat;
    private String gameChatFormat;

    private boolean teleportSpawnAfterLeave;
    private boolean clearInventoryAfterGame;
    private boolean teleportSpawnAfterGame;
    private boolean fireworks;

    private SoundType queueCountdownSound;
    private SoundType cageCountdownSound;
    private SoundType cageOpenSound;
    private SoundType killSound;

    private TitleTime cageTitleTime;
    private TitleTime cageFightTitleTime;
    private TitleTime queueTitleTime;
    private TitleTime winTitleTime;

    private boolean winTitleEnabled;
    private boolean winMessageEnabled;
    private boolean queueTitleEnabled;
    private boolean queueMessageEnabled;
    private boolean cageFightTitleEnabled;
    private boolean cageFightMessageEnabled;
    private boolean cageTitleEnabled;
    private boolean cageMessageEnabled;
    private boolean timeLimitMessageEnabled;

    private ColorFormat queueTitleColorFormat;
    private ColorFormat queueMessageColorFormat;
    private ColorFormat cageTitleColorFormat;
    private ColorFormat cageMessageColorFormat;

    private boolean showHealthBelowName;
    private String healthScoreboardName;

    private boolean unlimitedArenas;


    private final File configFile = new File(plugin.getDataFolder(), "config.yml");
    private final File settingsFile = new File(plugin.getDataFolder(), "settings.yml");
    private final Logger logger = new ModuleLogger("Config");

    public BridgeConfig() {
        load();
    }

    public void load() {
        if(!configFile.exists())
            plugin.saveResource("config.yml", false);
        YamlConfiguration config = new YamlConfiguration();
        try {
            FileInputStream stream = new FileInputStream(configFile);
            config.load(new InputStreamReader(stream, Charsets.UTF_8));
        }catch(Exception e1) {
            e1.printStackTrace();
        }
        deserialize(config);
        logger.info("Game config loaded.");

        if(!settingsFile.exists())
            plugin.saveResource("settings.yml", false);

        config = new YamlConfiguration();
        try {
            FileInputStream stream = new FileInputStream(settingsFile);
            config.load(new InputStreamReader(stream, Charsets.UTF_8));
        }catch(Exception e1) {
            e1.printStackTrace();
        }
        deserializeSettings(config);

        logger.info("Team config loaded.");
        logger.info("Successfully loaded.");
    }
    public TeamConfig getTeamSettings(int n) {
        if(n == 0)
            return teamOneSettings;
        else if(n == 1)
            return teamTwoSettings;
        return null;
    }
    public void save() throws IOException {
        YamlConfiguration config = new YamlConfiguration();
        serialize(config);
        config.save(configFile);
        YamlConfiguration tsConfig = new YamlConfiguration();
        serializeSettings(tsConfig);
        tsConfig.save(settingsFile);
    }

    public boolean isArrowRefillUsingGive() {
        // if allows changing arrow slot, it uses /give
        return !noSlotChangeArrow ? true : arrowRefillUsingGive;
    }

    public void serialize(YamlConfiguration config) {
        // UNFINISHED, because config.yml is NOT meant
        // to be saved/replaced with another
        config.set("version", CONFIG_VERSION);
        config.set("absorption-only-on-first-gap", absorptionOnFirstGap);
        config.set("only-break-placed-blocks", onlyBreakPlacedBlocks);
        config.set("allow-break", allowBreak.stream().map(BlockType::toString).collect(Collectors.toList()));

        config.set("bow-cooldown", bowCooldown);
        config.set("goal", goalCount);
        config.set("no-drop", noDrop);
        config.set("no-slot-change-armor", noSlotChangeArmor);
        config.set("no-slot-change-all", noSlotChangeAll);
        config.set("no-slot-change-arrow", noSlotChangeArrow);
        config.set("arrow-refill-using-give", arrowRefillUsingGive);
        config.set("actionbar-arrow-cooldown", actionbarArrowCooldown);
        config.set("custom-game-chat", gameChat);
        config.set("game-chat-format", gameChatFormat);
        config.set("clear-inv-after-game", clearInventoryAfterGame);
//        config.set("tp-to-spawn-after-game", );
//        config.set("do-end-actions-after-disconnect", );
//        config.set("sound-queue-countdown", );
//        config.set("sound-cage-open", );
//        config.set("sound-cage-countdown", );
//        config.set("sound-kill", );
//        config.set("fireworks", );
//        config.set("cage-title-time", );
//        config.set("cage-fight-title-time", );
//        config.set("queue-title-time", );
//        config.set("win-title-time", );
//        config.set("enable-cage-title", );
//        config.set("enable-cage-message", );
//        config.set("enable-cage-fight-title", );
//        config.set("enable-cage-fight-message", );
//        config.set("enable-queue-title", );
//        config.set("enable-queue-message", );
//        config.set("enable-win-title", );
//        config.set("enable-win-message", );
//        config.set("friendly-fire", );
//        config.set("self-bow-boost", );
//        config.set("arrow-kb-strength", );
//        config.set("game-time-ticks", );
//        config.set("queue-title-color-format", );
//        config.set("queue-message-color-format", );
//        config.set("cage-title-color-format", );
//        config.set("cage-message-color-format", );

    }
    public void serializeSettings(YamlConfiguration config) {
        config.set("version", SETTINGS_VERSION);
        config.set("team.1", teamOneSettings);
        config.set("team.2", teamTwoSettings);
        config.set("spawn-location", spawnLocation);
    }
    public void deserialize(YamlConfiguration config) {
        YamlConfiguration defaultsConfig = YamlConfiguration.loadConfiguration(
                new BufferedReader(new InputStreamReader(TheBridge.getInstance().getResource("config.yml"),
                        StandardCharsets.UTF_8))
        );
        config.setDefaults(defaultsConfig);

        absorptionOnFirstGap = config.getBoolean("game.absorption-only-on-first-gap");
        onlyBreakPlacedBlocks = config.getBoolean("game.only-break-placed-blocks");
        allowBreak = removeNull(config.getStringList("game.allow-break").stream().map(BlockType::create).collect(Collectors.toList()));
        placeBoxShrink = config.getInt("game.place-box-shrink", 0);
        bowCooldown = config.getInt("game.bow-cooldown");
        goalCount = config.getInt("game.goal");
        noDrop = config.getBoolean("game.no-drop");
        noBlockLootDrop = config.getBoolean("game.no-block-loot-drop");
        noPickup = config.getBoolean("game.no-pickup");
        noSlotChangeArmor = config.getBoolean("game.no-slot-change-armor");
        noSlotChangeAll = config.getBoolean("game.no-slot-change-all");
        noSlotChangeArrow = config.getBoolean("game.no-slot-change-arrow");
        // if allows changing arrow slot, it uses /give
//        arrowRefillUsingGive = !noSlotChangeArrow ? true : config.getBoolean("game.arrow-refill-using-give");
        arrowRefillUsingGive = !noSlotChangeArrow || config.getBoolean("game.arrow-refill-using-give");
        actionbarArrowCooldown = config.getBoolean("game.actionbar-arrow-cooldown");
        gameChat = config.getBoolean("game.custom-chat");
        gameChatFormat = config.getString("game.chat-format");
        gameTime = config.getInt("game.time-limit");

        gamemodeInGame = get(config, GameMode::valueOf, "game.gamemode.in-game");
        gamemodeInBox = get(config, GameMode::valueOf, "game.gamemode.in-box");
        gamemodeFinished = get(config, GameMode::valueOf, "game.gamemode.finished");
        gamemodeAfterGame = get(config, GameMode::valueOf, "game.gamemode.after-game");

        friendlyFire = config.getBoolean("game.friendly-fire");
        bowBoosting = config.getBoolean("game.self-bow-boost");
        fireworks = config.getBoolean("game.fireworks");
        if(config.isInt("game.arrow-kb-strength")) {
            this.arrowKbStrength = config.getInt("game.arrow-kb-strength");
        }else
            this.arrowKbStrength = null;

        clearInventoryAfterGame = config.getBoolean("after-game.clear-inv");
        teleportSpawnAfterGame = config.getBoolean("after-game.tp-to-spawn");

        queueCountdownSound = get(config, SoundType::create, "sound.queue-countdown");
        cageOpenSound = get(config, SoundType::create, "sound.cage-open");
        cageCountdownSound = get(config, SoundType::create, "sound.cage-countdown");
        killSound = get(config, SoundType::create, "sound.kill");

        cageTitleTime = get(config, TitleTime::create, "title.cage.times");
        cageFightTitleTime = get(config, TitleTime::create, "title.cage-fight.times");
        queueTitleTime = get(config, TitleTime::create, "title.queue.times");
        winTitleTime = get(config, TitleTime::create, "title.win.times");

        cageTitleEnabled = config.getBoolean("title.cage.enabled");
        cageFightTitleEnabled = config.getBoolean("title.cage-fight.enabled");
        queueTitleEnabled = config.getBoolean("title.queue.enabled");
        winTitleEnabled = config.getBoolean("title.win.enabled");

        cageMessageEnabled = config.getBoolean("messages-enabled.cage");
        cageFightMessageEnabled = config.getBoolean("messages-enabled.cage-fight");
        queueMessageEnabled = config.getBoolean("messages-enabled.queue");
        winMessageEnabled = config.getBoolean("messages-enabled.win");
        timeLimitMessageEnabled = config.getBoolean("messages-enabled.reached-time-limit");

        queueTitleColorFormat = new ColorFormat(config.getConfigurationSection("color-format.queue-title"));
        queueMessageColorFormat = new ColorFormat(config.getConfigurationSection("color-format.queue-message"));
        cageTitleColorFormat = new ColorFormat(config.getConfigurationSection("color-format.cage-title"));
        cageMessageColorFormat = new ColorFormat(config.getConfigurationSection("color-format.cage-message"));

        showHealthBelowName = config.getBoolean("show-health-below-name");
        healthScoreboardName = ChatColor.translateAlternateColorCodes('&',
                config.getString("health-scoreboard-name"));
        combatTagDamagerExpiry = config.getLong("combat-tag.damager-expiry");
        combatTagCauseExpiry = config.getLong("combat-tag.cause-expiry");

        unlimitedArenas = config.getBoolean("game.unlimited-arenas");
        teleportSpawnAfterLeave = config.getBoolean("teleport-to-spawn-after-leave");
    }
    private <T> T get(Configuration config, Function<String, T> constructor, String key) {
        return notNull(constructor.apply(config.getString(key)), ()->constructor.apply(config.getDefaults().getString(key)));
    }
    private <T> T notNull(T obj, Supplier<T> def) {
        return obj == null ? def.get() : obj;
    }
    private <T> List<T> removeNull(List<T> list) {
        list.remove(null);
        return list;
    }
    public void deserializeSettings(YamlConfiguration config) {
        teamOneSettings = getSerializable(config, "team.1", TeamConfig.class);
        teamTwoSettings = getSerializable(config, "team.2", TeamConfig.class);
        spawnLocation = getSerializable(config, "spawn-location", Location.class);
    }
}
