package me.bramar.thebridge.model.config;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.bramar.thebridge.util.CompatibilityUtils;
import me.bramar.thebridge.util.XSound;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

@Getter @AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SoundType {
    public static final SoundType NULL = new SoundType(null, 0, 0, null) {
        @Override
        public void playSound(Player p) {}
    };

    private Sound sound;
    private float volume, pitch;
    private Object category;
    private boolean isNull = false;

    private SoundType(Sound sound, float volume, float pitch, Object category) {
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
        this.category = category;
    }

    public static SoundType create(String combined) {
        if(combined.equalsIgnoreCase("null"))
            return NULL;
        try {
            boolean hasSoundCategory = CompatibilityUtils.hasSoundCategory();
            String[] split = combined.split(":");
            if(hasSoundCategory) {
                SoundCategory category = SoundCategory.MASTER;
                if(split.length > 3)
                    try {
                        category = SoundCategory.valueOf(split[3].toUpperCase());
                    }catch(Exception ignored) {}
                XSound xSound = XSound.matchXSound(split[0].toUpperCase()).get();
                return new SoundType(xSound.parseSound(),
                        Float.parseFloat(split[1]),
                        Float.parseFloat(split[2]),
                        category);
            }else {
                XSound xSound = XSound.matchXSound(split[0].toUpperCase()).get();
                return new SoundType(xSound.parseSound(),
                        Float.parseFloat(split[1]),
                        Float.parseFloat(split[2]),
                        null);
            }
        }catch(Exception e1) {
            e1.printStackTrace();
        }
        return NULL; // tells it to grab the default
    }

    public void playSound(Player p) {
        if(category == null || !CompatibilityUtils.hasSoundCategory())
            p.playSound(p.getLocation(), sound, volume, pitch);
        else
            p.playSound(p.getLocation(), sound, (SoundCategory) category, volume, pitch);
    }

    @Override
    public String toString() {
        return sound.toString() + ":" + volume + ":" + pitch;
    }
}
