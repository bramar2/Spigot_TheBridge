package me.bramar.thebridge.placeholders.impl.player;

import me.bramar.thebridge.TheBridge;
import me.bramar.thebridge.arena.ArenaManager;
import me.bramar.thebridge.arena.BridgeArena;
import me.bramar.thebridge.arena.BridgeTeam;
import me.bramar.thebridge.placeholders.BridgePlaceholder;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PlaceholderTeamGoalSymbol implements BridgePlaceholder {
    private final ArenaManager arenaManager = TheBridge.getInstance().getArenaManager();

    @Override
    public String prefix() {
        return "team_goal_symbol_$";
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String[] params, @NotNull String[] values) {
        if(player == null)
            return null;

        UUID uuid = player.getUniqueId();
        BridgeArena arena = arenaManager.findPlayerArena(uuid);
        if(arena == null)
            return null;
        BridgeTeam team = arena.getTeam(uuid);
        String symbol;
        int j = 0;
        if(values[0].equalsIgnoreCase("other")) {
            j++;
            if(params.length == 0)
                return null;
            team = arena.otherTeam(team);
            symbol = params[0];
        }else
            symbol = values[0];

        StringBuilder text = new StringBuilder("" + team.getColor());
        int max = TheBridge.getInstance().getBridgeConfig().getGoalCount();
        String separator = ChatColor.RESET + "";
        // 'a' stands for 'a'utomatic max score
        try {
            // j --> 0 if no 'other', 1 if there is 'other'
            if(!params[j].equalsIgnoreCase("a"))
                max = Integer.parseInt(params[j]);
            j++; // because it didnt cause out of bounds
        }catch(Exception ignored) {}
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
