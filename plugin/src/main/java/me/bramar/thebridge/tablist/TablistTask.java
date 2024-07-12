package me.bramar.thebridge.tablist;

import io.netty.channel.Channel;
import me.bramar.thebridge.TheBridge;
import me.bramar.thebridge.placeholders.BridgePlaceholders;
import me.bramar.thebridge.tinyprotocol.TinyProtocol;
import me.bramar.thebridge.util.nms.NMSEntityPlayer;
import me.bramar.thebridge.util.nms.NMSUtil;
import me.bramar.thebridge.util.nms.packet.NMSPlayerInfoAction;
import me.bramar.thebridge.util.nms.packet.NMSPlayerInfoActionEnum;
import me.bramar.thebridge.util.nms.packet.NMSPlayerInfoPacket;
import me.bramar.thebridge.util.nms.packet.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

public class TablistTask implements Listener {
    private final UUID uuid;
    private final TablistManager manager;
    private final BridgePlaceholders placeholders;
    private final NMSUtil nms;
    private final List<UUID> players;
    private final Map<UUID, PlayerInfoData> playerData;
    private TinyProtocol tinyProtocol;
    private final Set<Object> sentPackets = new HashSet<>();
    private final Map<UUID, Integer> fakePingMap;
    private final Random random = TheBridge.getInstance().getRandom();

