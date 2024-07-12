package me.bramar.thebridge.util.nms.v1_12_R1;

import me.bramar.thebridge.util.nms.*;
import me.bramar.thebridge.util.nms.packet.*;
import net.minecraft.server.v1_12_R1.IChatBaseComponent;
import net.minecraft.server.v1_12_R1.PacketPlayOutPlayerInfo;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NMSUtil implements me.bramar.thebridge.util.nms.NMSUtil {
    private final List<NMSBlockData> blockData = new ArrayList<>();
    private final List<NMSWorld> worlds = new ArrayList<>();

    @Override
    public NMSBlockData getBlockData(int id, int data) {
        for(NMSBlockData bd : blockData) {
            if(bd.getId() == id && bd.getData() == data)
                return bd;
        }
        NMSBlockData n = new BlockData(id, data);
        blockData.add(n);
        return n;
    }

    @Override
    public NMSWorld getWorld(World bukkitWorld) {
        net.minecraft.server.v1_12_R1.World nmsWorld = ((CraftWorld) bukkitWorld).getHandle();
        for(NMSWorld world : worlds) {
            if(world.getOriginal() == nmsWorld)
                return world;
        }
        NMSWorld world = new me.bramar.thebridge.util.nms.v1_12_R1.World(nmsWorld);
        worlds.add(world);
        return world;
    }

    @Override
    public NMSItemStack getItem(ItemStack bukkitItem) {
        return new me.bramar.thebridge.util.nms.v1_12_R1.ItemStack(CraftItemStack.asNMSCopy(bukkitItem));
    }

    @Override
    public NMSMultiBlockChangePacket newMultiBlockChange() {
        return new MultiBlockChangePacket();
    }

    @Override
    public NMSEntityPlayer getPlayer(Player bukkitPlayer) {
        if(bukkitPlayer == null)
            throw new NullPointerException();
        return new EntityPlayer(((CraftPlayer) bukkitPlayer).getHandle());
    }

    @Override
    public NMSPlayerListHeaderFooterPacket newPlayerListHeaderFooterPacket(String headerJson, String footerJson) {
        return new PlayerListHeaderFooterPacket(IChatBaseComponent.ChatSerializer.a(headerJson), IChatBaseComponent.ChatSerializer.a(footerJson));
    }

    @Override
    public NMSPlayerInfoPacket newPlayerInfoPacket(NMSPlayerInfoActionEnum actionEnum) {
        return new PlayerInfoPacket(actionEnum);
    }

    @Override
    public NMSPlayerInfoPacket newPlayerInfoPacket(Object nmsPacket) {
        try {
            return PlayerInfoPacket.of(nmsPacket);
        }catch(IOException e1) {
            e1.printStackTrace();
            throw new RuntimeException(e1);
        }
    }

    @Override
    public NMSUpdateScorePacket newUpdateScorePacket(String entityName, String objectiveName, int action, Integer value) {
        return new UpdateScorePacket(entityName, objectiveName, action, value);
    }

    @Override
    public boolean isPacketPlayOutPlayerInfo(Object packet) {
        return packet instanceof PacketPlayOutPlayerInfo;
    }

    @Override
    public void legacySetPickupStatus(Arrow arrow, int ordinal) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ItemStack[] legacyGetStorageContents(Player player) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}
