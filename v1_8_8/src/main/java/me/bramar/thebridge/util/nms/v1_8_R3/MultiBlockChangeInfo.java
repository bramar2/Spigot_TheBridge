package me.bramar.thebridge.util.nms.v1_8_R3;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import me.bramar.thebridge.util.nms.packet.NMSMultiBlockChangeInfo;
import me.bramar.thebridge.util.nms.packet.NMSMultiBlockChangePacket;
import net.minecraft.server.v1_8_R3.IBlockData;
import net.minecraft.server.v1_8_R3.PacketPlayOutMultiBlockChange;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class MultiBlockChangeInfo implements NMSMultiBlockChangeInfo {
    private final short loc;
    private final IBlockData ibd;

    @Override
    public Object asNMS(NMSMultiBlockChangePacket packet) {
        return ((PacketPlayOutMultiBlockChange) packet.getOriginal()).new MultiBlockChangeInfo(loc, ibd);
    }
}
