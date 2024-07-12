package me.bramar.thebridge.util.nms.v1_12_R1;

import io.netty.buffer.Unpooled;
import me.bramar.thebridge.util.nms.NMSEntityPlayer;
import me.bramar.thebridge.util.nms.packet.NMSPlayerListHeaderFooterPacket;
import net.minecraft.server.v1_12_R1.IChatBaseComponent;
import net.minecraft.server.v1_12_R1.PacketDataSerializer;
import net.minecraft.server.v1_12_R1.PacketPlayOutPlayerListHeaderFooter;

public class PlayerListHeaderFooterPacket implements NMSPlayerListHeaderFooterPacket {
    private final PacketPlayOutPlayerListHeaderFooter packet;
    public PlayerListHeaderFooterPacket(IChatBaseComponent header, IChatBaseComponent footer) {
        this.packet = new PacketPlayOutPlayerListHeaderFooter();
        PacketDataSerializer serializer = new PacketDataSerializer(Unpooled.buffer());
        serializer.a(header);
        serializer.a(footer);
        try {
            packet.a(serializer); // a = deserialize()
        }catch(Exception ignored) {}
    }

    @Override
    public void sendPacket(NMSEntityPlayer p) {
        ((net.minecraft.server.v1_12_R1.EntityPlayer) p.getOriginal()).playerConnection.sendPacket(packet);
    }

    @Override
    public Object getOriginal() {
        return packet;
    }
}
