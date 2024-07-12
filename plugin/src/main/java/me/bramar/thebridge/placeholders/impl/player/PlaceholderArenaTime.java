package me.bramar.thebridge.placeholders.impl.player;

import me.bramar.thebridge.TheBridge;
import me.bramar.thebridge.arena.BridgeArena;
import me.bramar.thebridge.placeholders.BridgePlaceholder;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class PlaceholderArenaTime implements BridgePlaceholder {

//    infoFunctions.put("time_elapsed_per_s", arena -> (arena.getGameTime() / 20) % 60);
//        infoFunctions.put("time_elapsed_per_m", arena -> (arena.getGameTime() / 20 / 60) % 60);
//        infoFunctions.put("time_elapsed_per_h", arena -> arena.getGameTime() / 20 / 60 / 60);
//        infoFunctions.put("time_elapsed_s", arena -> arena.getGameTime() / 20);
//        infoFunctions.put("time_elapsed_m", arena -> arena.getGameTime() / 20 / 60);
//        infoFunctions.put("time_elapsed_h", arena -> arena.getGameTime() / 20 / 60 / 60);
//
//
//        infoFunctions.put("time_left_per_s", arena -> (arena.getGameTimeLeft() / 20) % 60);
//        infoFunctions.put("time_left_per_m", arena -> (arena.getGameTimeLeft() / 20 / 60) % 60);
//        infoFunctions.put("time_left_per_h", arena -> arena.getGameTimeLeft() / 20 / 60 / 60);
//        infoFunctions.put("time_left_s", arena -> arena.getGameTimeLeft() / 20);
//        infoFunctions.put("time_left_m", arena -> arena.getGameTimeLeft() / 20 / 60);
//        infoFunctions.put("time_left_h", arena -> arena.getGameTimeLeft() / 20 / 60 / 60);
    @Override
    public String prefix() {
        return "arena_time_$_$";
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

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String[] params, @NotNull String[] values) {
        if(player == null)
            return null;

        BridgeArena arena = TheBridge.getInstance().getArenaManager().findPlayerArena(player.getUniqueId());
        if(arena == null)
            return null;

        String v1 = values[1].toLowerCase();
        boolean isPer = v1.equalsIgnoreCase("per");
        if(values[0].equalsIgnoreCase("elapsed")) {
            if(isPer) {
                if(params.length == 0)
                    return null;
                return timeElapsedPer(arena, params[0].toLowerCase(), params.length > 1 ? params[1].toLowerCase() : null);
            }else
                return timeElapsed(arena, v1, params.length != 0 ? params[0].toLowerCase() : null);
        }else if(values[0].equalsIgnoreCase("left")) {
            if(isPer) {
                if(params.length == 0)
                    return null;
                return timeLeftPer(arena, params[0].toLowerCase(), params.length > 1 ? params[1].toLowerCase() : null);
            }else
                return timeLeft(arena, v1, params.length != 0 ? params[0].toLowerCase() : null);
        }
        return null;
    }
}
