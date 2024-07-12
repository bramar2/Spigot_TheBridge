package me.bramar.thebridge.util.nms;

import java.util.List;

public interface NMSWorld extends NMSOriginal {
    void setTypeAndData(int x, int y, int z, NMSBlockData bd); // setTypeAndData(pos, ibd, 2); 2 -> noPhysics
    NMSChunk getChunkAt(int x, int z);
    List<NMSEntityPlayer> getPlayers();
}
