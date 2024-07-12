package me.bramar.thebridge.placeholders.impl.relative;

import me.bramar.thebridge.TheBridge;
import me.bramar.thebridge.arena.BridgeArena;
import me.bramar.thebridge.placeholders.BridgePlaceholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.regex.Pattern;

public class PlaceholderRelativeArenaTime implements BridgePlaceholder {
    private final Pattern UUID_REGEX = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

    @Override
    public String prefix() {
        return "arena_$_time_$_$";
    }
    private String convertDigits(String numberStr, String digits0) {
        int digits;
        try {
            if(digits0 == null || !digits0.endsWith("d"))
                return numberStr;
            digits = Integer.parseInt(digits0.substring(0, digits0.length()-1));
        }catch(NumberFormatException nfe) {
            return numberStr;
        }
        if(numberStr.length() >= digits || digits < 2)
            return numberStr;
        StringBuilder prefix = new StringBuilder();
        for(int i = 0; i < (digits - numberStr.length()); i++) {
            prefix.append("0");
        }
        return prefix + numberStr;
    }
    private String timeElapsedPer(BridgeArena arena, String value, String digits) {
        long number;
        switch(value) {
            case "s":
                number = (arena.getGameTime() / 20) % 60;
                break;
            case "m":
                number = (arena.getGameTime() / 20 / 60) % 60;
                break;
            case "h":
                number = arena.getGameTime() / 20 / 60 / 60;
                break;
            default:
                return null;
        }
        return convertDigits(number+"", digits);
    }
    private String timeLeftPer(BridgeArena arena, String value, String digits) {
        long number;
        switch(value) {
            case "s":
                number = (arena.getGameTimeLeft() / 20) % 60;
                break;
            case "m":
                number = (arena.getGameTimeLeft() / 20 / 60) % 60;
                break;
            case "h":
                number = arena.getGameTimeLeft() / 20 / 60 / 60;
                break;
            default:
                return null;
        }
        return convertDigits(number+"", digits);
    }


    private String timeElapsed(BridgeArena arena, String value, String digits) {
        long number;
        switch(value) {
            case "s":
                number = arena.getGameTime() / 20;
                break;
            case "m":
                number = arena.getGameTime() / 20 / 60;
                break;
            case "h":
                number = arena.getGameTime() / 20 / 60 / 60;
                break;
            default:
                return null;
        }
        return convertDigits(number+"", digits);
    }
    private String timeLeft(BridgeArena arena, String value, String digits) {
        long number;
        switch(value) {
            case "s":
                number = arena.getGameTimeLeft() / 20;
                break;
            case "m":
                number = arena.getGameTimeLeft() / 20 / 60;
                break;
            case "h":
                number = arena.getGameTimeLeft() / 20 / 60 / 60;
                break;
            default:
                return null;
        }
        return convertDigits(number+"", digits);
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
        if(uuid == null)
            return null;
        BridgeArena arena = TheBridge.getInstance().getArenaManager().findPlayerArena(uuid);
        if(arena == null)
            return null;

        String v2 = values[2].toLowerCase();
        boolean isPer = v2.equalsIgnoreCase("per");
        if(values[1].equalsIgnoreCase("elapsed")) {
            if(isPer) {
                if(params.length == 0)
                    return null;
                return timeElapsedPer(arena, params[0].toLowerCase(), params.length > 1 ? params[1].toLowerCase() : null);
            }else
                return timeElapsed(arena, v2, params.length != 0 ? params[0].toLowerCase() : null);
        }else if(values[1].equalsIgnoreCase("left")) {
            if(isPer) {
                if(params.length == 0)
                    return null;
                return timeLeftPer(arena, params[0].toLowerCase(), params.length > 1 ? params[1].toLowerCase() : null);
            }else
                return timeLeft(arena, v2, params.length != 0 ? params[0].toLowerCase() : null);
        }
        return null;
    }
}
