package me.bramar.thebridge.util.nms.packet;

import me.bramar.thebridge.util.nms.NMSBlockData;
import me.bramar.thebridge.util.nms.NMSEntityPlayer;
import me.bramar.thebridge.util.nms.NMSOriginal;

public interface NMSMultiBlockChangePacket extends NMSOriginal {
    void setChunk(int x, int z);
    void setInfo(NMSMultiBlockChangeInfo[] info);
    void sendPacket(NMSEntityPlayer ep);
    NMSMultiBlockChangeInfo newInfo(int perChunkX, int y, int perChunkZ, NMSBlockData bd);
    NMSMultiBlockChangeInfo newInfo(short loc, NMSBlockData bd);
}
