package me.bramar.thebridge.util.nms.v1_12_R1;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import me.bramar.thebridge.util.nms.NMSBlockData;
import me.bramar.thebridge.util.nms.NMSChunk;
import me.bramar.thebridge.util.nms.NMSEntityPlayer;
import me.bramar.thebridge.util.nms.NMSWorld;
import net.minecraft.server.v1_12_R1.BlockPosition;
import net.minecraft.server.v1_12_R1.IBlockData;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftInventoryPlayer;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class World implements NMSWorld {
    private final net.minecraft.server.v1_12_R1.World original;

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
            if(entityHuman instanceof net.minecraft.server.v1_12_R1.EntityPlayer)
                players.add(new EntityPlayer((net.minecraft.server.v1_12_R1.EntityPlayer) entityHuman));
        });
        return players;
    }
}
