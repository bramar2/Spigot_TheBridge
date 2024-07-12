package me.bramar.thebridge.util.nms.v1_8_R3;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import me.bramar.thebridge.util.nms.NMSBlockData;
import me.bramar.thebridge.util.nms.NMSChunk;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.IBlockData;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Chunk implements NMSChunk {
    private final net.minecraft.server.v1_8_R3.Chunk original;

    @Override
    public void setBlockData(int x, int y, int z, NMSBlockData bd) {
        BlockPosition pos = new BlockPosition(x, y, z);
        original.a(pos, (IBlockData) bd.getOriginal());
    }

    @Override
    public int getLocX() {
        return original.locX;
    }

    @Override
    public int getLocZ() {
        return original.locZ;
    }

    @Override
    public Object getOriginal() {
        return original;
    }
}
