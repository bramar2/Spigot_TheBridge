package me.bramar.thebridge.placeholders.impl.player;

import lombok.Getter;
import me.bramar.thebridge.TheBridge;
import me.bramar.thebridge.arena.ArenaManager;
import me.bramar.thebridge.arena.BridgeArena;
import me.bramar.thebridge.placeholders.BridgePlaceholder;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;


public class PlaceholderArenaInfo implements BridgePlaceholder {
    private final ArenaManager arenaManager = TheBridge.getInstance().getArenaManager();
    private final Pattern UUID_REGEX = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

    @Getter
    private final Map<String, Function<BridgeArena, Object>>
            infoFunctions = new HashMap<>();

    {
        infoFunctions.put("uuid", BridgeArena::getUniqueId);

        infoFunctions.put("name", BridgeArena::getArenaName);
        infoFunctions.put("map", BridgeArena::getMapName);
        infoFunctions.put("world", BridgeArena::getWorldName);

        infoFunctions.put("y_void", BridgeArena::getVoidY);
        infoFunctions.put("y_min_place", BridgeArena::getPlaceMinY);
        infoFunctions.put("y_max_place", BridgeArena::getPlaceMaxY);

        infoFunctions.put("state", arena -> arena.getGameState().getName());
        infoFunctions.put("state_internal", BridgeArena::getGameState);
        infoFunctions.put("state_number", arena -> arena.getGameState().getId());

        infoFunctions.put("min_players", BridgeArena::getNeededPlayers);
        infoFunctions.put("max_players", BridgeArena::getMaxPlayers);
        infoFunctions.put("player_count", arena -> arena.getQueue().size());
    }

    @Override
    public String prefix() {
        return "arena_$";
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String[] params, @NotNull String[] values) {
        if(player == null)
            return null;
        BridgeArena arena = arenaManager.findPlayerArena(player.getUniqueId());
        if(arena == null)
            return null;

        String info = concatenate(params, values[0]);
        if(infoFunctions.containsKey(info)) {
            return String.valueOf(infoFunctions.get(info).apply(arena));
        }
        return null;
    }
}
