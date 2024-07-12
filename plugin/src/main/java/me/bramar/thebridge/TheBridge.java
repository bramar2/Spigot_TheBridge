package me.bramar.thebridge;

import lombok.Getter;
import me.bramar.thebridge.arena.ArenaManager;
import me.bramar.thebridge.arena.BridgeArena;
import me.bramar.thebridge.arena.BridgeTeam;
import me.bramar.thebridge.arena.blueprint.BridgeArenaUnlimited;
import me.bramar.thebridge.config.BridgeConfig;
import me.bramar.thebridge.config.PlayerData;
import me.bramar.thebridge.config.PlayerDatabase;
import me.bramar.thebridge.config.TeamConfig;
import me.bramar.thebridge.model.BoundingBox;
import me.bramar.thebridge.model.config.TitleTime;
import me.bramar.thebridge.placeholders.BridgePlaceholders;
import me.bramar.thebridge.scoreboard.ScoreboardManager;
import me.bramar.thebridge.tablist.TablistManager;
import me.bramar.thebridge.util.CompatibilityUtils;
import me.bramar.thebridge.util.nms.NMSUtil;
import me.bramar.thebridge.util.nms.packet.NMSPlayerInfoAction;
import me.bramar.thebridge.util.nms.packet.NMSPlayerInfoActionEnum;
import me.bramar.thebridge.util.nms.packet.NMSPlayerInfoPacket;
import me.bramar.thebridge.util.nms.packet.NMSPlayerListHeaderFooterPacket;
import me.bramar.thebridge.util.schematic.BSchematic;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.CommandBlock;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Getter
public class TheBridge extends JavaPlugin implements Listener {
    String $A1 = "" +
            "All files in/made by the Software (including but not limited to, the .yml, .java and .class files), is licensed under the License," +
            "specified in the LICENSE file and/or the LICENSE.java file and/or the LICENSE.class file";
    @Getter private static TheBridge instance;
    public static final boolean ALLOW_SOLO_GAMES = true; // for debug
    //

    private boolean hasPlaceholderAPI;
    private ScoreboardManager scoreboardManager;
    private TablistManager tablistManager;
    private BridgeConfig bridgeConfig;
    private PlayerDatabase playerDatabase;
    private ArenaManager arenaManager;
    private BridgePlaceholders placeholders;

    private long currentTick = 0;
    private Location pos1, pos2;
    public boolean currentlySavingSchem = false;
    private NMSUtil nmsUtil;
    private String nmsVersion;
    private final Random r = new Random();

