package me.bramar.thebridge.placeholders.impl.misc;

import me.bramar.thebridge.TheBridge;
import me.bramar.thebridge.arena.ArenaManager;
import me.bramar.thebridge.arena.BridgeArena;
import me.bramar.thebridge.arena.BridgeTeam;
import me.bramar.thebridge.placeholders.BridgePlaceholder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.regex.Pattern;

public class PlaceholderAnyTeamGoalSymbol implements BridgePlaceholder {
    private final ArenaManager arenaManager = TheBridge.getInstance().getArenaManager();
    private final Pattern UUID_REGEX = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

    @Override
    public String prefix() {
        return "team_$_goal_symbol_$";
    }


    @Override
    public String onRequest(OfflinePlayer player, @NotNull String[] params, @NotNull String[] values) {
        if(player == null)
            return null;
        BridgeArena arena = arenaManager.findPlayerArena(player.getUniqueId());
        if(arena == null)
            return null;
        if(!(values[0].equals("0") || values[0].equals("1")))
            return null;

        BridgeTeam team = values[0].equals("0") ? arena.getTeam1() : arena.getTeam2();

        String symbol = values[1];
        StringBuilder text = new StringBuilder("" + team.getColor());
        int max = TheBridge.getInstance().getBridgeConfig().getGoalCount();
        int j = 0;
        try {
            if(!params[j].equalsIgnoreCase("a"))
                max = Integer.parseInt(params[j]);
            j++;
        }catch(Exception ignored) {}
        String separator = "" + ChatColor.RESET;
        try {
            separator = ChatColor.translateAlternateColorCodes('&', params[j]);
        }catch(Exception ignored) {}
        int score = team.getScore();
        for(int i = 0; i < max; i++) {
            if(i == score)
                text.append(separator);
            text.append(symbol);
        }
        return text.toString();
    }
}
