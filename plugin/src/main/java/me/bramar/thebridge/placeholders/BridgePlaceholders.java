package me.bramar.thebridge.placeholders;

import lombok.Getter;
import me.bramar.thebridge.placeholders.impl.misc.PlaceholderAnyTeamGoalSymbol;
import me.bramar.thebridge.placeholders.impl.misc.PlaceholderAnyTeamInfo;
import me.bramar.thebridge.placeholders.impl.misc.PlaceholderBridgeIf;
import me.bramar.thebridge.placeholders.impl.player.PlaceholderArenaInfo;
import me.bramar.thebridge.placeholders.impl.player.PlaceholderArenaTime;
import me.bramar.thebridge.placeholders.impl.player.PlaceholderTeamGoalSymbol;
import me.bramar.thebridge.placeholders.impl.player.PlaceholderTeamInfo;
import me.bramar.thebridge.placeholders.impl.relative.PlaceholderRelativeArenaInfo;
import me.bramar.thebridge.placeholders.impl.relative.PlaceholderRelativeArenaTime;
import me.bramar.thebridge.placeholders.impl.relative.PlaceholderRelativeTeamGoalSymbol;
import me.bramar.thebridge.placeholders.impl.relative.PlaceholderRelativeTeamInfo;
import me.bramar.thebridge.util.ModuleLogger;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
Making use of PlaceholderAPI
 */
public class BridgePlaceholders {
    private final Map<BridgePlaceholder, String[]> implementations = new HashMap<>();
    private final boolean hasPlaceholderAPI;
    private VanillaPlaceholders vanillaPlaceholders;
    @Getter
    private final ModuleLogger logger = new ModuleLogger("Placeholders");



    public BridgePlaceholders(boolean hasPlaceholderAPI) {
        this.hasPlaceholderAPI = hasPlaceholderAPI;
        if(hasPlaceholderAPI) {
            new BridgePlaceholderExpansion(this, "tb").register();
            new BridgePlaceholderExpansion(this, "thebridge").register();
            logger.info("PlaceholderAPI integration loaded.");
        }else
            vanillaPlaceholders = new VanillaPlaceholders(this);

        // add all implementations
        List<BridgePlaceholder> placeholderList =
                Arrays.asList(
                        new PlaceholderArenaInfo(),
                        new PlaceholderRelativeArenaInfo(),
                        new PlaceholderAnyTeamInfo(),
                        new PlaceholderTeamInfo(),
                        new PlaceholderRelativeTeamInfo(),
                        new PlaceholderAnyTeamGoalSymbol(),
                        new PlaceholderTeamGoalSymbol(),
                        new PlaceholderRelativeTeamGoalSymbol(),
                        new PlaceholderArenaTime(),
                        new PlaceholderRelativeArenaTime(),
                        new PlaceholderBridgeIf()
                );
        for(BridgePlaceholder placeholder : placeholderList)
            implementations.put(placeholder, placeholder.getPrefixArray());
        for(BridgePlaceholder placeholder : placeholderList) {
            placeholder.load(this);
        }

        logger.info("Successfully loaded.");
    }

    public String applyPlaceholders(@Nullable OfflinePlayer p, String text) {
        if(hasPlaceholderAPI)
            return PlaceholderAPI.setPlaceholders(p, text);
        else
            return vanillaPlaceholders.applyPlaceholders(p, text);
    }

    public <T extends BridgePlaceholder> T findImplementation(Class<T> clazz) {
        for(BridgePlaceholder placeholder : implementations.keySet()) {
            if(clazz.isInstance(placeholder))
                return (T) placeholder;
        }
        return null;
    }


    public String onRequest(OfflinePlayer player, @NotNull String paramsStr) {
        String[] params = paramsStr.split("_");
        outer:
        for(BridgePlaceholder placeholder : implementations.keySet()) {
            String[] prefix = implementations.get(placeholder);
            if(params.length < prefix.length)
                continue;
            List<String> values = new ArrayList<>();
            for(int i = 0; i < prefix.length; i++) {
                String value = prefix[i];
                if(value.equals("$")) {
                    values.add(params[i]);
                }else if(value.contains("$")) {
                    int indexOf = value.indexOf('$');
                    String first = Pattern.quote(value.substring(0, indexOf));
                    String second = Pattern.quote(value.substring(indexOf + 1));
                    String regex = second.isEmpty() ? String.format("^(?<=%s).+$", first) :
                            String.format("^(?<=%s).+(?=%s)$", first, second);

                    Matcher matcher = Pattern.compile(regex).matcher(value);

                    if(matcher.matches()) {
                        values.add(matcher.group(0));
                    }
                }else {
                    if(!params[i].equalsIgnoreCase(value))
                        continue outer;
                }
            }
            String[] newParams;
            // trim to only the important ones
            if(params.length != prefix.length) {
                newParams = new String[params.length - prefix.length];
                System.arraycopy(params, prefix.length, newParams, 0, newParams.length);
            }else
                newParams = new String[0];

            try {
                String result = placeholder.onRequest(player, newParams, values.toArray(new String[0]));
                if(result != null)
                    return result;
            }catch(Exception e1) {
                e1.printStackTrace();
                getLogger().warning("Placeholder failed (" + getClass().getCanonicalName() + "). Report to the author!");
            }
        }
        return null;
    }

    public Set<BridgePlaceholder> getPlaceholders() {
        return implementations.keySet();
    }
}
