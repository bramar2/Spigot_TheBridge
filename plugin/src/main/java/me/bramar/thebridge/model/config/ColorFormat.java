package me.bramar.thebridge.model.config;

import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class ColorFormat {
    @RequiredArgsConstructor
    private static class $ {
        final int min, max;
        final String color;
        boolean isElse = false;

        @Override
        public String toString() {
            return isElse ? "{Else="+color+'}' :
                    "{"+min+'-'+max+'='+color+'}';
        }
    }
    private final List<$> list = new ArrayList<>();

    public ColorFormat(ConfigurationSection config) {
        $ else$ = null;

        for(String key : config.getKeys(false)) {
            try {
                String s = config.getString(key);
                if(s == null)
                    continue;
                String color = ChatColor.translateAlternateColorCodes('&', s);
                if(key.equalsIgnoreCase("else")) {
                    $ n = new $(0, 0, color);
                    n.isElse = true;
                    else$ = n;
                }else {
                    String[] split = key.split("-");
                    int first = Integer.parseInt(split[0]);
                    if(split.length == 1) {
                        list.add(new $(first, first, color));
                    }else {
                        int second = Integer.parseInt(split[1]);
                        list.add(new $(Math.min(first, second), Math.max(first, second), color));
                    }
                }
            }catch(Exception ignored) {}
        }
        if(else$ != null)
            list.add(else$);
    }
    public String getColor(int n) {
        for($ c : list) {
            if(c.isElse)
                return c.color;

            if(n >= c.min && n <= c.max)
                return c.color;
        }
        // no else
        return "";
    }

    @Override
    public String toString() {
        return "ColorFormat" + list;
    }
}
