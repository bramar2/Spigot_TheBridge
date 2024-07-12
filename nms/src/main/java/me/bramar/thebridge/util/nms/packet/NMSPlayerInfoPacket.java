package me.bramar.thebridge.util.nms.packet;

import me.bramar.thebridge.util.nms.NMSEntityPlayer;
import me.bramar.thebridge.util.nms.NMSOriginal;

import java.util.Collection;

public interface NMSPlayerInfoPacket extends NMSOriginal {
    NMSPlayerInfoActionEnum getAction();
    Collection<NMSPlayerInfoAction> getActionList();
    void addAction(NMSPlayerInfoAction action);
    NMSPlayerInfoPacket createNMS();
    void sendPacket(NMSEntityPlayer p);
}
