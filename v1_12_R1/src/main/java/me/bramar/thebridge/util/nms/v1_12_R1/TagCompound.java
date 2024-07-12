package me.bramar.thebridge.util.nms.v1_12_R1;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import me.bramar.thebridge.util.nms.NMSTagCompound;
import net.minecraft.server.v1_12_R1.NBTTagCompound;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class TagCompound implements NMSTagCompound {
    private final NBTTagCompound original;

    @Override
    public Object getOriginal() {
        return original;
    }

    @Override
    public String getString(String var1) {
        return original.getString(var1);
    }
}
