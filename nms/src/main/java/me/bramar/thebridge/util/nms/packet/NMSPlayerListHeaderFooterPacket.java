package me.bramar.thebridge.util.nms.packet;

import me.bramar.thebridge.util.nms.NMSEntityPlayer;
import me.bramar.thebridge.util.nms.NMSOriginal;

public interface NMSPlayerListHeaderFooterPacket extends NMSOriginal {
    void sendPacket(NMSEntityPlayer p);
}
