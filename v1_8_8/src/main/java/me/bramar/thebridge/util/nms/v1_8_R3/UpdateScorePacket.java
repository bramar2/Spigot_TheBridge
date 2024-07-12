package me.bramar.thebridge.util.nms.v1_8_R3;

import io.netty.buffer.Unpooled;
import me.bramar.thebridge.util.nms.NMSEntityPlayer;
import me.bramar.thebridge.util.nms.packet.NMSUpdateScorePacket;
import net.minecraft.server.v1_8_R3.PacketDataSerializer;
import net.minecraft.server.v1_8_R3.PacketPlayOutScoreboardScore;

import java.io.IOException;

public class UpdateScorePacket implements NMSUpdateScorePacket {
    private final String entityName, objectiveName;
    private final int action;
    private final Integer value;
    private PacketPlayOutScoreboardScore packet;
    public UpdateScorePacket(String entityName, String objectiveName, int action, Integer value) {
        this.entityName = entityName;
        this.objectiveName = objectiveName;
        this.action = action;
        this.value = value;
    }

    public void serialize(PacketDataSerializer packetData) {
        packetData.a(this.entityName);
        packetData.b(this.action);
        packetData.a(this.objectiveName);
//        if(this.d != PacketPlayOutScoreboardScore.EnumScoreboardAction.REMOVE) {
//            var1.b(this.c);
//        }
        if(this.action != 1) {
            packetData.b(this.value);
        }
    }

    @Override
    public NMSUpdateScorePacket createNMS() {
        packet = new PacketPlayOutScoreboardScore();
        try {
            PacketDataSerializer packetData = new PacketDataSerializer(Unpooled.buffer());
            serialize(packetData);
            packet.a(packetData);
        }catch(IOException e1) {
            e1.printStackTrace();
            throw new RuntimeException(e1);
        }
        return this;
    }

    @Override
    public Object getOriginal() {
        return packet;
    }

    @Override
    public void sendPacket(NMSEntityPlayer p) {
        ((net.minecraft.server.v1_8_R3.EntityPlayer) p.getOriginal()).playerConnection.sendPacket(packet);
    }
}
