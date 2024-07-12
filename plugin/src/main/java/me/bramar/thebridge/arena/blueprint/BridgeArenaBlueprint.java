package me.bramar.thebridge.arena.blueprint;

import lombok.Getter;
import me.bramar.thebridge.TheBridge;
import me.bramar.thebridge.arena.BridgeArena;
import me.bramar.thebridge.arena.BridgeTeam;
import me.bramar.thebridge.model.BoundingBox;
import me.bramar.thebridge.model.GameState;
import me.bramar.thebridge.model.Location3D;
import me.bramar.thebridge.util.schematic.BSchematic;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Getter
public class BridgeArenaBlueprint {
    private String worldPrefixName;
    private BoundingBox arenaBox, placeBox;
    private int voidY, placeMinY, placeMaxY;
    private List<BoundingBox> disallowPlace, allowBreak, disallowBreak;
    private BridgeTeamBlueprint team1, team2;
    private String mapName;
    private Location3D joinLocation;
    private int neededPlayers, maxPlayers;
    private File file;
    private UUID blueprintUUID;
    private BridgeArenaBlueprint() {}

    public void generateWorld(int id, Consumer<World> consumer) {
        WorldCreator worldCreator = new WorldCreator(worldPrefixName + id)
                .generateStructures(false)
                .environment(World.Environment.NORMAL)
                .type(WorldType.FLAT)
                .generatorSettings("0;");
        World world = worldCreator.createWorld();
        world.setAutoSave(false);
        BSchematic.pasteSchematic(world, blueprintUUID + ".schem", _o -> consumer.accept(world));
    }

    public void createArena(Consumer<BridgeArenaUnlimited> consumer) {
        int id;
        do {
            id = TheBridge.getInstance().getRandom().nextInt(9999);
        }while(Bukkit.getWorld(worldPrefixName + id) != null);
        int finalId = id;
        generateWorld(id, world -> {
            String arenaName = mapName + finalId;
            UUID uuid = TheBridge.getInstance().getArenaManager().newUUID();
            BridgeTeam team1 = this.team1.createTeam(world, blueprintUUID, uuid);
            BridgeTeam team2 = this.team2.createTeam(world, blueprintUUID, uuid);
            BridgeArenaUnlimited arena = new BridgeArenaUnlimited(
                    world,
                    world.getName(),
                    arenaBox, placeBox, voidY, placeMinY, placeMaxY, disallowPlace, allowBreak, disallowBreak, team1, team2, mapName, arenaName, joinLocation.toBukkit(world), neededPlayers, maxPlayers,
                    uuid, file);
            arena.loadEvents(TheBridge.getInstance());
            arena.setAutoClose(); // closes if noone in 30s joins
            consumer.accept(arena);
        });
    }

    public static BridgeArenaBlueprint of(BridgeArena arena) {
        BridgeArenaBlueprint blueprint = new BridgeArenaBlueprint();
        blueprint.worldPrefixName = arena.getWorldName();
        blueprint.arenaBox = arena.getArenaBox();
        blueprint.placeBox = arena.getPlaceBox();
        blueprint.voidY = arena.getVoidY();
        blueprint.placeMinY = arena.getPlaceMinY();
        blueprint.placeMaxY = arena.getPlaceMaxY();
        blueprint.disallowPlace = arena.getDisallowPlace();
        blueprint.allowBreak = arena.getAllowBreak();
        blueprint.disallowBreak = arena.getDisallowBreak();
        blueprint.team1 = BridgeTeamBlueprint.of(arena.getTeam1());
        blueprint.team2 = BridgeTeamBlueprint.of(arena.getTeam2());
        blueprint.mapName = arena.getMapName();
        blueprint.joinLocation = new Location3D(arena.getJoinLocation());
        blueprint.neededPlayers = arena.getNeededPlayers();
        blueprint.maxPlayers = arena.getMaxPlayers();
        blueprint.file = arena.getFile();
        blueprint.blueprintUUID = arena.getUniqueId();
        return blueprint;
    }
}
