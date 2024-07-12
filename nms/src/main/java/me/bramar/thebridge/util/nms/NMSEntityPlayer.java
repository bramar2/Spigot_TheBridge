package me.bramar.thebridge.util.nms;

import me.bramar.thebridge.util.nms.packet.ProfileProperty;

import java.util.Collection;

public interface NMSEntityPlayer extends NMSOriginal {
    double getLocX();
    double getLocZ();
    int getPing();
    Collection<ProfileProperty> getProfileProperties();
    void sendActionbar(String message);
    void legacySendTitle(String title, String subTitle, int fadeIn, int stay, int fadeOut) throws UnsupportedOperationException;
}
