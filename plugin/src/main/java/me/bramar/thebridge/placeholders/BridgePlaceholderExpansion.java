package me.bramar.thebridge.placeholders;

import lombok.AllArgsConstructor;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public class BridgePlaceholderExpansion extends PlaceholderExpansion {
    private final BridgePlaceholders b;
    private final String identifier;
    @Override
    public @NotNull String getIdentifier() {
        return identifier;
    }
    @Override
    public @NotNull String getAuthor() {
        return "bramar2";
    }
    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        return b.onRequest(player, params);
    }
}
