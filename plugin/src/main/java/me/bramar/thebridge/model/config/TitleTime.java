package me.bramar.thebridge.model.config;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.bramar.thebridge.TheBridge;
import me.bramar.thebridge.util.CompatibilityUtils;
import org.bukkit.entity.Player;

@Getter @AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TitleTime {
    private final int fadeIn, stay, fadeOut;

    public static TitleTime create(String combined) {
        String[] split = combined.split("-");
        try {
            if(split.length == 1) {
                return new TitleTime(0, Integer.parseInt(split[0]), 0);
            }else if(split.length >= 3) {
                return new TitleTime(
                        Integer.parseInt(split[0]),
                        Integer.parseInt(split[1]),
                        Integer.parseInt(split[2])
                );
            }
        }catch(Exception ignored) {}
        return null;
    }

    public void sendTitle(Player p, String title, String subTitle) {
        if(CompatibilityUtils.isModernSendTitle())
            p.sendTitle(title, subTitle, fadeIn, stay, fadeOut);
        else
            TheBridge.getInstance().getNMSUtil().getPlayer(p).legacySendTitle(title, subTitle, fadeIn, stay, fadeOut);
    }

    @Override
    public String toString() {
        return fadeIn + "-" + stay + "-" + fadeOut;
    }
}
