package me.bramar.thebridge.tablist;

import me.bramar.thebridge.TheBridge;
import me.bramar.thebridge.placeholders.BridgePlaceholders;
import me.bramar.thebridge.util.nms.NMSEntityPlayer;
import me.bramar.thebridge.util.nms.NMSUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class TablistHeaderFooterTask extends BukkitRunnable {
    private static final String EMPTY_JSON = "{\"translate\":\"\"}";
    private final UUID uuid;
    private final NMSUtil nms = TheBridge.getInstance().getNMSUtil();
    private final TablistManager manager;
    private final BridgePlaceholders placeholders;
    private int tick = 0;
    private String lastHeader, lastFooter;

    public TablistHeaderFooterTask(TablistManager manager, UUID uuid) {
        this.manager = manager;
        this.uuid = uuid;
        this.placeholders = manager.getPlugin().getPlaceholders();
    }

    @Override
    public void run() {
        Player p = Bukkit.getPlayer(uuid);
        if(p != null) {
            if(tick > manager.getTotalTick())
                tick = 0;
            updateTablist(p, tick);
            tick++;
        }
    }
    // applyPlaceholder
    public String p(Player p, String str) {
        return ChatColor.translateAlternateColorCodes('&', placeholders.applyPlaceholders(p, str));
    }
    public void updateTablist(Player p, int tick) {
        String header, footer;
        if(!manager.isHeaderAvailable()) {
            header = EMPTY_JSON;
        }else if(manager.getHeader().containsKey(tick)) {
            header = manager.getHeader().get(tick);
            header = "{\"text\":\"" +
                    p(p, header).replace("\"", "\\\"").replace("\n", "\\n")
                    + "\"}"; // " -> \", New line -> \n (literal)
            lastHeader = header;
        }else {
            header = lastHeader;
        }
        if(!manager.isFooterAvailable()) {
            footer = EMPTY_JSON;
        }else if(manager.getFooter().containsKey(tick)) {
            footer = manager.getFooter().get(tick);
            footer = "{\"text\":\"" + p(p, footer).replace("\"", "\\\"").replace("\n", "\\n")
                    + "\"}";
            lastFooter = footer;
        }else {
            footer = lastFooter;
        }

        NMSEntityPlayer nmsPlayer = nms.getPlayer(p);
        nms.newPlayerListHeaderFooterPacket(header, footer).sendPacket(nmsPlayer);
    }
    public void resetTablist() {
        Player p = Bukkit.getPlayer(uuid);
        if(p != null) {
            nms.newPlayerListHeaderFooterPacket(EMPTY_JSON, EMPTY_JSON).sendPacket(nms.getPlayer(p));
        }
    }
}
