package me.bramar.thebridge.placeholders;

import me.bramar.thebridge.TheBridge;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;

public interface BridgePlaceholder {
    String prefix(); // prefix identifier for the placeholder excluding tb or thebridge

    String onRequest(OfflinePlayer player, @NotNull String[] params, @NotNull String[] values);

    default void load(BridgePlaceholders placeholders) {}

    default BridgePlaceholders getPlaceholders() {
        return TheBridge.getInstance().getPlaceholders();
    }

    default Logger getLogger() {
        return getPlaceholders().getLogger();
    }

    default String concatenate(String[] params, @Nullable String prefix) {
        if(params.length == 0) {
            if(prefix == null)
                return "";
            else
                return prefix;
        }
        StringBuilder str = new StringBuilder(prefix == null ? "" : prefix + "_");
        for(String param : params) {
            str.append(param).append("_");
        }
        return str.substring(0, str.length() - 1).toLowerCase();

    }
    default String[] getPrefixArray() {
        return prefix().split("_");
    }
}
