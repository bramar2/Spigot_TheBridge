package me.bramar.thebridge.arena;

import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Setter;
import me.bramar.thebridge.TheBridge;
import me.bramar.thebridge.config.BridgeConfig;
import me.bramar.thebridge.config.TeamConfig;
import me.bramar.thebridge.model.BoundingBox;
import me.bramar.thebridge.scoreboard.ScoreboardManager;
import me.bramar.thebridge.tablist.PlayerInfoData;
import me.bramar.thebridge.tablist.TablistManager;
import me.bramar.thebridge.util.schematic.BSchematic;
import me.bramar.thebridge.util.nms.NMSUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;

@Getter
public class BridgeTeam implements ConfigurationSerializable {
    private final TheBridge plugin = TheBridge.getInstance();
    private final ScoreboardManager scoreboardManager = plugin.getScoreboardManager();
    private final TablistManager tablistManager = plugin.getTablistManager();
    private final NMSUtil nms = plugin.getNMSUtil();
    private final BridgeConfig config = plugin.getBridgeConfig();

    private int minPlayers, maxPlayers;
    private final int teamNumber; // 0 or 1 (red or blue)
    private String color;
    private List<BoundingBox> goal; // this team's goal, might be more than 1 box
    //
    private BoundingBox spawnBox; // spawn a.k.a after a goal/after game starts

    private Location spawnLoc;
    //
    private Location respawnLoc; // after you die
    // when the game is started
    private final List<UUID> players;
    @Setter private int score = 0;
    @Setter private UUID arenaUniqueId;

    public BridgeTeam(UUID arenaUUID, int teamNumber) {
        this.arenaUniqueId = arenaUUID;
        minPlayers = maxPlayers = -1;
        this.teamNumber = teamNumber;
        players = new ArrayList<>();

        try {
            color = config.getTeamSettings(teamNumber).getColorPrefix();
        }catch(Exception ignored) {}

        goal = null;
        spawnBox = null;
        spawnLoc = null;
        respawnLoc = null;
    }

    public BridgeTeam(int minPlayers, int maxPlayers, int teamNumber, String color, List<BoundingBox> goal, BoundingBox spawnBox, Location spawnLoc, Location respawnLoc, UUID arenaUniqueId) {
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.teamNumber = teamNumber;
        this.color = color;
        this.goal = goal;
        this.spawnBox = spawnBox;
        this.spawnLoc = spawnLoc;
        this.respawnLoc = respawnLoc;
        this.arenaUniqueId = arenaUniqueId;
        this.players = new ArrayList<>();
    }

    public String getColor() {
        if(color == null) try {
            color = config.getTeamSettings(teamNumber).getColorPrefix();
        }catch(Exception ignored) {}

        return color;
    }

    public boolean isReady() {
        if(minPlayers == -1 ||
        maxPlayers == -1 || teamNumber == -1)
            return false;

        if(goal == null || goal.size() == 0 || respawnLoc == null ||
        spawnLoc == null || spawnBox == null)
            return false;

        return true;
    }

    public String getTeamName() {
        TeamConfig bt = teamNumber == 0 ? config.getTeamOneSettings() : config.getTeamTwoSettings();
        return bt.getName();
    }
    public void updateHealthScoreboard(Player player) {
        if(TheBridge.getInstance().getTablistManager().isShowHealth()) {
            scoreboardManager.getScoreboardTasks().get(player.getUniqueId()).updateHealthScoreboard(player,
                    "tb tablist hp", plugin.getArenaManager().findArena(arenaUniqueId.toString(), true));
        }
        if(TheBridge.getInstance().getBridgeConfig().isShowHealthBelowName()) {
            scoreboardManager.getScoreboardTasks().get(player.getUniqueId()).updateHealthScoreboard(player,
                    "tb bname hp", plugin.getArenaManager().findArena(arenaUniqueId.toString(), true));
        }
    }
    public void showScoreboard(Player player) {
        if(player == null) return;
        scoreboardManager.showScoreboard(player);
        Scoreboard scoreboard = scoreboardManager.getScoreboardTasks().get(player.getUniqueId()).getScoreboard();

        player.setScoreboard(scoreboard);
    }
    public void hideAllScoreboard() {
        for(UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            scoreboardManager.hideScoreboard(uuid, player);
        }
    }
    public void showTablist(UUID uuid) {
        if(uuid == null) return;
        List<UUID> players = plugin.getArenaManager().findArena(arenaUniqueId.toString(), true).getQueue();

        tablistManager.showTablist(uuid,
                players, // all players ingame
                Maps.asMap(new HashSet<>(players), u -> new PlayerInfoData(Bukkit.getPlayer(u))));
    }
    public void hideAllTablist() {
        for(UUID uuid : players) {
            tablistManager.hideTablist(uuid);
        }
    }
    public void start(World world, boolean atStart, Runnable onDone) {
        BSchematic.pasteSchematic(world, arenaUniqueId + "-box"+ teamNumber +".schem", (a) -> {
            onDone.run();
        });
        players.forEach(uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            showScoreboard(p);
            showTablist(uuid);
            if(atStart)
                updateHealthScoreboard(p);
        });
    }
    public void teleportBox() {
        players.forEach(uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            if(p != null) p.teleport(spawnLoc);
        });
    }

    // YAML Serializable

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("arena_uuid", arenaUniqueId);
        map.put("team_n", teamNumber +1);
        map.put("min_players", minPlayers);
        map.put("max_players", maxPlayers);
        map.put("spawn_location", spawnLoc);
        map.put("respawn_location", respawnLoc);
        map.put("spawn_box", spawnBox);
        map.put("goal_boxes", goal);

        return map;
    }
    public static BridgeTeam deserialize(Map<String, Object> map) {
        UUID arenaUUID = UUID.fromString((String) map.get("arena_uuid"));
        int team = (int) map.get("team_n") - 1;
        BridgeTeam bt = new BridgeTeam(arenaUUID, team);
        bt.goal = (List<BoundingBox>) map.get("goal_boxes");
        bt.minPlayers = (int) map.get("min_players");
        bt.maxPlayers = (int) map.get("max_players");
        bt.spawnLoc = (Location) map.get("spawn_location");
        bt.respawnLoc = (Location) map.get("respawn_location");
        bt.spawnBox = (BoundingBox) map.get("spawn_box");
        return bt;
    }

    public void finishUp() {
        hideAllScoreboard();
        hideAllTablist();
        players.clear();
        score = 0;
    }
}
