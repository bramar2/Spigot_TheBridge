package me.bramar.thebridge.util.nms.packet;

import me.bramar.thebridge.util.nms.NMSEntityPlayer;
import me.bramar.thebridge.util.nms.NMSOriginal;

public interface NMSUpdateScorePacket extends NMSOriginal {
    NMSUpdateScorePacket createNMS();
    void sendPacket(NMSEntityPlayer p);
}
