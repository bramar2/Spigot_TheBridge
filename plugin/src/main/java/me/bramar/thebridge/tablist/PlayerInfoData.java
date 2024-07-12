package me.bramar.thebridge.tablist;

import lombok.Getter;
import me.bramar.thebridge.TheBridge;
import me.bramar.thebridge.util.nms.packet.ProfileProperty;
import org.bukkit.entity.Player;

import java.util.Collection;

// for some data used in tablist if the player leaves/disconnects
@Getter
public class PlayerInfoData {
    private final String name;
    private final Collection<ProfileProperty> profileProperties;

    public PlayerInfoData(Player p) {
        name = p.getName();
        profileProperties = TheBridge.getInstance().getNMSUtil().getPlayer(p).getProfileProperties();
    }
}
