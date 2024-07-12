package me.bramar.thebridge.util.nms.packet;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProfileProperty {
    private final String name, value, signature;

    public boolean hasSignature() {
        return signature != null;
    }
}
