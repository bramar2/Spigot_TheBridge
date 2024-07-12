package me.bramar.thebridge.scoreboard;

import lombok.Getter;
import lombok.Setter;
import me.bramar.thebridge.TheBridge;
import me.bramar.thebridge.arena.BridgeArena;
import me.bramar.thebridge.placeholders.BridgePlaceholders;
import me.bramar.thebridge.util.ScoreboardWrapper;
import me.bramar.thebridge.util.nms.NMSEntityPlayer;
import me.bramar.thebridge.util.nms.NMSUtil;
import me.bramar.thebridge.util.nms.packet.NMSUpdateScorePacket;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@Getter @Setter
public class ScoreboardTask extends ScoreboardWrapper implements Runnable {
    private final TheBridge plugin = TheBridge.getInstance();
    private final ScoreboardManager config = plugin.getScoreboardManager();
    private final BridgePlaceholders placeholders = plugin.getPlaceholders();
    private final NMSUtil nms = plugin.getNMSUtil();
    private int taskId;
    private int tick = 0;
    private final OfflinePlayer player;
    private final boolean spectator;

    public ScoreboardTask(OfflinePlayer p, String title, boolean spectator) {
        super(title);
        this.player = p;
        this.spectator = spectator;
    }

    @Override
    public void run() {
        if(tick > (spectator ? config.getSpectatorTotalTick() : config.getTotalTick()))
            tick = 0;
        updateScoreboard(tick);
        tick++;
    }
    // applyPlaceholder
    public String p(String str) {
        return ChatColor.translateAlternateColorCodes('&', placeholders.applyPlaceholders(player, str));
    }
    public List<String> p(List<String> list) {
        list = new ArrayList<>(list); // clone it
        for(int i = 0; i < list.size(); i++) {
            list.set(i, p(list.get(i)));
        }
        return list;
    }

    public void updateScoreboard(int tick) {
        Map<Integer, ScoreboardTick> scoreboard = spectator ? config.getSpectatorScoreboard() : config.getScoreboard();
        if(scoreboard.containsKey(tick)) {
            ScoreboardTick scoreboardTick = scoreboard.get(tick);
            setTitle(p(scoreboardTick.getTitle()));
            if(!scoreboardTick.isSameContent())
                setLines(p(scoreboardTick.getContent()));
        }
    }
    public void updateHealthScoreboard(Player player, String scoreboardName, BridgeArena arena) {
        NMSEntityPlayer nmsPlayer = nms.getPlayer(player);
        for(UUID playerUUID : arena.getQueue()) {
            Player p = Bukkit.getPlayer(playerUUID);
            String name;
            int health;
            if(p == null) {
                name = Bukkit.getOfflinePlayer(playerUUID).getName();
                health = 20;
            }else {
                name = p.getName();
                health = (int) Math.round(p.getHealth());
            }
            NMSUpdateScorePacket packet = nms.newUpdateScorePacket(name, scoreboardName, 0, health);
            packet.createNMS().sendPacket(nmsPlayer);
        }
    }
}
