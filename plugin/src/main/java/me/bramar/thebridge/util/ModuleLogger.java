package me.bramar.thebridge.util;

import org.bukkit.Bukkit;

import java.util.logging.Level;
import java.util.logging.Logger;

// Taken from PluginLogger
public class ModuleLogger extends Logger {
    public ModuleLogger(String name) { // 'name' as in [..] [TheBridge/{name}]
        super("TheBridge/" + name + "", null);
        setParent(Bukkit.getServer().getLogger());
        setLevel(Level.ALL);
    }
}
