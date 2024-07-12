package me.bramar.thebridge.placeholders.impl.misc;

import lombok.AllArgsConstructor;
import me.bramar.thebridge.placeholders.BridgePlaceholder;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PlaceholderBridgeIf implements BridgePlaceholder {

    @Override
    public String prefix() {
        return "if";
    }

    @AllArgsConstructor
    private static final class Condition {
        String check, output;
    }

    // FORMAT: %tb_if_{tb_placeholder_whatever}_0_ZERO_1_ONE_2_TWO%
    // FORMAT: %tb_if_{tb_placeholder_whatever}_0_ZERO_1_ONE_2_TWO_NONE,thisiselse%

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String[] params, @NotNull String[] values) {
        String placeholder = null;
        boolean start = false;
        boolean wasStart = false;
        int n = 0;
        for(int i = 0; i < params.length; i++) {
            String param = params[i];
            if(!start) {
                start = param.contains("{");
                if(start) {
                    placeholder = param.substring(param.indexOf('{') + 1) + "_";
                    wasStart = true;
                }
            }else {
                if(param.contains("}")) {
                    placeholder += param.substring(0, param.indexOf('}'));
                    start = false;
                    n = i + 1;
                    break;
                }else
                    placeholder += param + "_";
            }
        }
        if(!wasStart)
            return null; // no {}
        if(start)
            return null; // there was no ending braces } (it is still going)

        placeholder = '%' + placeholder + '%'; // because {A} --> A, so we turn A --> %A%

        List<Condition> conditions = new ArrayList<>();
        String last = null;
        for(int i = n; i < params.length; i++) {
            String param = params[i];
            if(last == null)
                last = param;
            else {
                conditions.add(new Condition(last, param));
                last = null;
            }
        }
        String elseString = last; // If last is null, then else string is null, else it is last string
        String result = getPlaceholders().applyPlaceholders(player, placeholder);
        for(Condition condition : conditions) {
            if(condition.check.equalsIgnoreCase(result))
                return condition.output;
        }

        return elseString;
    }
}
