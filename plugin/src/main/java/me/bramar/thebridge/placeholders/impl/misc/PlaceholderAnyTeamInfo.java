package me.bramar.thebridge.placeholders.impl.misc;

import lombok.Getter;
import me.bramar.thebridge.TheBridge;
import me.bramar.thebridge.arena.ArenaManager;
import me.bramar.thebridge.arena.BridgeArena;
import me.bramar.thebridge.arena.BridgeTeam;
import me.bramar.thebridge.placeholders.BridgePlaceholder;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class PlaceholderAnyTeamInfo implements BridgePlaceholder {
    private final ArenaManager arenaManager = TheBridge.getInstance().getArenaManager();

    @Getter
    private final Map<String, Function<BridgeTeam, Object>>
            infoFunctions = new HashMap<>();

    {
        infoFunctions.put("min_players", BridgeTeam::getMinPlayers);
        infoFunctions.put("max_players", BridgeTeam::getMaxPlayers);
        infoFunctions.put("score", BridgeTeam::getScore);
        infoFunctions.put("number", BridgeTeam::getTeamNumber); // team number (0 or 1)

        // technically in the config, but for practical reasons its here
        infoFunctions.put("color", BridgeTeam::getColor);
        infoFunctions.put("name", team -> team.getConfig().getTeamSettings(team.getTeamNumber()).getName());
    }

    @Override
    public String prefix() {
        return "team_$_$";
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String[] params, @NotNull String[] values) {
        if(player == null)
            return null;

        UUID uuid = player.getUniqueId();
        BridgeArena arena = arenaManager.findPlayerArena(uuid);
        if(arena == null)
            return null;

        if(!(values[0].equals("0") || values[0].equals("1")))
            return null;

        int teamNumber = Integer.parseInt(values[0]);
        BridgeTeam team = teamNumber == 0 ? arena.getTeam1() : arena.getTeam2();

        String info = concatenate(params, values[1]);
        if(infoFunctions.containsKey(info)) {
            return String.valueOf(infoFunctions.get(info).apply(team));
        }
        return null;
    }
}