    public TablistTask(TablistManager manager, UUID uuid, List<UUID> tablistPlayers, Map<UUID, PlayerInfoData> playerData) {
        this.manager = manager;
        this.uuid = uuid;
        this.players = tablistPlayers;
        this.playerData = playerData;
        this.nms = manager.getPlugin().getNMSUtil();
        this.placeholders = manager.getPlugin().getPlaceholders();
        this.fakePingMap = manager.getFakePingMode() == 0 ? null : new HashMap<>();
    }

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, manager.getPlugin());
        tinyProtocol = new TinyProtocol(manager.getPlugin()) {
            @Override
            public Object onPacketOutAsync(Player receiver, Channel channel, Object packet) {
                if(receiver != null && receiver.getUniqueId().equals(uuid)) {
                    if(nms.isPacketPlayOutPlayerInfo(packet)) {
                        if(sentPackets.contains(packet)) {
                            sentPackets.remove(packet);
                            return packet;
                        }else {
                            NMSPlayerInfoPacket nmsPacket = nms.newPlayerInfoPacket(packet);
                            if(nmsPacket.getAction() == NMSPlayerInfoActionEnum.UPDATE_GAME_MODE) {
                                Set<NMSPlayerInfoAction> toRemove = new HashSet<>();
                                for(NMSPlayerInfoAction action : nmsPacket.getActionList()) {
                                    if(!players.contains(action.getUUID())) {
                                        toRemove.add(action);
                                    }
                                }
                                nmsPacket.getActionList().removeAll(toRemove);
                                if(nmsPacket.getActionList().size() == 0)
                                    return null;
                                return nmsPacket.createNMS().getOriginal();
                            }else if(nmsPacket.getAction() == NMSPlayerInfoActionEnum.UPDATE_LATENCY) {
                                Set<NMSPlayerInfoAction> toRemove = new HashSet<>();
                                for(NMSPlayerInfoAction action : nmsPacket.getActionList()) {
                                    if(!players.contains(action.getUUID())) {
                                        toRemove.add(action);
                                    }else {
                                        // fake ping stuff
                                        if(manager.isFakePing()) {
                                            if(manager.getFakePingMode() == 0) {
                                                action.setPing(manager.getFakePingValue()[0]);
                                            }else {
                                                int[] v = manager.getFakePingValue();
                                                int ping = random.nextInt(Math.abs(v[1] - v[0])) + Math.min(v[0], v[1]);
                                                fakePingMap.put(action.getUUID(), ping);
                                                action.setPing(ping);
                                            }
                                        }
                                    }
                                }
                                nmsPacket.getActionList().removeAll(toRemove);
                                if(nmsPacket.getActionList().size() == 0)
                                    return null;
                                return nmsPacket.createNMS().getOriginal();
                            }else return null;
                        }
                    }
                }
                return super.onPacketOutAsync(receiver, channel, packet);
            }
        };
        Player p = Bukkit.getPlayer(uuid);
        if(p != null)
            tinyProtocol.injectPlayer(p);
    }
    public void unregister() {
        HandlerList.unregisterAll(this);
        tinyProtocol.close();
    }

    public int ping(UUID uuid, int realPing) {
        if(manager.isFakePing()) {
            if(manager.getFakePingMode() == 0) {
                return manager.getFakePingValue()[0];
            }else {
                if(fakePingMap.containsKey(uuid))
                    return fakePingMap.get(uuid);
                int[] v = manager.getFakePingValue();
                int ping = random.nextInt(Math.abs(v[1] - v[0])) + Math.min(v[0], v[1]);
                fakePingMap.put(uuid, ping);
                return ping;
            }
        }else return realPing;
    }

    public String getDisplayName(OfflinePlayer op) {
        String str = manager.getNameFormat()
                .replace("{player}", op.getName());
        if(op instanceof Player) {
            Player p = (Player) op;
            str = str.replace("{health}", "" + (int) Math.round(p.getHealth()))
                .replace("{ping}", "" + ping(p.getUniqueId(), nms.getPlayer(p).getPing()));
        }else {
            str = str.replace("{health}", "0")
                    .replace("{ping}", "0");
        }
        return ChatColor.translateAlternateColorCodes('&', placeholders.applyPlaceholders(op, str));
    }
    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        Player sp = event.getPlayer();
        if(!players.contains(sp.getUniqueId()))
            return;
        if(sp.getUniqueId().equals(uuid)) {
            tinyProtocol.uninjectPlayer(sp);
            return;
        }

        Player p = Bukkit.getPlayer(uuid);
        if(p == null)
            return;
        NMSEntityPlayer nmsPlayer = nms.getPlayer(p);
        if(manager.isShowDisconnectedAsSpectator()) {
            NMSPlayerInfoPacket packet = nms.newPlayerInfoPacket(NMSPlayerInfoActionEnum.UPDATE_GAME_MODE);
            packet.addAction(
                    new NMSPlayerInfoAction(sp.getUniqueId()).setGamemode(GameMode.SPECTATOR)
            );
            sentPackets.add(packet.createNMS().getOriginal());
            packet.sendPacket(nmsPlayer);
        }else {
            NMSPlayerInfoPacket packet = nms.newPlayerInfoPacket(NMSPlayerInfoActionEnum.REMOVE_PLAYER);
            packet.addAction(
                    new NMSPlayerInfoAction(sp.getUniqueId())
            );
            sentPackets.add(packet.createNMS().getOriginal());
            packet.sendPacket(nmsPlayer);
        }
    }
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player sp = event.getPlayer();
        if(!players.contains(sp.getUniqueId()))
            return;
        if(sp.getUniqueId().equals(uuid)) {
            tinyProtocol.injectPlayer(sp);
            initPackets();
        }else {
            Player p = Bukkit.getPlayer(uuid);
            if(p == null)
                return;

            NMSEntityPlayer nmsPlayer = nms.getPlayer(p);
            if(manager.isShowDisconnectedAsSpectator()) {
                NMSPlayerInfoPacket packet = nms.newPlayerInfoPacket(NMSPlayerInfoActionEnum.UPDATE_GAME_MODE);
                packet.addAction(
                        new NMSPlayerInfoAction(sp.getUniqueId()).setGamemode(sp.getGameMode())
                );
                sentPackets.add(packet.createNMS().getOriginal());
                packet.sendPacket(nmsPlayer);
            }else {
                NMSPlayerInfoPacket packet = nms.newPlayerInfoPacket(NMSPlayerInfoActionEnum.ADD_PLAYER);
                NMSEntityPlayer nmsSP = nms.getPlayer(sp);
                Collection<ProfileProperty> profileProperties = nmsSP.getProfileProperties();
                if(manager.isHideSkin()) {
                    profileProperties = new ArrayList<>(profileProperties);
                    profileProperties.removeIf(property -> property.getName().equalsIgnoreCase("textures"));
                }
                packet.addAction(
                        new NMSPlayerInfoAction(sp.getUniqueId())
                                .setNameToAdd(sp.getName())
                                .setPropertiesToAdd(profileProperties)
                                .setGamemode(sp.getGameMode())
                                .setPing(ping(sp.getUniqueId(), nmsSP.getPing()))
                                .setDisplayName(getDisplayName(sp))
                );
                sentPackets.add(packet.createNMS().getOriginal());
                packet.sendPacket(nmsPlayer);
            }
        }
    }

    public void initPackets() {
        Player p = Bukkit.getPlayer(uuid);
        if(p != null) {
            NMSEntityPlayer nmsPlayer = nms.getPlayer(p);
            // remove all players (including in-game because they will be re-added)
            NMSPlayerInfoPacket removePacket = nms.newPlayerInfoPacket(NMSPlayerInfoActionEnum.REMOVE_PLAYER);
            Bukkit.getOnlinePlayers().forEach(sp -> removePacket.addAction(
                            new NMSPlayerInfoAction(sp.getUniqueId())));
            sentPackets.add(removePacket.createNMS().getOriginal());
            removePacket.sendPacket(nmsPlayer);
            NMSPlayerInfoPacket addPacket = nms.newPlayerInfoPacket(NMSPlayerInfoActionEnum.ADD_PLAYER);
            for(UUID uuid : players) {
                Player sp = Bukkit.getPlayer(uuid);
                NMSPlayerInfoAction addAction = new NMSPlayerInfoAction(uuid);
                if(sp != null) {
                    String displayName = getDisplayName(sp);
                    NMSEntityPlayer nmsSP = nms.getPlayer(sp);
                    Collection<ProfileProperty> profileProperties = nmsSP.getProfileProperties();
                    if(manager.isHideSkin()) {
                        profileProperties = new ArrayList<>(profileProperties);
                        profileProperties.removeIf(property -> property.getName().equalsIgnoreCase("textures"));
                    }
                    addAction.setNameToAdd(sp.getName())
                            .setPropertiesToAdd(profileProperties)
                            .setGamemode(sp.getGameMode())
                            .setPing(ping(sp.getUniqueId(), nmsSP.getPing()))
                            .setDisplayName(displayName);
                }else if(manager.isShowDisconnectedAsSpectator()) {
                    String displayName = getDisplayName(Bukkit.getOfflinePlayer(uuid));
                    PlayerInfoData playerInfoData = playerData.get(uuid);
                    Collection<ProfileProperty> profileProperties = playerInfoData.getProfileProperties();
                    if(manager.isHideSkin()) {
                        profileProperties = new ArrayList<>(profileProperties);
                        profileProperties.removeIf(property -> property.getName().equalsIgnoreCase("textures"));
                    }
                    addAction.setNameToAdd(playerInfoData.getName())
                            .setPropertiesToAdd(profileProperties)
                            .setGamemode(GameMode.SPECTATOR)
                            .setPing(0)
                            .setDisplayName(displayName);
                }
                addPacket.addAction(addAction);
            }
            sentPackets.add(addPacket.createNMS().getOriginal());
            addPacket.sendPacket(nmsPlayer);
        }
    }
    public void resetTablist() {
        Player p = Bukkit.getPlayer(uuid);
        if(p != null) {
            NMSEntityPlayer nmsPlayer = nms.getPlayer(p);
            // remove all players (including in-game because they will be re-added)
            NMSPlayerInfoPacket removePacket = nms.newPlayerInfoPacket(NMSPlayerInfoActionEnum.REMOVE_PLAYER);
            Bukkit.getOnlinePlayers().forEach(sp -> removePacket.addAction(
                    new NMSPlayerInfoAction(sp.getUniqueId())));
            sentPackets.add(removePacket.createNMS().getOriginal());
            removePacket.sendPacket(nmsPlayer);
            NMSPlayerInfoPacket addPacket = nms.newPlayerInfoPacket(NMSPlayerInfoActionEnum.ADD_PLAYER);
            for(Player sp : Bukkit.getOnlinePlayers()) {
                NMSPlayerInfoAction addAction = new NMSPlayerInfoAction(sp.getUniqueId());
                NMSEntityPlayer nmsSP = nms.getPlayer(sp);
                Collection<ProfileProperty> profileProperties = nmsSP.getProfileProperties();
                if(manager.isHideSkin()) {
                    profileProperties = new ArrayList<>(profileProperties);
                    profileProperties.removeIf(property -> property.getName().equalsIgnoreCase("textures"));
                }
                addAction.setNameToAdd(sp.getName())
                        .setPropertiesToAdd(profileProperties)
                        .setGamemode(sp.getGameMode())
                        .setPing(nmsSP.getPing())
                        .setDisplayName(sp.getDisplayName());
                addPacket.addAction(addAction);
            }
            sentPackets.add(addPacket.createNMS().getOriginal());
            addPacket.sendPacket(nmsPlayer);
        }
    }
}
