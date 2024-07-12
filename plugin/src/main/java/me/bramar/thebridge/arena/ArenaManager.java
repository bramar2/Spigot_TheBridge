package me.bramar.thebridge.arena;

import com.google.common.base.Charsets;
import lombok.Getter;
import me.bramar.thebridge.TheBridge;
import me.bramar.thebridge.arena.blueprint.BridgeArenaBlueprint;
import me.bramar.thebridge.util.ModuleLogger;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static me.bramar.thebridge.util.CompatibilityUtils.*;

@Getter
public class ArenaManager {
    private final List<BridgeArena> arenas = new ArrayList<>();
    private final List<BridgeArenaBlueprint> arenaBlueprints = new ArrayList<>();
    private final TheBridge plugin = TheBridge.getInstance();
    private final ModuleLogger logger = new ModuleLogger("Arena");
    public ArenaManager() {
        load();
    }
    public UUID newUUID() {
        AtomicReference<UUID> uuid = new AtomicReference<>();
        do {
            uuid.set(UUID.randomUUID());
        }while(arenas.stream().anyMatch(arena -> arena.getUniqueId().equals(uuid.get())));
        return uuid.get();
    }

    public void join(Player p, String name) {
        // for unlimited arenas, name is treated as "mapName" and "arenaName" if found one
        // for normal mode, name is treated as "arenaName"
        BridgeArena playerArena0 = null;
        for(BridgeArena arena : arenas) {
            if(arena.getQueue().contains(p.getUniqueId())) {
                if(arena.getGameState().hasStarted()) {
                    p.sendMessage(ChatColor.RED + "Error: You are currently in a game.");
                    return;
                }
                playerArena0 = arena;
            }
        }
        BridgeArena playerArena = playerArena0;
        List<BridgeArena> arenaList = new ArrayList<>();
        int playerCount = 0;
        // prioritize arenas with more players in queue
        for(BridgeArena arena : findArenaByAllName(name)) {
            if(!arena.availableForJoin())
                continue;
            if(arena.getQueue().size() == playerCount) {
                arenaList.add(arena);
            }else if(arena.getQueue().size() > playerCount) {
                arenaList.clear();
                arenaList.add(arena);
                playerCount = arena.getQueue().size();
            }
        }
        if(arenaList.isEmpty()) {
            p.sendMessage(ChatColor.RED + "Error: No available arenas found by that name.");
            return;
        }
        BridgeArena arena = arenaList.get(TheBridge.getInstance().getRandom().nextInt(arenaList.size()));
        if(arena != null && arena.availableForJoin()) {
            if(playerArena != null && !TheBridge.ALLOW_SOLO_GAMES)
                playerArena.leaveQueue(p);
            arena.joinQueue(p);
        }else {
            BridgeArenaBlueprint blueprint = arenaBlueprints.stream().filter(b -> b.getMapName().equalsIgnoreCase(name)).findAny().orElse(null);
            if(blueprint == null) {
                if(arena != null)
                    p.sendMessage(ChatColor.RED + "Error: Arena is not available to join.");
                else
                    p.sendMessage(ChatColor.RED + "Error: No arenas found by that name.");
                return;
            }
            blueprint.createArena(a -> {
                if(playerArena != null && !TheBridge.ALLOW_SOLO_GAMES)
                    playerArena.leaveQueue(p);
                a.joinQueue(p);
                arenas.add(a);
            });
        }
    }

