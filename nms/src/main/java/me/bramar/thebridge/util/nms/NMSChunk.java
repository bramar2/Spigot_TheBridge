package me.bramar.thebridge.util.nms;

public interface NMSChunk extends NMSOriginal {
    void setBlockData(int x, int y, int z, NMSBlockData bd);

    int getLocX();
    int getLocZ();
}
