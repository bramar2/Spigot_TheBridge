package me.bramar.thebridge.util.nms.v1_8_R3;

import io.netty.buffer.Unpooled;
import me.bramar.thebridge.util.nms.NMSEntityPlayer;
import me.bramar.thebridge.util.nms.packet.NMSPlayerInfoAction;
import me.bramar.thebridge.util.nms.packet.NMSPlayerInfoActionEnum;
import me.bramar.thebridge.util.nms.packet.NMSPlayerInfoPacket;
import me.bramar.thebridge.util.nms.packet.ProfileProperty;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketDataSerializer;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerInfo;

import java.io.IOException;
import java.util.*;

public class PlayerInfoPacket implements NMSPlayerInfoPacket {

    private final NMSPlayerInfoActionEnum actionEnum;
    private final List<NMSPlayerInfoAction> actionList = new ArrayList<>();
    private PacketPlayOutPlayerInfo packet = null;

    public PlayerInfoPacket(NMSPlayerInfoActionEnum actionEnum) {
        this.actionEnum = actionEnum;
    }

    @Override
    public NMSPlayerInfoActionEnum getAction() {
        return actionEnum;
    }

    @Override
    public Collection<NMSPlayerInfoAction> getActionList() {
        return actionList;
    }

    @Override
    public void addAction(NMSPlayerInfoAction action) {
        actionList.add(action);
    }

    private void serialize(PacketDataSerializer var1) throws IOException {
        var1.a(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.valueOf(actionEnum.toString().toUpperCase()));
        var1.b(actionList.size());

        for(NMSPlayerInfoAction action : actionList) {
            switch(actionEnum) {
                case ADD_PLAYER:
                    var1.a(action.getUUID());
                    var1.a(action.getNameToAdd());
                    if(action.getPropertiesToAdd() == null)
                        action.setPropertiesToAdd(Collections.emptyList());
                    var1.b(action.getPropertiesToAdd().size());
                    for(ProfileProperty property : action.getPropertiesToAdd()) {
                        var1.a(property.getName());
                        var1.a(property.getValue());
                        if(property.hasSignature()) {
                            var1.writeBoolean(true);
                            var1.a(property.getSignature());
                        }else {
                            var1.writeBoolean(false);
                        }
                    }

                    var1.b(action.getGamemode());
                    var1.b(action.getPing());
                    if(action.getDisplayName() == null) {
                        var1.writeBoolean(false);
                    }else {
                        var1.writeBoolean(true);
                        var1.a(action.getDisplayName());
                    }
                    break;
                case UPDATE_GAME_MODE:
                    var1.a(action.getUUID());
                    var1.b(action.getGamemode());
                    break;
                case UPDATE_LATENCY:
                    var1.a(action.getUUID());
                    var1.b(action.getPing());
                    break;
                case UPDATE_DISPLAY_NAME:
                    var1.a(action.getUUID());
                    if(action.getDisplayName() == null) {
                        var1.writeBoolean(false);
                    }else {
                        var1.writeBoolean(true);
                        var1.a(action.getDisplayName());
                    }
                    break;
                case REMOVE_PLAYER:
                    var1.a(action.getUUID());
            }
        }
    }

    @Override
    public NMSPlayerInfoPacket createNMS() {
        PacketDataSerializer packetData = new PacketDataSerializer(Unpooled.buffer());
        try {
            packet = new PacketPlayOutPlayerInfo();
            serialize(packetData);
            packet.a(packetData);
        }catch(IOException e1) {
            e1.printStackTrace();
            throw new RuntimeException(e1);
        }
        return this;
    }

    @Override
    public void sendPacket(NMSEntityPlayer p) {
        if(packet == null)
            throw new IllegalStateException("Packet has not been created using createNMS() yet");
        ((EntityPlayer)p.getOriginal()).playerConnection.sendPacket(packet);
    }

    @Override
    public Object getOriginal() {
        return packet;
    }
    public static PlayerInfoPacket of(Object nmsPacket) throws IOException {
        if(!(nmsPacket instanceof PacketPlayOutPlayerInfo))
            throw new IllegalArgumentException("packet is not of PacketPlayOutPlayerInfo");

        PacketPlayOutPlayerInfo packet = (PacketPlayOutPlayerInfo) nmsPacket;

        // Serializes NMS Packet -> Packet Data Serializer
        PacketDataSerializer var1 = new PacketDataSerializer(Unpooled.buffer());
        packet.b(var1);


        NMSPlayerInfoActionEnum action = NMSPlayerInfoActionEnum.valueOf(var1.a(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.class).name().toUpperCase());
        int actionCount = var1.e();
        PlayerInfoPacket p = new PlayerInfoPacket(action);
        for(int var3 = 0; var3 < actionCount; var3++) {
            int gamemode, ping;
            String displayName = null;
            UUID uuid = var1.g();
            switch(action) {
                case ADD_PLAYER:
                    String name = var1.c(16);
                    int propertiesCount = var1.e();
                    List<ProfileProperty> profileProperties = new ArrayList<>();
                    for(int i = 0; i < propertiesCount; i++) {
                        String propertyName = var1.c(32767);
                        String propertyValue = var1.c(32767);
                        if (var1.readBoolean()) { // hasSignature
                            String signature = var1.c(32767);
                            profileProperties.add(new ProfileProperty(propertyName, propertyValue, signature));
                        } else {
                            profileProperties.add(new ProfileProperty(propertyName, propertyValue, null));
                        }
                    }

                    gamemode = var1.e();
                    ping = var1.e();
                    if (var1.readBoolean()) {
                        displayName = IChatBaseComponent.ChatSerializer.a(var1.d());
                    }
                    p.addAction(new NMSPlayerInfoAction(uuid)
                            .setNameToAdd(name)
                            .setPropertiesToAdd(profileProperties)
                            .setDisplayName(displayName)
                            .setGamemode(gamemode)
                            .setPing(ping));
                    break;
                case UPDATE_GAME_MODE:
                    gamemode = var1.e();
                    p.addAction(new NMSPlayerInfoAction(uuid).setGamemode(gamemode));
                    break;
                case UPDATE_LATENCY:
                    ping = var1.e();
                    p.addAction(new NMSPlayerInfoAction(uuid).setPing(ping));
                    break;
                case UPDATE_DISPLAY_NAME:
                    if(var1.readBoolean()) {
                        displayName = IChatBaseComponent.ChatSerializer.a(var1.d());
                    }
                    p.addAction(new NMSPlayerInfoAction(uuid).setDisplayName(displayName));
                    break;
                case REMOVE_PLAYER:
                    p.addAction(new NMSPlayerInfoAction(uuid));
            }

        }
        p.packet = packet;
        return p;
    }
}
