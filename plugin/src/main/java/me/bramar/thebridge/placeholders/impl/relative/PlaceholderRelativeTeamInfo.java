package me.bramar.thebridge.placeholders.impl.relative;

import me.bramar.thebridge.TheBridge;
import me.bramar.thebridge.arena.ArenaManager;
import me.bramar.thebridge.arena.BridgeArena;
import me.bramar.thebridge.arena.BridgeTeam;
import me.bramar.thebridge.placeholders.BridgePlaceholder;
import me.bramar.thebridge.placeholders.BridgePlaceholders;
import me.bramar.thebridge.placeholders.impl.player.PlaceholderTeamInfo;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;

public class PlaceholderRelativeTeamInfo implements BridgePlaceholder {
    private final ArenaManager arenaManager = TheBridge.getInstance().getArenaManager();
    private final Pattern UUID_REGEX = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

    private Map<String, Function<BridgeTeam, Object>>
            infoFunctions;

    @Override
    public void load(BridgePlaceholders placeholders) {
        infoFunctions = placeholders.findImplementation(PlaceholderTeamInfo.class).getInfoFunctions();
    }

    @Override
    public String prefix() {
        return "r_team_$_$";
    }

    private UUID getPlayerUUID(String[] values) {
        if(UUID_REGEX.matcher(values[0]).matches()) {
            return UUID.fromString(values[0]);
        }else {
            OfflinePlayer p = Bukkit.getOfflinePlayer(values[0]);
            if(p == null)
                return null;
            return p.getUniqueId();
        }
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String[] params, @NotNull String[] values) {

        UUID uuid = getPlayerUUID(values);
        BridgeArena arena = arenaManager.findPlayerArena(uuid);
        if(arena == null)
            return null;
        BridgeTeam team = arena.getTeam(uuid);
        if(team == null)
            return null;

        String info;
        if(values[1].equalsIgnoreCase("other")) {
            team = arena.otherTeam(team);
            info = concatenate(params, null);
        }else {
            info = concatenate(params, values[1]);
        }

        if(infoFunctions.containsKey(info)) {
            return String.valueOf(infoFunctions.get(info).apply(team));
        }
        return null;
    }
}
