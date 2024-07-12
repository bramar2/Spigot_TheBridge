package me.bramar.thebridge.util.nms.v1_8_R3;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import me.bramar.thebridge.util.nms.NMSBlockData;
import me.bramar.thebridge.util.nms.NMSChunk;
import me.bramar.thebridge.util.nms.NMSEntityPlayer;
import me.bramar.thebridge.util.nms.NMSWorld;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.IBlockData;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerListHeaderFooter;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class World implements NMSWorld {
    private final net.minecraft.server.v1_8_R3.World original;

    @Override
    public Object getOriginal() {
        return original;
    }

    @Override
    public void setTypeAndData(int x, int y, int z, NMSBlockData bd) {
        original.setTypeAndData(new BlockPosition(x, y, z), (IBlockData) bd.getOriginal(), 2);
    }

    @Override
    public NMSChunk getChunkAt(int x, int z) {
        return new Chunk(original.getChunkAt(x, z));
    }

    @Override
    public List<NMSEntityPlayer> getPlayers() {
        List<NMSEntityPlayer> players = new ArrayList<>();
        original.players.forEach(entityHuman -> {
            if(entityHuman instanceof net.minecraft.server.v1_8_R3.EntityPlayer)
                players.add(new EntityPlayer((net.minecraft.server.v1_8_R3.EntityPlayer) entityHuman));
        });
        return players;
    }
}
