package me.bramar.thebridge.util.nms.v1_12_R1;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import me.bramar.thebridge.util.nms.NMSEntityPlayer;
import me.bramar.thebridge.util.nms.packet.ProfileProperty;
import net.minecraft.server.v1_12_R1.ChatMessage;
import net.minecraft.server.v1_12_R1.IChatBaseComponent;
import net.minecraft.server.v1_12_R1.PacketPlayOutTitle;

import java.util.Collection;
import java.util.stream.Collectors;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class EntityPlayer implements NMSEntityPlayer {
    private final net.minecraft.server.v1_12_R1.EntityPlayer original;
    @Override
    public double getLocX() {
        return original.locX;
    }

    @Override
    public double getLocZ() {
        return original.locZ;
    }

    @Override
    public void sendActionbar(String message) {
        IChatBaseComponent chat = new ChatMessage(message);
        PacketPlayOutTitle packet = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.ACTIONBAR, chat);
        original.playerConnection.sendPacket(packet);
    }

    @Override
    public int getPing() {
        return original.ping;
    }

    @Override
    public Collection<ProfileProperty> getProfileProperties() {
        return original.getProfile().getProperties().values()
                .stream().map(property -> new ProfileProperty(property.getName(), property.getValue(), property.getSignature()))
                .collect(Collectors.toSet());
    }

    @Override
    public Object getOriginal() {
        return original;
    }

    @Override
    public void legacySendTitle(String title, String subTitle, int fadeIn, int stay, int fadeOut) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}
