package me.bramar.thebridge.util.nms;

import org.bukkit.inventory.ItemStack;

public interface NMSItemStack extends NMSOriginal {
    boolean hasTag();
    NMSTagCompound getTag();
    void setTag(NMSTagCompound tag);
    ItemStack asBukkit();
}