    public void joinRandom(Player p) {
        BridgeArena playerArena = this.findPlayerArena(p.getUniqueId());
        List<BridgeArena> availableArenas = arenas.stream().filter(arena -> arena.availableForJoin() &&
                (TheBridge.ALLOW_SOLO_GAMES ? true : arena != playerArena)).collect(Collectors.toList());
        if(availableArenas.isEmpty()) {
            if(TheBridge.getInstance().getBridgeConfig().isUnlimitedArenas() && !arenaBlueprints.isEmpty()) {
                BridgeArenaBlueprint blueprint = arenaBlueprints.get(TheBridge.getInstance().getRandom().nextInt(arenaBlueprints.size()));
                blueprint.createArena(arena -> {
                    if(playerArena != null && !TheBridge.ALLOW_SOLO_GAMES)
                        playerArena.leaveQueue(p);
                    arena.joinQueue(p);
                    arenas.add(arena);
                });
            }else {
                p.sendMessage(ChatColor.RED + "Error: There are no games currently available right now");
            }
        }else {
            List<BridgeArena> arenaList = new ArrayList<>();
            int playerCount = 0;
            // prioritize arenas with more players in queue
            for(BridgeArena arena : availableArenas) {
                if(arena.getQueue().size() == playerCount) {
                    arenaList.add(arena);
                }else if(arena.getQueue().size() > playerCount) {
                    arenaList.clear();
                    arenaList.add(arena);
                    playerCount = arena.getQueue().size();
                }
            }
            BridgeArena arena = arenaList.get(TheBridge.getInstance().getRandom().nextInt(arenaList.size()));
            if(playerArena != null && !TheBridge.ALLOW_SOLO_GAMES)
                playerArena.leaveQueue(p);
            arena.joinQueue(p);
        }
    }
    public void load() {
        File arenaFolder = new File(plugin.getDataFolder(), "arenas");
        if(!arenaFolder.exists()) {
            if(!arenaFolder.mkdirs())
                logger.warning("Failed to create 'arenas' folder");
            return;
        }
        File[] files = arenaFolder.listFiles();
        if(files != null)
            outer:
            for(File file : files) {
                if(!file.getName().endsWith(".yml"))
                    continue;
                YamlConfiguration arenaConfig = new YamlConfiguration();
                try {
                    FileInputStream stream = new FileInputStream(file);
                    arenaConfig.load(new InputStreamReader(stream, Charsets.UTF_8));
                }catch(Exception e1) {
                    e1.printStackTrace();
                }
                if(arenaConfig.isSet("arena")) {
                    BridgeArena arena = null;
                    try {
                        arena = getSerializable(arenaConfig, "arena", BridgeArena.class);
                    }catch(Exception ignored) {}
                    for(BridgeArena otherArena : arenas) {
                        if(otherArena.getArenaName().equalsIgnoreCase(otherArena.getArenaName())) {
                            logger.warning("Invalid arena at " + file.getName() + ": Duplicate arena name");
                            continue outer;
                        }
                    }
                    if(arena == null) {
                        // check for version then upgrade if needed
                        logger.warning("Invalid arena at " + file.getName() + " (maybe world is not ready)");
                    }else {
                        if(!arenaConfig.isSet("version")) {
                            arenaConfig.set("version", BridgeArena.VERSION);
                            try {
                                arenaConfig.save(file);
                            }catch(IOException ignored) {}
                        }else {
                            // check for version then upgrade if needed
                        }
                        if(plugin.getBridgeConfig().isUnlimitedArenas()) {
                            arena.setFile(file);
                            for(BridgeArenaBlueprint otherBlueprint : arenaBlueprints) {
                                if(otherBlueprint.getMapName().equalsIgnoreCase(arena.getArenaName())) {
                                    logger.warning("Invalid arena at " + file.getName() + ": Duplicate map name");
                                    continue outer;
                                }
                            }
                            arenaBlueprints.add(BridgeArenaBlueprint.of(arena));
                        }else {
                            arena.setFile(file);
                            arenas.add(arena);
                            arena.loadEvents(plugin);
                        }
                    }
                }
            }
        logger.info("Successfully loaded with " + arenas.size() + " arena" + (arenas.size() > 1 ? "s." : "."));
    }

    public BridgeArena findArena(String arenaName, boolean checkForUUID) {
        for(BridgeArena arena : arenas) {
            if(checkForUUID) {
                if(arena.getUniqueId().toString().equals(arenaName))
                    return arena;
            }else
                if(arena.getArenaName().equalsIgnoreCase(arenaName))
                    return arena;
        }
        return null;
    }
    public Collection<BridgeArena> findArenaByAllName(String arenaName) {
        Set<BridgeArena> arenaSet = new HashSet<>();
        for(BridgeArena arena : arenas) {
            if(arena.getArenaName().equalsIgnoreCase(arenaName))
                arenaSet.add(arena);
        }
        for(BridgeArena arena : arenas) {
            if(arena.getMapName().equalsIgnoreCase(arenaName))
                arenaSet.add(arena);
        }
        return arenaSet;
    }
    public BridgeArena findPlayerArena(UUID playerUUID) {
        for(BridgeArena arena : arenas) {
            for(UUID p2 : arena.getQueue()) {
                if(p2.equals(playerUUID))
                    return arena;
            }
        }
        return null;
    }
}
