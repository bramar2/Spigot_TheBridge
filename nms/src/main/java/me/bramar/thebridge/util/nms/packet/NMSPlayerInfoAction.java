package me.bramar.thebridge.util.nms.packet;

import lombok.AccessLevel;
import lombok.Getter;
import org.bukkit.GameMode;

import java.util.Collection;
import java.util.UUID;

@Getter
public class NMSPlayerInfoAction {
    @Getter(AccessLevel.NONE)
    private final UUID uuid;
    private int gamemode;
    private int ping;
    private String displayName;

    private String nameToAdd;
    private Collection<ProfileProperty> propertiesToAdd;
    public NMSPlayerInfoAction(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUUID() {
        return uuid;
    }

    public NMSPlayerInfoAction setGamemode(int gamemode) {
        this.gamemode = gamemode;
        return this;
    }
    public NMSPlayerInfoAction setGamemode(GameMode gamemode) {
        switch(gamemode) {
            case SURVIVAL:
                this.gamemode = 0;
                break;
            case CREATIVE:
                this.gamemode = 1;
                break;
            case ADVENTURE:
                this.gamemode = 2;
                break;
            case SPECTATOR:
                this.gamemode = 3;
                break;
            default:
                break;
        }
        return this;
    }
    public NMSPlayerInfoAction setPing(int ping) {
        this.ping = ping;
        return this;
    }
    public NMSPlayerInfoAction setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }
    public NMSPlayerInfoAction setNameToAdd(String nameToAdd) {
        this.nameToAdd = nameToAdd;
        return this;
    }
    public NMSPlayerInfoAction setPropertiesToAdd(Collection<ProfileProperty> propertiesToAdd) {
        this.propertiesToAdd = propertiesToAdd;
        return this;
    }

}
