package me.bramar.thebridge.arena;

import me.bramar.thebridge.TheBridge;
import me.bramar.thebridge.config.PlayerData;
import me.bramar.thebridge.config.PlayerDatabase;
import me.bramar.thebridge.util.nms.NMSUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;

public class ShootCooldown extends BukkitRunnable {
    private final boolean isActionbar;
    private final Player player;
    private final NMSUtil nms = TheBridge.getInstance().getNMSUtil();
    private final long ticks;
    private final long tickCalc;
    private final Map<UUID, ?> arrowCooldown;

    private final PlayerDatabase playerDatabase = TheBridge.getInstance().getPlayerDatabase();

    public ShootCooldown(Player player, Map<UUID, ?> arrowCooldown, int ticks, boolean isActionbar) {
        this.isActionbar = isActionbar;
        this.player = player;
        this.tickCalc = TheBridge.getCurrentTick() + ticks;
        this.arrowCooldown = arrowCooldown;
        this.ticks = ticks;
    }

    @Override
    public synchronized void cancel() throws IllegalStateException {
        arrowCooldown.remove(player.getUniqueId());
        player.setLevel(0);
        player.setTotalExperience(0);
        player.setExp(0f);
        Player p = Bukkit.getPlayer(player.getUniqueId());
        if(p != null) {
            p.setLevel(0);
            p.setTotalExperience(0);
            p.setExp(0f);
            if(isActionbar) {
                if(p.isOnline())
                    nms.getPlayer(player).sendActionbar(" ");
            }
        }
        super.cancel();
    }

    @Override
    public void run() {
        if(!player.isOnline()) {
            cancel();
            return;
        }
        long currentTick = TheBridge.getCurrentTick();
        long diff = tickCalc - currentTick; // Time left until arrow is given
        if(diff <= 0) { // has been *TICKS* time
            PlayerData playerData = playerDatabase.getPlayerData(player.getUniqueId());
            playerData.giveArrowOnPlayer(player);
            if(isActionbar) {
                // last set of actionbar
                double seconds = round(diff / 20d);
                String message = ChatColor.GREEN + "■■■■■■■■ " + ChatColor.GOLD + seconds + " seconds";

                Player p = Bukkit.getPlayer(player.getUniqueId());
                if(p != null && p.isOnline()) {
                    nms.getPlayer(p).sendActionbar(message);
                }
            }
            cancel();
            return;
        }
        Player p = Bukkit.getPlayer(player.getUniqueId());
        if(isActionbar) {
            float percentage = 1f - ((float) diff / ticks);
            int greenAmount = (int) (percentage * 8f);
            String symbol = "■";
            StringBuilder message = new StringBuilder("" + ChatColor.GREEN);
            boolean changedToRed = false;
            for(int i = 0; i < 8; i++) {
                if(!changedToRed) {
                    if(i >= greenAmount) {
                        changedToRed = true;
                        message.append(ChatColor.RED);
                    }
                }
                message.append(symbol);
            }

            double seconds = round(diff / 20d);

            message.append(" ").append(ChatColor.GOLD).append(seconds).append(" seconds");

            if(p != null && p.isOnline()) {
                nms.getPlayer(p).sendActionbar(message.toString());
            }
        }else {
            int secondsLeft = (int) Math.ceil(diff / 20d);
            int ticksLeft = (int) (diff - (secondsLeft * 20)) + 20;
            float xp = ticksLeft / 20f;
            player.setLevel(secondsLeft - 1);
            player.setExp(xp);
            if(p != null) {
                p.setLevel(secondsLeft - 1);
                p.setExp(xp);
            }
        }
    }
    private double round(double value) {
//        int scale = (int) Math.pow(10, 1);
        int scale = 10;
        return (double) Math.round(value * scale) / scale;
    }
}
