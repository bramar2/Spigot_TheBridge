package me.bramar.thebridge.util.nms.v1_8_R3;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import me.bramar.thebridge.util.nms.NMSEntityPlayer;
import me.bramar.thebridge.util.nms.packet.ProfileProperty;
import net.minecraft.server.v1_8_R3.ChatMessage;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import net.minecraft.server.v1_8_R3.PacketPlayOutTitle;

import java.util.Collection;
import java.util.stream.Collectors;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class EntityPlayer implements NMSEntityPlayer {
    private final net.minecraft.server.v1_8_R3.EntityPlayer original;
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
        PacketPlayOutChat packet = new PacketPlayOutChat(chat, (byte) 2);
        original.playerConnection.sendPacket(packet);
    }

    @Override
    public int getPing() {
        return original.ping;
    }

    @Override
    public Object getOriginal() {
        return original;
    }

    @Override
    public Collection<ProfileProperty> getProfileProperties() {
        return original.getProfile().getProperties().values()
                .stream().map(property -> new ProfileProperty(property.getName(), property.getValue(), property.getSignature()))
                .collect(Collectors.toSet());
    }

    @Override
    public void legacySendTitle(String title, String subTitle, int fadeIn, int stay, int fadeOut) throws UnsupportedOperationException {
        PacketPlayOutTitle titlePacket = title == null ? null : new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TITLE, IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + title + "\"}"), fadeIn, stay, fadeOut);
        PacketPlayOutTitle subtitlePacket = subTitle == null ? null : new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.SUBTITLE, IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + subTitle + "\"}"), fadeIn, stay, fadeOut);
        if(titlePacket != null) original.playerConnection.sendPacket(titlePacket);
        if(subtitlePacket != null) original.playerConnection.sendPacket(subtitlePacket);
    }
}
