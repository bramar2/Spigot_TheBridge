package me.bramar.thebridge.arena.blueprint;

import lombok.Getter;
import me.bramar.thebridge.arena.BridgeArena;
import me.bramar.thebridge.arena.BridgeTeam;
import me.bramar.thebridge.model.BoundingBox;
import me.bramar.thebridge.model.Location3D;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;
import java.util.UUID;

@Getter
public class BridgeTeamBlueprint {
    private int minPlayers, maxPlayers;
    private int teamNumber;
    private String color;
    private List<BoundingBox> goal;
    private BoundingBox spawnBox;
    private Location3D spawnLoc, respawnLoc;
    private BridgeTeamBlueprint() {}

    public BridgeTeam createTeam(World world, UUID blueprintId, UUID arenaId) {
        return new BridgeTeamUnlimited(blueprintId, minPlayers, maxPlayers, teamNumber, color, goal, spawnBox, spawnLoc.toBukkit(world), respawnLoc.toBukkit(world), arenaId);
    }


    public static BridgeTeamBlueprint of(BridgeTeam team) {
        BridgeTeamBlueprint blueprint = new BridgeTeamBlueprint();
        blueprint.minPlayers = team.getMinPlayers();
        blueprint.maxPlayers = team.getMaxPlayers();
        blueprint.teamNumber = team.getTeamNumber();
        blueprint.color = team.getColor();
        blueprint.goal = team.getGoal();
        blueprint.spawnBox = team.getSpawnBox();
        blueprint.spawnLoc = new Location3D(team.getSpawnLoc());
        blueprint.respawnLoc = new Location3D(team.getRespawnLoc());
        return blueprint;
    }
}
