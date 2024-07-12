package me.bramar.thebridge.model;

import lombok.Getter;
import lombok.Setter;
import me.bramar.thebridge.TheBridge;
import me.bramar.thebridge.arena.BridgeArena;
import me.bramar.thebridge.arena.BridgeTeam;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static me.bramar.thebridge.model.DeathCause.*;

@Getter
@Setter
public class CombatTag {
//    private static final int TICK_EXPIRY = 15 * 20; // Combat tag disappears in 10 seconds (PLAYER)
//    private static final int CAUSE_EXPIRY = 30 * 20; // time it takes before a cause is deemed "invalid" like VOID but expired, will become OTHER

    private static long getTickExpiry() {
        return TheBridge.getInstance().getBridgeConfig().getCombatTagDamagerExpiry();
    }
    private static long getCauseExpiry() {
        return TheBridge.getInstance().getBridgeConfig().getCombatTagCauseExpiry();
    }

    private @Nullable UUID damager;
    private UUID damaged;
    private DeathCause cause;
    private @Nullable EntityDamageEvent.DamageCause internalDamageCause;
    private long tick;
    private long causeTick=0;
    private CombatTag(UUID damaged) { this.damaged = damaged; }

    public static CombatTag ofEmpty(UUID damaged) {
        return new CombatTag(damaged);
    }

    public void setDamager(UUID damager) {
        if(damager != null) {
            this.damager = damager;
            this.tick = TheBridge.getCurrentTick();
        }
    }

    public UUID getDamager() {
        return isActive() ? damager : null;
    }

    public void voidify() {
        if(cause == SHOT)
            cause = DeathCause.SHOT_VOID;
        else
            cause = DeathCause.VOID;
    }
    public void setCombatCause(EntityDamageEvent.DamageCause cause) {
        if(cause == EntityDamageEvent.DamageCause.VOID) {
            voidify();
        }else if(cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK) { // melee
            setCause(DeathCause.MELEE);
        }else if(cause == EntityDamageEvent.DamageCause.PROJECTILE) {
            setCause(DeathCause.SHOT);
        }else if(cause == EntityDamageEvent.DamageCause.THORNS) {
            setCause(DeathCause.THORNS);
        }else {
            setCause(DeathCause.OTHER);
            setInternalDamageCause(cause);
        }
        causeTick = TheBridge.getCurrentTick();
    }
    public boolean isActive() {
        return (TheBridge.getCurrentTick() - tick) <= getTickExpiry();
    }

    public @Nullable UUID getActiveDamager() {
        return isActive() ? damager : null;
    }

    public String getDeathMessage(BridgeArena arena) {
        BridgeTeam damagedTeam = arena.getTeam(damaged);
        BridgeTeam damagerTeam = damager == null ? null : arena.getTeam(damager);
        boolean sameTeam = damagedTeam == damagerTeam;
        String damagedName = damagedTeam.getColor() + Bukkit.getPlayer(damaged).getName();
        String damagerName = damager == null ? null : damagerTeam.getColor() + Bukkit.getPlayer(damager).getName();
        DeathCause cause0 = cause;
        boolean isShotVoid = cause == SHOT_VOID;
        if(cause == MELEE || cause == SHOT || cause == SHOT_VOID) {
            internalDamageCause = null;
            if((sameTeam || damagerTeam == null) || !isActive())
                cause0 = DeathCause.OTHER;
            // make sure that there is a killer, else do OTHER type
        }
        if(TheBridge.getCurrentTick() - causeTick >= getCauseExpiry()) {
            cause = OTHER;
            internalDamageCause = null;
        }
        switch(cause0) {
            case MELEE:
                return damagedName + ChatColor.GRAY + " was killed by " + damagerName + ChatColor.GRAY + ".";
            case VOID:
                if(damagerTeam != null && isActive())
                    return damagedName + ChatColor.GRAY + " was knocked into the void by " + damagerName + ChatColor.GRAY + ".";
                return damagedName + ChatColor.GRAY + " fell into the void.";
            case SHOT:
                return damagedName + ChatColor.GRAY + " was shot by " + damagerName + ChatColor.GRAY + ".";
            case SHOT_VOID:
                return damagedName + ChatColor.GRAY + " was shot into the void by " + damagerName + ChatColor.GRAY + ".";
            case THORNS:
                return damagedName + ChatColor.GRAY + " died to thorns by " + damagerName + ChatColor.GRAY + ".";
            case OTHER:
                // special case
                if(isShotVoid)
                    return damagedName + ChatColor.GRAY + " fell into the void.";

                // unknown death (like drown, lava) OR no combat tag
                if(internalDamageCause == null)
                    return damagedName + ChatColor.GRAY + " died.";
                else
                    return damagedName + ChatColor.GRAY + " died to " + internalDamageCause + ".";
        }
        return damagedName + ChatColor.GRAY + " unknowingly died a funny cause.";
    }
}