    @Override
    public void onEnable() {
        instance = this;
        hasPlaceholderAPI = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        if(!loadNMSUtil()) {
            Bukkit.getPluginManager().disablePlugin(this);
            throw new IllegalArgumentException("NMS not detected");
        }
        getCommand("tb").setExecutor(this);
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("This plugin will load after the server starts.");
        if(CompatibilityUtils.mcVersion() < 12)
            getLogger().warning("It is recommended to use 1.12 or above to avoid bugs and issues. Versions below 1.12 might get discontinued. A 1.12 server with OldCombatMechanics and ViaVersion will give the same feel as a 1.8 server.");
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
            registerSerializable();
            // ORDER MATTERS!!!!!!
            // ArenaManager depends on BridgeConfig, PlayerDatabase
            // BridgeConfig 0 depends
            // PlayerDatabase 0 depends
            // ScoreboardManager 0 depends on load, BridgePlaceholders on use
            // TablistManager 0 depends on load, BridgePlaceholders on use
            // Placeholders UNKNOWN, too many placeholder classes to check
            bridgeConfig = new BridgeConfig();
            playerDatabase = new PlayerDatabase();
            scoreboardManager = new ScoreboardManager();
            tablistManager = new TablistManager();
            arenaManager = new ArenaManager();
            placeholders = new BridgePlaceholders(hasPlaceholderAPI);

            getLogger().info("TheBridge v" + getDescription().getVersion() + " successfully loaded!");
        }, 20);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> currentTick++, 1, 1);
    }

    public boolean loadNMSUtil() {
//        nmsUtil = new me.bramar.thebridge.util.nms.v1_12_R1.NMSUtil();
        nmsVersion = Bukkit.getServer().getClass().getPackage().getName();
        nmsVersion = nmsVersion.substring(nmsVersion.lastIndexOf('.') + 1);
        // TODO; more NMS versions with checks
        if(nmsVersion.equalsIgnoreCase("v1_12_R1")) {
            nmsUtil = new me.bramar.thebridge.util.nms.v1_12_R1.NMSUtil();
        }else if(nmsVersion.equalsIgnoreCase("v1_8_R3")) {
            nmsUtil = new me.bramar.thebridge.util.nms.v1_8_R3.NMSUtil();
        }else {
            getLogger().warning("NMS version undetected or unsupported. Disabling plugin!");
            getLogger().warning("Supported NMS versions: [v1_8_R3, v1_12_R1]");
            return false;
        }
        return true;
    }

    private void registerSerializable() {
        ConfigurationSerialization.registerClass(PlayerData.class);
        ConfigurationSerialization.registerClass(BridgeArena.class);
        ConfigurationSerialization.registerClass(BridgeTeam.class);
        ConfigurationSerialization.registerClass(BoundingBox.class);
        ConfigurationSerialization.registerClass(TeamConfig.class);
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        if(getBridgeConfig().isUnlimitedArenas()) {
            for(BridgeArena arena : arenaManager.getArenas()) {
                if(arena instanceof BridgeArenaUnlimited) {
                    BridgeArenaUnlimited arenaUnlimited = (BridgeArenaUnlimited) arena;
                    if(arenaUnlimited.isClosing())
                        arenaUnlimited.forceCloseArena();
                }
            }
        }
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player p = (Player) sender;
        if(args[0].equalsIgnoreCase("stick")) {
            p.getInventory().addItem(new ItemStack(Material.STICK));
            p.sendMessage(ChatColor.GOLD + "Stick given");
        }else if(args[0].equalsIgnoreCase("save")) {
            p.sendMessage(ChatColor.GOLD + "Saving schematic. This could take a while");
            BoundingBox box = new BoundingBox(pos1, pos2);
            BSchematic.saveSchematic(args[1], p.getWorld(), box, () -> {
                p.sendMessage(ChatColor.GOLD + "Failed to save! Check console for more info");
            }, (output) -> {
                p.sendMessage(ChatColor.GOLD + "Save success!");
                p.sendMessage(ChatColor.GRAY + "Total Time (Main Thread + Async): " + (output.getTotalTime() / 1000d) + "s");
                p.sendMessage(ChatColor.GRAY + "Save file size: " + output.getMbSize() + " MB (" + output.getKbSize() + " KB)");
            });
        }else if(args[0].equalsIgnoreCase("play")) {
            arenaManager.joinRandom(p);
        }else if(args[0].equalsIgnoreCase("join")) {
            arenaManager.join(p, args[1]);
        }else if(args[0].equalsIgnoreCase("joingame")) {
            arenaManager.getArenas().get(0).joinQueue(p);
        }else if(args[0].equalsIgnoreCase("leave")) {
            arenaManager.findPlayerArena(p.getUniqueId()).leaveQueue(p);
        }else if(args[0].equalsIgnoreCase("paste")) {
            p.sendMessage(ChatColor.GOLD + "Pasting schematic. This could take a while");
            BSchematic.pasteSchematic(p.getWorld(), args[1], (output) -> {
                p.sendMessage(ChatColor.GOLD + "Pasting done.");
                p.sendMessage(ChatColor.GRAY + "Main Thread + Async Time: " + (output.getTotalTime() / 1000d) + "s");
                p.sendMessage(ChatColor.GRAY + "Main Thread Time: " + (output.getMainTime() / 1000d) + "s");
            });
        }else if(args[0].equalsIgnoreCase("tablist")) {
            p.sendMessage("Send header, {\"translate\":\"\"} for empty/remove header/footer");
            tablist = 1;
        }else if(args[0].equalsIgnoreCase("tabtest")) {
            NMSPlayerInfoPacket packet = nmsUtil.newPlayerInfoPacket(NMSPlayerInfoActionEnum.ADD_PLAYER);
            for(int i = 0; i < 20; i++) {
                NMSPlayerInfoAction action = new NMSPlayerInfoAction(UUID.randomUUID())
                    .setDisplayName("{\"text\":\"" + ChatColor.BLUE + "Player" + (i + 1) + " [R]" + "\"}")
                    .setGamemode(i % 4)
                    .setPing(i * 4 + (i * 2 - 1))
                    .setNameToAdd("\"Player" + (i + 1) + "\"");
                packet.addAction(action);
            }
            packet.createNMS().sendPacket(nmsUtil.getPlayer(p));
            p.sendMessage("Packet sent!");
        }else if(args[0].equalsIgnoreCase("convert")) {
            BSchematic.convert("b00ae781-8fec-4c8e-81f1-c4e259ea7eb1-box0.schem");
            BSchematic.convert("b00ae781-8fec-4c8e-81f1-c4e259ea7eb1-box1.schem");
            p.sendMessage("Done!");
        }else if(args[0].equalsIgnoreCase("compress")) {
            try {
                p.sendMessage("Done!");
            }catch(Exception e1) {
                e1.printStackTrace();
                p.sendMessage("error");
            }
        }else if(args[0].equalsIgnoreCase("schemtest")) {
            String schem = args[1];
            File file = new File(
                    getDataFolder(), "maps/" + schem
            );
//            BoundingBox box = new BoundingBox(pos1, pos2);
//            BSchematic.clearBox(pos1.getWorld(), box);
            p.sendMessage("Starting in 2s");
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
                p.sendMessage("Pasting...");
                try {
                    FileInputStream fis = new FileInputStream(file);
                    BufferedInputStream bis = new BufferedInputStream(fis);

                    BSchematic.pasteSchematic(p.getWorld(), schem, null);

                    bis.close();
                    fis.close();
                    p.sendMessage("Done!");
                }catch(IOException e) {
                    e.printStackTrace();
                    p.sendMessage("error");
                }
            }, 40);
        }else if(args[0].equalsIgnoreCase("benchmark")) {
            BoundingBox box = new BoundingBox(pos1, pos2);
            BSchematic.clearBox(pos1.getWorld(), box);
            p.sendMessage(ChatColor.AQUA + "Benchmark starting in 5s");
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
                benchmarkSchem(p, Integer.parseInt(args[2]), pos1.getWorld(), box, args[1], 1, new ArrayList<>());
            }, 5 * 20);
        }else
            return false;
        return true;
    }
    private int tablist = 0;
    private String header;
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        if(tablist == 1) {
            header = e.getMessage();
            e.getPlayer().sendMessage("Send footer, {\"translate\":\"\"} for empty/remove header/footer");
            tablist = 2;
        }else if(tablist == 2) {
            String footer = e.getMessage();
            NMSPlayerListHeaderFooterPacket packet = nmsUtil.newPlayerListHeaderFooterPacket(header, footer);
            packet.sendPacket(nmsUtil.getPlayer(e.getPlayer()));
            e.getPlayer().sendMessage("Packet sent!");
            tablist = 0;
        }
    }

    private void benchmarkSchem(Player p, int amount, World world, BoundingBox box, String schem, final int i, List<BSchematic.SPasteOutput> times) {
        if(i > amount) {
            p.sendMessage(ChatColor.AQUA + "Schematic Benchmark done ("+amount+")");
            p.sendMessage(ChatColor.AQUA + "========================================");
            p.sendMessage(ChatColor.AQUA + "Schematic Benchmark Result\n");
            long allMain = 0;
            long allAsync = 0;
            long allTotal = 0;
            long minMain = Integer.MAX_VALUE; long minAsync = Integer.MAX_VALUE; long minTotal = Integer.MAX_VALUE; // lowest time
            long maxMain = -1; long maxAsync = -1; long maxTotal = -1; // longest time
            for(BSchematic.SPasteOutput s : times) {
                long mainTime = s.getMainTime();
                long totalTime = s.getTotalTime();
                long asyncTime = s.getTotalTime() - s.getMainTime();
                allMain += mainTime;
                allTotal += totalTime;
                allAsync += asyncTime;

                if(mainTime < minMain) minMain = mainTime;
                if(totalTime < minTotal) minTotal = totalTime;
                if(asyncTime < minAsync) minAsync = asyncTime;

                if(mainTime > maxMain) maxMain = mainTime;
                if(totalTime > maxTotal) maxTotal = totalTime;
                if(asyncTime > maxAsync) maxAsync = asyncTime;
            }
            double size = times.size();
            double meanMain = round(allMain / size);
            double meanAsync = round(allAsync / size);
            double meanTotal = round(allTotal / size);

            p.sendMessage(ChatColor.AQUA + "Average");
            p.sendMessage(translate("&7Async &b| &7Main &b| &7Total"));
            p.sendMessage(translate(String.format(
                    "&7%sms &b| &7%sms &b| &7%sms\n", meanAsync, meanMain, meanTotal)));
            p.sendMessage(ChatColor.AQUA + "Shortest time");
            p.sendMessage(translate("&7Async &b| &7Main &b| &7Total"));
            p.sendMessage(translate(String.format(
                    "&7%sms &b| &7%sms &b| &7%sms\n", minAsync, minMain, minTotal)));
            p.sendMessage(ChatColor.AQUA + "Longest time");
            p.sendMessage(translate("&7Async &b| &7Main &b| &7Total"));
            p.sendMessage(translate(String.format(
                    "&7%sms &b| &7%sms &b| &7%sms\n", maxAsync, maxMain, maxTotal)));

            p.sendMessage(ChatColor.AQUA + "========================================");
//            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0f);
            return;
        }
        TitleTime.create("0-20-0").sendTitle(p, "", ChatColor.AQUA + "Running Benchmark " + i + "/" + amount);
//        p.sendTitle("", ChatColor.AQUA + "Running Benchmark " + i + "/"+amount, 0, 20, 0);
        BSchematic.pasteSchematic(p.getWorld(), schem, (output) -> {
            times.add(output);
            p.sendMessage(ChatColor.AQUA + "========================================");
            p.sendMessage(ChatColor.AQUA + "Benchmark " + i + "/"+amount+": Output\n");
            p.sendMessage(translate("&7Async &b| &7Main &b| &7Total"));
            p.sendMessage(translate(String.format(
                    "&7%sms &b| &7%sms &b| &7%sms\n", output.getTotalTime() - output.getMainTime(), output.getMainTime(), output.getTotalTime())));
            p.sendMessage(ChatColor.AQUA + "Current average projection:\n");
            p.sendMessage(translate("&7Async &b| &7Main &b| &7Total"));
            long allMain = 0;
            long allAsync = 0;
            long allTotal = 0;
            for(BSchematic.SPasteOutput s : times) {
                allMain += s.getMainTime();
                allTotal += s.getTotalTime();
                allAsync += (s.getTotalTime() - s.getMainTime());
            }
            double size = times.size();
            double meanMain = round(allMain / size);
            double meanAsync = round(allAsync / size);
            double meanTotal = round(allTotal / size);
            p.sendMessage(translate(String.format(
                    "&7%sms &b| &7%sms &b| &7%sms", meanAsync, meanMain, meanTotal)));
            p.sendMessage(ChatColor.AQUA + "========================================");
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
                BSchematic.clearBox(world, box);
                Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
                    benchmarkSchem(p, amount, world, box, schem, i + 1, times);
                }, 6 * 20);
            }, 3 * 20);
        });
    }
    private double round(double d) {
        return (double) Math.round(d * 100) / 100d;
    }
    private String translate(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    @EventHandler
    public void stickInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if(p.getInventory().getItemInHand() == null) return;
        if(p.getInventory().getItemInHand().getType() == Material.STICK && "BridgeWand".equalsIgnoreCase(p.getInventory().getItemInHand().getItemMeta().getDisplayName())) {
            if(e.getAction() == Action.LEFT_CLICK_BLOCK) {
                // pos1
                e.setCancelled(true);
                pos1 = e.getClickedBlock().getLocation();
                p.sendMessage(ChatColor.GOLD + "TheBridge position 1 ( ONE ) has been set!");
            }else if(e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                // pos2
                e.setCancelled(true);
                pos2 = e.getClickedBlock().getLocation();
                p.sendMessage(ChatColor.GOLD + "TheBridge position 2 ( TWO ) has been set!");
            }
        }else if(p.getInventory().getItemInHand().getType() == Material.SPONGE) {
            if(e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                e.setCancelled(true);
                int n = 0;
                for(int x = -20; x <= 20; x++) {
                    for(int y = -20; y <= 20; y++) {
                        for(int z = -20; z <= 20; z++) {
                            Location loc = new Location(p.getWorld(), p.getLocation().getBlockX() + x,
                                    p.getLocation().getBlockY() + y,
                                    p.getLocation().getBlockZ() + z);
                            Block block = p.getWorld().getBlockAt(loc);
                            if(block.getType() == Material.COMMAND) {
                                String command = ((CommandBlock) block.getState()).getCommand();
                                String filtered = command.replaceAll("[Rr][eE][gG][iI][sS][yY][oO][uU][tT][uU][bB][eE]", "Backqards")
                                        .replaceAll("[iI][Rr][eE][gG][iI][sS]", "Backqards")
                                        .replace("bramar", "bramar2")
                                        .replaceAll("[bB][rR][aA][mM][aA][Rr][2]*", "bramar2");
                                if(!command.equalsIgnoreCase(filtered)) {
                                    CommandBlock state = ((CommandBlock) block.getState());
                                    state.setCommand(filtered);
                                    state.update();
                                    n++;
                                }
                            }
                        }
                    }
                }
                p.sendMessage("Filtered " + n + " command blocks!");
            }
        }
    }

    public static long getCurrentTick() {
        return instance.currentTick;
    }

    public NMSUtil getNMSUtil() {
        return nmsUtil;
    }

    public String getNMSVersion() {
        return nmsVersion;
    }

    public Random getRandom() {
        return r;
    }
}
