package me.bramar.thebridge.util.nms.v1_8_R3;

import me.bramar.thebridge.util.nms.NMSBlockData;
import me.bramar.thebridge.util.nms.NMSEntityPlayer;
import me.bramar.thebridge.util.nms.packet.NMSMultiBlockChangeInfo;
import me.bramar.thebridge.util.nms.packet.NMSMultiBlockChangePacket;
import net.minecraft.server.v1_8_R3.ChunkCoordIntPair;
import net.minecraft.server.v1_8_R3.IBlockData;
import net.minecraft.server.v1_8_R3.PacketPlayOutMultiBlockChange;

import java.lang.reflect.Field;

public class MultiBlockChangePacket implements NMSMultiBlockChangePacket {
    private static Field chunkField;
    private static Field infoField;
    static {
        // only gets loaded if its same version
        Field chunkField = null;
        Field infoField = null;
        try {
            chunkField = PacketPlayOutMultiBlockChange.class.getDeclaredField("a");
            infoField = PacketPlayOutMultiBlockChange.class.getDeclaredField("b");
            chunkField.setAccessible(true);
            infoField.setAccessible(true);
        }catch(Exception e1) {
            e1.printStackTrace();
            System.out.println("Reflection error with PacketPlayOutMultiBlockChange");
        }
        MultiBlockChangePacket.chunkField = chunkField;
        MultiBlockChangePacket.infoField = infoField;
    }

    private final PacketPlayOutMultiBlockChange original;

    protected MultiBlockChangePacket() {
        original = new PacketPlayOutMultiBlockChange();
    }

    @Override
    public void setChunk(int x, int z) {
        try {
            chunkField.set(original, new ChunkCoordIntPair(x, z));
        }catch(IllegalAccessException e1) {
            e1.printStackTrace();
            System.out.println("IllegalAccessException error at reflection even though field has been set as accessible.");
        }
    }
    @Override
    public NMSMultiBlockChangeInfo newInfo(int perChunkX, int y, int perChunkZ, NMSBlockData bd) {
        return newInfo((short) ((perChunkX & 15) << 12 | (perChunkZ & 15) << 8 | y), bd);
//        IBlockData ibd = (IBlockData) bd.getOriginal();
//        short loc = (short) ((perChunkX & 15) << 12 | (perChunkZ & 15) << 8 | y);
//        return new MultiBlockChangeInfo(loc, ibd);
    }
    @Override
    public NMSMultiBlockChangeInfo newInfo(short loc, NMSBlockData bd) {
        IBlockData ibd = (IBlockData) bd.getOriginal();
        return new MultiBlockChangeInfo(loc, ibd);
    }

    @Override
    public void setInfo(NMSMultiBlockChangeInfo[] info) {
        try {
            PacketPlayOutMultiBlockChange.MultiBlockChangeInfo[] infoArray = new PacketPlayOutMultiBlockChange.MultiBlockChangeInfo[info.length];
            for(int i = 0; i < info.length; i++) {
                infoArray[i] = (PacketPlayOutMultiBlockChange.MultiBlockChangeInfo) info[i].asNMS(this);
            }
            infoField.set(original, infoArray);
        }catch(IllegalAccessException e1) {
            e1.printStackTrace();
            System.out.println("IllegalAccessException error at reflection even though field has been set as accessible.");
        }
    }

    @Override
    public void sendPacket(NMSEntityPlayer p) {
        ((net.minecraft.server.v1_8_R3.EntityPlayer) p.getOriginal()).playerConnection.sendPacket(original);
    }

    @Override
    public Object getOriginal() {
        return original;
    }
}
