package me.bramar.thebridge.util.nms;

import me.bramar.thebridge.util.nms.packet.*;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface NMSUtil {

    NMSBlockData getBlockData(int id, int data);
    NMSWorld getWorld(World bukkitWorld);
    NMSItemStack getItem(ItemStack bukkitItem);
    NMSMultiBlockChangePacket newMultiBlockChange();
    NMSPlayerListHeaderFooterPacket newPlayerListHeaderFooterPacket(String headerJson, String footerJson);
    NMSPlayerInfoPacket newPlayerInfoPacket(NMSPlayerInfoActionEnum actionEnum);
    NMSPlayerInfoPacket newPlayerInfoPacket(Object nmsPacket);
    // integer value is optional, action 0 = create/update, action 1 = remove
    NMSUpdateScorePacket newUpdateScorePacket(String entityName, String objectiveName, int action, Integer value);
    boolean isPacketPlayOutPlayerInfo(Object packet);
    NMSEntityPlayer getPlayer(Player bukkitPlayer);
    void legacySetPickupStatus(Arrow arrow, int ordinal) throws UnsupportedOperationException;
    ItemStack[] legacyGetStorageContents(Player player) throws UnsupportedOperationException;
}
