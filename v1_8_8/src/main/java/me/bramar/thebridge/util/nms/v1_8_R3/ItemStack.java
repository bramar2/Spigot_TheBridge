package me.bramar.thebridge.util.nms.v1_8_R3;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import me.bramar.thebridge.util.nms.NMSItemStack;
import me.bramar.thebridge.util.nms.NMSTagCompound;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemStack implements NMSItemStack {
    private final net.minecraft.server.v1_8_R3.ItemStack original;

    @Override
    public boolean hasTag() {
        return original.hasTag();
    }

    @Override
    public NMSTagCompound getTag() {
        return new TagCompound(original.getTag());
    }

    @Override
    public void setTag(NMSTagCompound tag) {
        original.setTag((NBTTagCompound) tag.getOriginal());
    }

    @Override
    public org.bukkit.inventory.ItemStack asBukkit() {
        return CraftItemStack.asBukkitCopy(original);
    }

    @Override
    public Object getOriginal() {
        return original;
    }
}
