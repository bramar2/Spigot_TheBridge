package me.bramar.thebridge.arena.blueprint;

import lombok.Getter;
import me.bramar.thebridge.TheBridge;
import me.bramar.thebridge.arena.BridgeArena;
import me.bramar.thebridge.arena.BridgeTeam;
import me.bramar.thebridge.model.BoundingBox;
import me.bramar.thebridge.util.schematic.BSchematic;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class BridgeArenaUnlimited extends BridgeArena {

    // Will close automatically after 30s of inactivity
    private BukkitRunnable autoClose = null;
    @Getter
    private boolean closing = false;

    protected BridgeArenaUnlimited(World world, String worldName, BoundingBox arenaBox, BoundingBox placeBox, int voidY, int placeMinY, int placeMaxY, List<BoundingBox> disallowPlace, List<BoundingBox> allowBreak, List<BoundingBox> disallowBreak, BridgeTeam team1, BridgeTeam team2, String mapName, String arenaName, Location joinLocation, int neededPlayers, int maxPlayers, UUID uniqueId, File file) {
        super(world, worldName, arenaBox, placeBox, voidY, placeMinY, placeMaxY, disallowPlace, allowBreak, disallowBreak, team1, team2, mapName, arenaName, joinLocation, neededPlayers, maxPlayers, uniqueId, file);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        if(closing && e.getTo().getWorld() == getWorld()) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("Error: The world you're trying to join is not available");
        }
    }

    @Override
    public void joinQueue(Player p) {
        if(closing) {
            p.sendMessage(ChatColor.RED + "That game has already ended!");
            return;
        }
        super.joinQueue(p);

        if(!getGameState().hasStarted() && getQueue().size() > 0 && autoClose != null) {
            autoClose.cancel();
            autoClose = null;
        }
    }

    @Override
    public void leaveQueue(Player p) {
        super.leaveQueue(p);
        if(!getGameState().hasStarted() && getQueue().isEmpty() && autoClose == null) {
            setAutoClose();
        }
    }
    public void setAutoClose() {
        autoClose = new BukkitRunnable() {
            @Override
            public void run() {
                closeArena();
            }
        };
        autoClose.runTaskLater(getPlugin(), 30 * 20);
    }

    @Override
    public void onEndGame() {
        getQueue().clear();
        getDisconnected().clear();
        getFirstGapple().clear();
        getArrowCooldown().clear();
        getPlacedBlocks().clear();
        getTeam1().finishUp();
        getTeam2().finishUp();
        closing = true;
        setAutoClose();
    }

    public void forceCloseArena() {
        Bukkit.unloadWorld(getWorld(), false);
        HandlerList.unregisterAll(this);
        try {
            FileUtils.deleteDirectory(new File(Bukkit.getWorldContainer(), getWorldName()));
        }catch(IOException e) {
            e.printStackTrace();
            getLogger().warning("Failed to delete TheBridge world " + getWorldName());
        }
    }

    public void closeArena() {
        closing = true;
        Location spawnLoc = TheBridge.getInstance().getBridgeConfig().getSpawnLocation();
        if(spawnLoc.getWorld() == getWorld() || spawnLoc.getWorld() == null)
            spawnLoc.setWorld(Bukkit.getWorlds().stream().filter(world -> world != getWorld()).findFirst().orElse(null));
        boolean wIsNull = spawnLoc.getWorld() == null;
        for(Player p : getWorld().getPlayers()) {
            if(wIsNull) {
                p.sendMessage(ChatColor.RED + "Couldn't find a world to teleport you to. You might be kicked since this world will be deleted.");
            }else {
                p.teleport(spawnLoc);
                p.sendMessage(ChatColor.RED + "You have been teleported because the world you were in was getting deleted.");
            }
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), () -> {
            for(Player p : getWorld().getPlayers()) {
                p.kickPlayer("Error: World will be deleted and no other world found");
            }
            Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), () -> {
                Bukkit.unloadWorld(getWorld(), false);
                HandlerList.unregisterAll(this);
                getListeners().forEach(HandlerList::unregisterAll);
                Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), () -> {
                    try {
                        FileUtils.deleteDirectory(new File(Bukkit.getWorldContainer(), getWorldName()));
                    }catch(IOException e) {
                        e.printStackTrace();
                        getLogger().warning("Failed to delete TheBridge world " + getWorldName());
                    }
                }, 1);
                TheBridge.getInstance().getArenaManager().getArenas().remove(this);
            }, 1);
        }, 5 * 20);
    }

    @Override
    public void onPlayerReset(List<UUID> queueCopy) {
        super.onPlayerReset(queueCopy);
        BSchematic.pasteSchematic(getWorld(), getUniqueId() + ".schem", (output) -> {
            getLogger().info("Finished re-pasting/regenerating");
        });
    }
}