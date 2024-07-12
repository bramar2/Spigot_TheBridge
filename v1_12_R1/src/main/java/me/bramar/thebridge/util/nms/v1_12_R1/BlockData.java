package me.bramar.thebridge.util.nms.v1_12_R1;

import me.bramar.thebridge.util.nms.NMSBlockData;
import net.minecraft.server.v1_12_R1.Block;
import net.minecraft.server.v1_12_R1.IBlockData;

class BlockData implements NMSBlockData {

    private final IBlockData original;
    private final int id, data;
    protected BlockData(int id, int data) {
        this.id = id;
        this.data = data;
        this.original = Block.getById(id).fromLegacyData(data);
    }

    @Override
    public Object getOriginal() {
        return original;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getData() {
        return data;
    }
}
