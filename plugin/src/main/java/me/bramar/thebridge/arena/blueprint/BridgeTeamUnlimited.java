package me.bramar.thebridge.arena.blueprint;

import me.bramar.thebridge.arena.BridgeTeam;
import me.bramar.thebridge.model.BoundingBox;
import me.bramar.thebridge.util.schematic.BSchematic;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class BridgeTeamUnlimited extends BridgeTeam {
    private final UUID blueprintUUID;
    public BridgeTeamUnlimited(UUID blueprintUUID, int minPlayers, int maxPlayers, int teamNumber, String color, List<BoundingBox> goal, BoundingBox spawnBox, Location spawnLoc, Location respawnLoc, UUID arenaUniqueId) {
        super(minPlayers, maxPlayers, teamNumber, color, goal, spawnBox, spawnLoc, respawnLoc, arenaUniqueId);
        this.blueprintUUID = blueprintUUID;
    }
    @Override
    public void start(World world, boolean atStart, Runnable onDone) {
        BSchematic.pasteSchematic(world, blueprintUUID + "-box"+ getTeamNumber() +".schem", (a) -> {
            onDone.run();
        });
        getPlayers().forEach(uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            showScoreboard(p);
            showTablist(uuid);
            if(atStart)
                updateHealthScoreboard(p);
        });
    }
}
