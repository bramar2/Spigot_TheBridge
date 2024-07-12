package me.bramar.thebridge.util.nms.v1_8_R3;

import me.bramar.thebridge.util.nms.NMSBlockData;
import me.bramar.thebridge.util.nms.NMSEntityPlayer;
import me.bramar.thebridge.util.nms.NMSItemStack;
import me.bramar.thebridge.util.nms.NMSWorld;
import me.bramar.thebridge.util.nms.packet.*;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftArrow;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftInventoryPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NMSUtil implements me.bramar.thebridge.util.nms.NMSUtil {
    private final List<NMSBlockData> blockData = new ArrayList<>();
    private final List<NMSWorld> worlds = new ArrayList<>();
    private final net.minecraft.server.v1_8_R3.ItemStack nullItem = new net.minecraft.server.v1_8_R3.ItemStack((Item) null);

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
        net.minecraft.server.v1_8_R3.World nmsWorld = ((CraftWorld) bukkitWorld).getHandle();
        for(NMSWorld world : worlds) {
            if(world.getOriginal() == nmsWorld)
                return world;
        }
        NMSWorld world = new me.bramar.thebridge.util.nms.v1_8_R3.World(nmsWorld);
        worlds.add(world);
        return world;
    }

    @Override
    public NMSItemStack getItem(ItemStack bukkitItem) {
        return new me.bramar.thebridge.util.nms.v1_8_R3.ItemStack(CraftItemStack.asNMSCopy(bukkitItem));
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
    public void legacySetPickupStatus(Arrow arrow, int ordinal) throws UnsupportedOperationException {
        EntityArrow nmsArrow = ((CraftArrow) arrow).getHandle();
        nmsArrow.fromPlayer = ordinal;
    }

    private ItemStack[] asCraftMirror(List<net.minecraft.server.v1_8_R3.ItemStack> mcItems) {
        int size = mcItems.size();
        ItemStack[] items = new ItemStack[size];

        for(int i = 0; i < size; ++i) {
            net.minecraft.server.v1_8_R3.ItemStack mcItem = mcItems.get(i);
            items[i] = isEmpty(mcItem) ? null : CraftItemStack.asCraftMirror(mcItem);
        }

        return items;
    }
    public boolean isEmpty(net.minecraft.server.v1_8_R3.ItemStack itemStack) {
        if(itemStack == null)
            return true;
        Item item = itemStack.getItem();
        // itemStack.getData() = damage of itemStack
        return nullItem.equals(itemStack) ? true : (item != null && item != Item.getItemOf(Blocks.AIR) ? (itemStack.count <= 0 ? true :
                itemStack.getData() < -32768 || itemStack.getData() > 65535) : true);
    }

    @Override
    public ItemStack[] legacyGetStorageContents(Player player) throws UnsupportedOperationException {
        CraftInventoryPlayer craftInventory = (CraftInventoryPlayer) player.getInventory();
        return asCraftMirror(Arrays.asList(craftInventory.getInventory().items));
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
}
