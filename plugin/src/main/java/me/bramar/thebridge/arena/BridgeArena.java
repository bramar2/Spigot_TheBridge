package me.bramar.thebridge.arena;

import lombok.Getter;
import lombok.Setter;
import me.bramar.thebridge.TheBridge;
import me.bramar.thebridge.arena.blueprint.BridgeArenaUnlimited;
import me.bramar.thebridge.config.BridgeConfig;
import me.bramar.thebridge.config.PlayerData;
import me.bramar.thebridge.config.PlayerDatabase;
import me.bramar.thebridge.model.BoundingBox;
import me.bramar.thebridge.model.CombatTag;
import me.bramar.thebridge.model.GameState;
import me.bramar.thebridge.model.config.BlockType;
import me.bramar.thebridge.model.config.SoundType;
import me.bramar.thebridge.model.config.TitleTime;
import me.bramar.thebridge.util.schematic.BSchematic;
import me.bramar.thebridge.util.CompatibilityUtils;
import me.bramar.thebridge.util.ModuleLogger;
import me.bramar.thebridge.util.nms.NMSUtil;
import org.apache.logging.log4j.core.pattern.UUIDPatternConverter;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.*;
import java.util.logging.Logger;

import static me.bramar.thebridge.util.CompatibilityUtils.setPickupStatus;
import static me.bramar.thebridge.util.CompatibilityUtils.setStorageContents;

@Getter
public class BridgeArena implements ConfigurationSerializable, Listener {
    public static final int VERSION = 1;
    private final Map<UUID, CombatTag> combatTag = new HashMap<>();
    private World world;
    private String worldName;
    private BoundingBox placeBox;
    private BoundingBox arenaBox;
    private int voidY = -1;
    private int placeMinY = -1;
    private int placeMaxY = -1; // -1 for unspecified
    private List<BoundingBox> disallowPlace; // list of boundingboxes that disallow placing blocks
    private List<BoundingBox> allowBreak; // list of boundingboxes that allow break blocks if its not clay/block in config
    private List<BoundingBox> disallowBreak; // disallow breaking blocks that may be broken if its clay/block in config
    private BridgeTeam team1, team2;
    private GameState gameState = GameState.IDLE;
    private String mapName;
    private String arenaName;
    // queue
    private Location joinLocation;
    private int neededPlayers = -1;
    private int maxPlayers = -1;
    private int countdown; // in seconds
    private BukkitRunnable countdownRunnable;
    private final List<UUID> queue = new ArrayList<>(); // queue | all players (in game)
    private final List<UUID> disconnected = new ArrayList<>();
    private final Map<UUID, BukkitRunnable> arrowCooldown = new HashMap<>();
    private final List<UUID> firstGapple = new ArrayList<>(); // list of players that already ate first gapple
    private final List<Location> placedBlocks = new ArrayList<>();
    private final List<Listener> listeners = new ArrayList<>();
    private UUID uniqueId;
    private long tickStarted;
    private long gameTime; // This is time ELAPSED, not time LEFT
    private int timeLimitTaskId = -1;

    private final TheBridge plugin = TheBridge.getInstance();
    private final BridgeConfig config = plugin.getBridgeConfig();
    private final PlayerDatabase playerDatabase = plugin.getPlayerDatabase();
    private final NMSUtil nms = plugin.getNMSUtil();
    private Logger logger;
    @Setter private File file;

    protected BridgeArena(World world, String worldName, BoundingBox arenaBox, BoundingBox placeBox, int voidY, int placeMinY, int placeMaxY, List<BoundingBox> disallowPlace, List<BoundingBox> allowBreak, List<BoundingBox> disallowBreak, BridgeTeam team1, BridgeTeam team2, String mapName, String arenaName, Location joinLocation, int neededPlayers, int maxPlayers, UUID uniqueId, File file) {
        this.world = world;
        this.worldName = worldName;
        this.arenaBox = arenaBox;
        this.placeBox = placeBox;
        this.voidY = voidY;
        this.placeMinY = placeMinY;
        this.placeMaxY = placeMaxY;
        this.disallowPlace = disallowPlace;
        this.allowBreak = allowBreak;
        this.disallowBreak = disallowBreak;
        this.team1 = team1;
        this.team2 = team2;
        this.mapName = mapName;
        this.arenaName = arenaName;
        this.joinLocation = joinLocation;
        this.neededPlayers = neededPlayers;
        this.maxPlayers = maxPlayers;
        this.uniqueId = uniqueId;
        this.file = file;
    }


    private BridgeArena() {
        disallowPlace = new ArrayList<>();
        disallowBreak = new ArrayList<>();
        allowBreak = new ArrayList<>();
        mapName = arenaName = null;
        joinLocation = null;
        world = null;
        worldName = null;
        arenaBox = null;
        placeBox = null;
    }

    public BridgeArena(UUID uniqueId) {
        this();
        this.uniqueId = uniqueId;
        // set up an empty arena
        mapName = arenaName = null;
        joinLocation = null;
        world = null;
        worldName = null;
        arenaBox = null;
        team1 = new BridgeTeam(uniqueId, 0);
        team2 = new BridgeTeam(uniqueId, 1);
    }

    public void loadEvents(TheBridge plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        if(CompatibilityUtils.arrowPickupAvailable()) {
            Listener listener = new Listener() {
                @EventHandler
                public void onPickupArrow0(PlayerPickupArrowEvent e) {
                    e.setCancelled(BridgeArena.this.onPickupArrow(e.getPlayer().getUniqueId()));
                }
            };
            listeners.add(listener);
            Bukkit.getPluginManager().registerEvents(listener, plugin);
        }
        if(CompatibilityUtils.entityItemPickupAvailable()) {
            Listener listener = new Listener() {
                @EventHandler
                public void onItemPickup0(EntityPickupItemEvent e) {
                    if(!(e.getEntity() instanceof Player))
                        return;
                    e.setCancelled(BridgeArena.this.onPickup(e.getEntity().getUniqueId()));
                }
            };
            listeners.add(listener);
            Bukkit.getPluginManager().registerEvents(listener, plugin);
        }else {
            Listener listener = new Listener() {
                @EventHandler
                public void onItemPickup0(PlayerPickupItemEvent e) {
                    e.setCancelled(BridgeArena.this.onPickup(e.getPlayer().getUniqueId()));
                }
            };
            listeners.add(listener);
            Bukkit.getPluginManager().registerEvents(listener, plugin);
        }
    }


    public boolean availableForJoin() {
        return gameState == GameState.IDLE && queue.size() < maxPlayers;
    }
    public boolean isReady() {
        // is the arena set up?

        neededPlayers = team1.getMinPlayers() + team2.getMinPlayers();
        maxPlayers = team1.getMaxPlayers() + team2.getMaxPlayers();

        if(uniqueId == null || mapName == null || arenaName == null ||
        joinLocation == null || worldName == null ||
        arenaBox == null)
            return false;

        if(logger == null)
            logger = new ModuleLogger("Arena/" + arenaName);

        if(world == null && (world = Bukkit.getWorld(worldName)) == null)
            return false;

        if(!(uniqueId.equals(team1.getArenaUniqueId()) && uniqueId.equals(team2.getArenaUniqueId()))) {
            team1.setArenaUniqueId(uniqueId);
            team2.setArenaUniqueId(uniqueId);
            logger.warning("Team and arena UUIDs are different. Automatically changed them.");
        }

        if(voidY != -1 && !arenaBox.inRangeY(voidY)) {
            logger.warning("The box does not fit the Y for void. Void deaths will not work and glitch out!");
        }

        if(!team1.isReady() || !team2.isReady())
            return false;

        if(placeMinY == -1 || placeMaxY == -1 || voidY == -1)
            return false;

        return true;
    }
    public boolean isAvailable() {
        return gameState == GameState.IDLE;
    }

    public BridgeTeam getTeam(UUID uuid) {
        if(uuid == null)
            return null;
        for(UUID u1 : team1.getPlayers()) {
            if(u1.equals(uuid))
                return team1;
        }
        for(UUID u2 : team2.getPlayers()) {
            if(u2.equals(uuid))
                return team2;
        }
        return null;
    }
    public BridgeTeam otherTeam(BridgeTeam team) {
        return team == team1 ? team2 : team1;
    }

//    @EventHandler This is only supported in 1.9+
//    public void onPickupArrow(PlayerPickupArrowEvent e) {
//        UUID uuid = e.getPlayer().getUniqueId();
//        if(getTeam(uuid) != null)
//            e.setCancelled(true);
//    }

    public boolean onPickupArrow(UUID uuid) { // returns isCancelled()
        if(getTeam(uuid) != null)
            return true;
        return false;
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if(getTeam(e.getPlayer().getUniqueId()) != null &&
        config.isNoDrop())
            e.setCancelled(true);
    }
    @EventHandler
    public void onInvClick(InventoryClickEvent e) {
        UUID uuid = e.getWhoClicked().getUniqueId();
        if(getTeam(uuid) == null)
            return;

        if(e.getSlotType() == InventoryType.SlotType.ARMOR &&
        config.isNoSlotChangeArmor()) {
            e.setCancelled(true);
            return;
        }

        if(config.isNoSlotChangeAll()) {
            e.setCancelled(true);
            return;
        }

        if(config.isNoSlotChangeArrow()) {
            ItemStack item;
            if(e.getClickedInventory() instanceof PlayerInventory &&
                    (item = e.getClickedInventory().getItem(e.getSlot())) != null && item.getType() == Material.ARROW)
                e.setCancelled(true);
        }
    }
    public boolean onPickup(UUID uuid) { // returns isCancelled()
        if(getTeam(uuid) != null && config.isNoPickup())
            return true;
        return false;
    }
    @EventHandler
    public void onEntityDamage(EntityDamageEvent e) {
        DamageCause cause = e.getCause();
        if(e.getEntity() instanceof Player) {
            Player damaged = (Player) e.getEntity();
            UUID damagedUUID = damaged.getUniqueId();
            BridgeTeam damagedTeam = getTeam(damagedUUID);
            if(damagedTeam == null) return;
            if(cause == DamageCause.FALL) {
                e.setCancelled(true); return;
            }
            if(gameState == GameState.IN_BOX) {
                e.setCancelled(true);
                return;
            }

            UUID damager = null;
            Entity realDamager = null;
            if(e instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent be = (EntityDamageByEntityEvent) e;
                realDamager = be.getDamager();
                if(realDamager instanceof Projectile) {
                    ProjectileSource shooter = ((Projectile) realDamager).getShooter();
                    if(shooter instanceof Player) {
                        damager = ((Player) shooter).getUniqueId();
                    }else {
                        e.setCancelled(true);
                        return;
                    }
                }else if(realDamager instanceof Player) {
                    damager = realDamager.getUniqueId();
                }else {
                    e.setCancelled(true);
                    return; // by other mobs
                }
            }
            if(!combatTag.containsKey(damagedUUID))
                combatTag.put(damagedUUID, CombatTag.ofEmpty(damagedUUID));

            if(!config.isFriendlyFire()) {
                if(damager != null && getTeam(damager) == damagedTeam && !damaged.getUniqueId().equals(damager)) {
                    // If there is a damager, both of them are in the same team and both are different players
                    if(realDamager instanceof Projectile)
                        realDamager.remove();
                    e.setCancelled(true);
                    return;
                }
            }
            if(!config.isBowBoosting()) {
                // if bow boosting disabled, and it is the same player
                if(damager != null && damager.equals(damaged.getUniqueId())) {
                    if(realDamager instanceof Projectile)
                        realDamager.remove();
                    e.setCancelled(true);
                    return;
                }
            }

            CombatTag tag = combatTag.get(damagedUUID);
            tag.setDamager(damager);
            tag.setCombatCause(cause);
            // ^^ set combat tag
            // after this, check if this damage makes them dead

            // death check
            if(!e.isCancelled() && (damaged.getHealth() - e.getFinalDamage()) <= 0.0d) {
//                e.setCancelled(true);
                e.setDamage(0.0d); // dont show death screen, but still inflict damage so arrows don't "deflect"
                onPlayerDie(damaged, damagedTeam, tag);
                e.getEntity().setVelocity(new Vector(0, 0, 0));
            }
        }
    }
    public Player getPlayer(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        return disconnected.contains(uuid) ? null : p;
    }
    public void onPlayerDie(Player player, BridgeTeam team, CombatTag tag) {
        broadcastMessage(tag.getDeathMessage(this));

        UUID damagerUUID = tag.getActiveDamager();
        if(damagerUUID != null) {
            Player damager = getPlayer(damagerUUID);
            if(damager != null)
                config.getKillSound().playSound(damager);
        }

        tag.setTick(0); // invalidate damager tag (after "death")
        tag.setDamager(null);
        // to avoid knockback death
        player.setVelocity(new Vector(0,0,0));
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> player.teleport(team.getRespawnLoc()), 1);
        onPlayerDieEffect(player, team);
    }
    public void onPlayerDieEffect(Player player, BridgeTeam team) {
        player.setHealth(player.getMaxHealth());
        PlayerData playerData = playerDatabase.getPlayerData(player.getUniqueId());

        setStorageContents(player, playerData.formatInventory(team.getTeamNumber()));
//        player.getInventory().setArmorContents(
//                config.getTeamSettings(team.getTeam()).getArmor()
//        );
        config.getTeamSettings(team.getTeamNumber()).giveArmorToPlayer(player);

        for(PotionEffect effect : player.getActivePotionEffects())
            player.removePotionEffect(effect.getType());

        if(arrowCooldown.containsKey(player.getUniqueId())) {
            arrowCooldown.get(player.getUniqueId()).cancel();
            arrowCooldown.remove(player.getUniqueId());
        }

        player.setLevel(0);
        player.setTotalExperience(0);
        player.setExp(0f);
        player.setFoodLevel(20);

        firstGapple.remove(player.getUniqueId());
        arrowCooldown.remove(player.getUniqueId());
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent e) {
        HumanEntity entity = e.getEntity();
        if(entity instanceof Player) {
            if(getTeam(entity.getUniqueId()) != null) { // same as p.getUUID()
                e.setCancelled(true);
                ((Player) entity).setFoodLevel(20);
            }
        }
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent e) {
        if(e.getItem().getType() == Material.GOLDEN_APPLE) {
            Player p = e.getPlayer();
            UUID u = p.getUniqueId();
            if(getTeam(u) == null) return;
            e.setCancelled(true);
            p.setHealth(p.getMaxHealth());
            if(config.isAbsorptionOnFirstGap()) {
                if(firstGapple.contains(u))
                    return; // not first gapple
            }
            p.removePotionEffect(PotionEffectType.ABSORPTION);
            p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 69 * 20, 0, false, false));
            firstGapple.add(u);
        }
    }

    @EventHandler
    public void onShoot(ProjectileLaunchEvent e) {
        if(e.getEntity() instanceof Arrow && e.getEntity().getShooter() instanceof Player) {
            Player shooter = (Player) e.getEntity().getShooter();
            if(getTeam(shooter.getUniqueId()) == null) return;
            if(arrowCooldown.containsKey(shooter.getUniqueId())) {
                e.setCancelled(true);
                return; // player is still on arrow cooldown
            }
            if(config.getArrowKbStrength() != null)
                ((Arrow) e.getEntity()).setKnockbackStrength(config.getArrowKbStrength());
            setPickupStatus((Arrow) e.getEntity(), 2); // 2 for CREATIVE-ONLY

            ShootCooldown shootCooldown = new ShootCooldown(shooter, arrowCooldown, config.getBowCooldown(), config.isActionbarArrowCooldown());
            arrowCooldown.put(shooter.getUniqueId(), shootCooldown);
            shootCooldown.runTaskTimer(plugin, 1, 1);
        }
    }
    @EventHandler
    public void onLand(ProjectileHitEvent e) {
        Location loc = e.getEntity().getLocation();
        if(loc.getWorld() == world && arenaBox.contains(loc))
            e.getEntity().remove();
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Location to = e.getTo();
        if(e.getFrom().getBlockX() == to.getBlockX() &&
                e.getFrom().getBlockY() == to.getBlockY() &&
                e.getFrom().getBlockZ() == to.getBlockZ())
            return; // didnt move 1 block
        BridgeTeam team = getTeam(e.getPlayer().getUniqueId());
        if(team == null)
            return;

        if(!e.getPlayer().getWorld().getName().equals(worldName)) {
            e.setCancelled(true);
            return; // escaped to another world
        }

        // 1: check if player entered the void
        if(e.getTo().getBlockY() <= voidY) {
            UUID uuid = e.getPlayer().getUniqueId();
            if(!combatTag.containsKey(uuid))
                combatTag.put(uuid, CombatTag.ofEmpty(uuid));
            CombatTag tag = combatTag.get(uuid);
            tag.voidify();

            e.setTo(team.getRespawnLoc());
            onPlayerDie(e.getPlayer(), team, tag);
            return;
        }

        // 2: check if player is trying to exit the arena
        if(!arenaBox.contains(to)) {
            e.setCancelled(true);
            return;
        }
        // 3: check if player entered own goal
        for(BoundingBox box : team.getGoal()) {
            if(box.contains(to)) {
                Location toLoc = to.subtract(0, 3, 0);
                Player p = e.getPlayer();
                if(p.isOnGround()) // TODO: Temporary, make set in config
                    e.setTo(toLoc);
                return;
            }
        }
        // 4: check if player entered other team's goal

        // GameState isnt IDLE or REGENERATING (someone already won)
        // and GameState not SCORING (currently someone just scored faster)

//        if(gameState != GameState.SCORING && gameState != GameState.REGENERATING) {
        if(gameState != GameState.SCORING && gameState.hasStarted()) {
            for(BoundingBox box : otherTeam(team).getGoal()) {
                if(box.contains(to)) {
                    gameState = GameState.SCORING;
                    onScore(e.getPlayer(), team);
                }
            }
        }

        // 5: check if player escaped the GOAL box while still in IN_BOX state
        if(gameState == GameState.IN_BOX && !team.getSpawnBox().contains(e.getTo())) {
            if(team.getSpawnBox().contains(e.getFrom()))
                e.setCancelled(true);
            else
                e.setTo(team.getSpawnLoc());
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        if(!gameState.hasStarted() && queue.contains(uuid)) {
            leaveQueue(e.getPlayer());
        }else if(gameState.hasStarted()) {
            BridgeTeam team = getTeam(uuid);
            if(team == null)
                return;
            disconnected.add(uuid);
            broadcastMessage(team.getColor() + e.getPlayer().getName() + ChatColor.GRAY + " has disconnected.");
            checkNoTeamPlayers(uuid);
        }
    }
    private void checkNoTeamPlayers(UUID playerLeft) {
        BridgeTeam winningTeam;
        if(hasNoTeamPlayers(team1, playerLeft))
            winningTeam = team2;
        else if(hasNoTeamPlayers(team2, playerLeft)) {
            winningTeam = team1;
        }else return;

        onFinish(winningTeam);
    }
    private boolean hasNoTeamPlayers(BridgeTeam team0, UUID playerLeft) {
        // Check if a team has NO online players

        for(UUID uuid : team0.getPlayers()) {
            if(uuid.equals(playerLeft))
                continue;
            Player p = getPlayer(uuid);
            if(p != null && p.isOnline())
                return false; // there is at least 1 person
        }
        return true;
    }
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();
        BridgeTeam team = getTeam(uuid);
        if(team != null) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                p.teleport(team.getRespawnLoc());
                onPlayerDieEffect(p, team);
                TheBridge.getInstance().getTablistManager().getTablistTasks().get(uuid).initPackets();
                team.updateHealthScoreboard(p);
            }, 3);
            broadcastMessage(team.getColor() + e.getPlayer().getName() + ChatColor.GRAY + " has reconnected.");
        }
    }
    public void broadcastMessage(String str) {
        for(UUID uuid : team1.getPlayers()) {
            Player p = getPlayer(uuid);
            if(p != null)
                p.sendMessage(str);
        }
        for(UUID uuid : team2.getPlayers()) {
            Player p = getPlayer(uuid);
            if(p != null)
                p.sendMessage(str);
        }
    }
    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        Location loc = e.getBlockPlaced().getLocation();
        UUID uuid = e.getPlayer().getUniqueId();
        if(getTeam(uuid) == null)
            return; // player is not in game
        if(gameState == GameState.IN_BOX || gameState == GameState.REGENERATING) {
            e.setCancelled(true);
            return;
        }
        if(gameState.hasStarted()) {
            if(!placeBox.contains(loc) || loc.getWorld() != world) { // ensure in box and in same world
                e.setCancelled(true);
                return;
            }
            boolean disallow = false;
            for(BoundingBox box : disallowPlace) {
                if(box.contains(loc)) {
                    disallow = true;
                    break;
                }
            }
            // disallow has more priority than allow
            // so if its disallow, cancel immediately
            if(disallow) {
                e.setCancelled(true);
                return;
            }

            // check Y
            int y = loc.getBlockY();
            if(placeMinY != -1 && y < placeMinY) {
                // if placeMinY is set and it is lower, disallow
                e.setCancelled(true);
            }else if(placeMaxY != -1 && y > placeMaxY) {
                e.setCancelled(true);
            }
            if(!e.isCancelled()) placedBlocks.add(loc);
        }
    }
    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Location loc = e.getBlock().getLocation();
        UUID uuid = e.getPlayer().getUniqueId();
        if(getTeam(uuid) == null)
            return; // player is not in game
        if(gameState == GameState.IN_BOX) {
            e.setCancelled(true);
            return;
        }
        if(config.isNoBlockLootDrop()) {
            e.setDropItems(false);
            e.setExpToDrop(0);
        }
        if(gameState.hasStarted()) {
            if(placedBlocks.contains(loc))
                return; // Always allow to break placed blocks, onlyBreakPlacedBlocks disallows any block other than placed ones

            if(!arenaBox.contains(loc) || loc.getWorld() != world) {
                e.setCancelled(true);
                return;
            }
            if(config.isOnlyBreakPlacedBlocks()) {
                if(!placedBlocks.contains(loc)) {
                    e.setCancelled(true);
                    return;
                }
            }
            boolean isBlock = false;
            for(BlockType blockType : config.getAllowBreak()) {
                if(blockType.correct(e.getBlock().getType(), e.getBlock().getData())) {
                    isBlock = true;
                    break;
                }
            }
            if(isBlock) {
                for(BoundingBox box : disallowBreak) {
                    if(box.contains(loc)) {
                        // is block, but specifically disallowed
                        e.setCancelled(true);
                        break;
                    }
                }
            }else {
                for(BoundingBox box : allowBreak) {
                    if(box.contains(loc)) {
                        // is not block, but specifically allowed
                        return;
                    }
                }
                e.setCancelled(true);
            }
        }
    }


    public void joinQueue(Player p) {
        if(gameState.hasStarted()) {
            p.sendMessage(ChatColor.RED + "That game already started!");
            return;
        }
        if(queue.size() >= neededPlayers) {
            p.sendMessage(ChatColor.RED + "That game is full!");
            return;
        }
        queue.add(p.getUniqueId());
        broadcastQueue(ChatColor.GRAY + p.getName() + " joined the game! (" + queue.size() + "/" + maxPlayers + ")");
        if(queue.size() == neededPlayers) {
            startQueueCountdown(5, this::startGame);
        }
        p.getInventory().clear();
        p.teleport(joinLocation);
    }
    public void broadcastQueue(String str) {
        for(UUID uuid : queue) {
            Player p = getPlayer(uuid);
            if(p != null)
                p.sendMessage(str);
        }
    }
    public void leaveQueue(Player p) {
        if(!queue.contains(p.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "Error: You can't leave a game you're not in!");
            return;
        }
        if(gameState.hasStarted()) {
            BridgeTeam team = getTeam(p.getUniqueId());
            if(team == null) {
                p.sendMessage(ChatColor.RED + "Error: You can't leave a game you're not in!");
            }else {
                broadcastMessage(team.getColor() + p.getName() + ChatColor.GRAY + " has disconnected.");
                checkNoTeamPlayers(p.getUniqueId());
            }
            return;
        }
        broadcastQueue(ChatColor.GRAY + p.getName() + " left the game! (" + (queue.size() - 1) + "/" + maxPlayers + ")");
        queue.remove(p.getUniqueId());
        if(queue.size() < neededPlayers) {
            broadcastQueue(ChatColor.RED + "\nFailed to meet player requirements.\n");
            if(countdownRunnable != null) {
                countdownRunnable.cancel();
                countdownRunnable = null;
            }
            countdown = -1;
        }
        if(config.isTeleportSpawnAfterLeave()) {
            p.teleport(config.getSpawnLocation());
        }
    }

    public void startQueueCountdown(int countdown1, Runnable run) {
        this.countdown = countdown1;
        countdownRunnable = new BukkitRunnable() {
            @Override
            public void run() {
                if(countdown > 0) {
                    queue.forEach((uuid) -> {
                        Player p = getPlayer(uuid);
                        if(p != null) {
                            if(config.isQueueTitleEnabled()) {
                                config.getQueueTitleTime().sendTitle(p, "", config.getQueueTitleColorFormat().getColor(countdown) + countdown);
                            }
                            if(config.isQueueMessageEnabled()) {
                                p.sendMessage(ChatColor.YELLOW + "The game starts in " +
                                        config.getQueueMessageColorFormat().getColor(countdown) + countdown + ChatColor.YELLOW + " seconds!");
                            }
                            config.getQueueCountdownSound().playSound(p);
                        }
                    });
                }else {
                    run.run();
                    cancel();
                    countdownRunnable = null;
                }
                countdown--;
            }
        };
        countdownRunnable.runTaskTimer(plugin, 0, 20);
    }
    public void onScore(Player p, BridgeTeam team) {
        team.setScore(team.getScore()+1);
        if(team.getScore() >= config.getGoalCount()) {
            onFinish(team);
            return;
        }
        String title = team.getColor() + p.getName() + " scored!";
        team1.start(world, false, () -> startGameBox(title));
        team2.start(world, false, () -> startGameBox(title));
    }
    public long getGameTimeLeft() { // in ticks, time left from 15minutes
        long n = config.getGameTime() - getGameTime();
//        if(n < 0)
//            return 0;
//        else
//            return n;
        return Math.max(n, 0);
    }
    public long getGameTime() {
        if(gameState.hasStarted())
            return TheBridge.getCurrentTick() - tickStarted;
        else
            return gameTime;
    }

    public void onTimeLimitOver() {
        if(countdownRunnable != null) {
            countdownRunnable.cancel();
            countdownRunnable = null;
        }

        gameState = GameState.REGENERATING;
        BridgeTeam winner = team1.getScore() > team2.getScore() ? team1 : team2;

        broadcastMessage(ChatColor.YELLOW + "Reached time limit!");
        if(team1.getScore() == team2.getScore()) {
            /*
            DRAW
             */
            String title = ChatColor.YELLOW + "DRAW!";
            String subTitle = ChatColor.YELLOW + "Reached time limit.";

            if(config.isWinTitleEnabled())
                title(title, subTitle, config.getWinTitleTime());

            onEndGame();
        }else {
            onFinish(winner);
        }
    }

    public void onFinish(BridgeTeam team) { // team = winner
        gameState = GameState.REGENERATING;
        String title = team.getColor() + team.getTeamName() + " WINS!";
        BridgeTeam other = otherTeam(team);
        String subTitle = "" + team.getColor() + team.getScore() + " " + ChatColor.GRAY + "- " + other.getColor() + other.getScore();

        if(config.isWinTitleEnabled())
            title(title, subTitle, config.getWinTitleTime());

        onEndGame();
    }
    public void onEndGame() {
        for(UUID uuid : queue) {
            Player p = getPlayer(uuid);
            if(p != null) {
                onPlayerDieEffect(p, getTeam(uuid));

                p.setGameMode(config.getGamemodeFinished());
                p.teleport(joinLocation);
                if(config.isClearInventoryAfterGame()) {
                    p.getInventory().clear();
                    p.getInventory().setArmorContents(new ItemStack[0]);
                    p.getInventory().setContents(new ItemStack[0]);
                }
            }
        }

        gameTime = TheBridge.getCurrentTick() - tickStarted;
        if(gameTime > config.getGameTime()) gameTime = config.getGameTime();

        List<UUID> queueCopy = new ArrayList<>(queue);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> onPlayerReset(queueCopy), 10 * 20);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, this::cleanUp, (this instanceof BridgeArenaUnlimited) ? 30 * 20 : 5 * 20);
    }
    public void onPlayerReset(List<UUID> queueCopy) {
        queueCopy.forEach(uuid -> {
            Player p = getPlayer(uuid);
            if(p != null) {
                p.setGameMode(config.getGamemodeAfterGame());
                if(config.isTeleportSpawnAfterGame())
                    p.teleport(config.getSpawnLocation());
                p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        });
    }
    public void cleanUp() {
        // clean up after finish
        BSchematic.pasteSchematic(world, uniqueId + ".schem", (output) -> {
            logger.info("Finished re-pasting/regenerating");
            gameState = GameState.IDLE;
        });
        queue.clear();
        disconnected.clear();
        firstGapple.clear();
        arrowCooldown.clear();
        placedBlocks.clear();
        team1.finishUp(); team2.finishUp();
    }

    public boolean splitTeams() {
        // random split team
        try {
            Collections.shuffle(queue, SecureRandom.getInstanceStrong());
        }catch(GeneralSecurityException e) {
            Collections.shuffle(queue);
        }
        if(queue.size() > maxPlayers) {
            broadcastQueue(ChatColor.RED + "Error: The game exceeded the maximum player count");
            queue.clear();
            return false;
        }
        if(queue.size() < neededPlayers) {
            broadcastQueue(ChatColor.RED + "Error: The game didn't meet the needed players count");
            queue.clear();
            return false;
        }
        team1.getPlayers().clear();
        team2.getPlayers().clear();

        int max1 = team1.getMaxPlayers();
        int max2 = team2.getMaxPlayers();
        UUID[] t1 = new UUID[max1];
        UUID[] t2 = new UUID[max2];

        if(t2.length < t1.length) {
            UUID[] a = t2;
            t2 = t1;
            t1 = a;
        }
        try {
            Collections.shuffle(queue, SecureRandom.getInstanceStrong());
        }catch(GeneralSecurityException e) {
            Collections.shuffle(queue);
        }

        // the most "evenly fair" splitting-team algorithm
        // 3 -> [1,2] [3]
        // 4 -> [1,2] [3,4]
        // 5 -> [1,2,5] [3,4]
        boolean isMinOut = false; // min has no more space
        int currentInQueue = 0;
        for(int current = 0; current < t2.length; current++) {
            if(!isMinOut && current >= t1.length) {
                isMinOut = true;
            }
            if(currentInQueue >= queue.size())
                break;
            if(!isMinOut) {

                t1[current] = queue.get(currentInQueue++);
            }
            // t2 is assummed to not be out because current < t2.length

            if(currentInQueue >= queue.size())
                break;
            t2[current] = queue.get(currentInQueue++);
        }
        List<UUID> listT1 = Arrays.asList(t1);
        List<UUID> listT2 = Arrays.asList(t2);
        team1.getPlayers().addAll(listT1);
        team2.getPlayers().addAll(listT2);
        team1.getPlayers().remove(null);
        team2.getPlayers().remove(null); // null spaces not filled up because didn't meet max players (only met needed)

        return true;
    }
    public void startGame() {
        if(!splitTeams()) return;
        tickStarted = TheBridge.getCurrentTick();
        team1.start(world, true, () -> startGameBox(null));
        team2.start(world, true, () -> startGameBox(null));
    }
    private boolean tmp1 = false;
    // start game and they're in box
    public void startGameBox(String title) {
        if(!tmp1)
            tmp1 = true;
        else {
            tmp1 = false;
            onBox(title);
        }
    }
    public void onBox(String title) { // make teams spawn in box
        gameState = GameState.IN_BOX;
        team1.teleportBox();
        team2.teleportBox();
        for(UUID uuid : queue) {
            Player p = getPlayer(uuid);
            if(p == null) continue;
            onPlayerDieEffect(p, getTeam(uuid));
            p.setGameMode(config.getGamemodeInBox());
        }
        timeLimitTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if(!gameState.hasStarted()) {
                Bukkit.getScheduler().cancelTask(timeLimitTaskId);
                timeLimitTaskId = -1;
            }else {
                if(getGameTimeLeft() <= 0)
                    onTimeLimitOver();
            }
        }, 10, 10);
        startBridgeCountdown(3, () -> {
            gameState = GameState.STARTED;
//            BSchematic.pasteSchematic(uniqueId + "-nobox"+".schem", null);
            BSchematic.clearBox(world, team1.getSpawnBox());
            BSchematic.clearBox(world, team2.getSpawnBox());
            for(UUID uuid : queue) {
                Player p = getPlayer(uuid);
                if(p == null) continue;
                p.setGameMode(config.getGamemodeInGame());
            }
        }, title);
    }

    public void title(String title, String subTitle, TitleTime titleTime) {
        for(UUID uuid : team1.getPlayers()) {
            Player p = getPlayer(uuid);
            if(p != null)
                titleTime.sendTitle(p, title, subTitle);
        }
        for(UUID uuid : team2.getPlayers()) {
            Player p = getPlayer(uuid);
            if(p != null)
                titleTime.sendTitle(p, title, subTitle);
        }
    }
    public void playSound(SoundType sound) {
        for(UUID uuid : team1.getPlayers()) {
            Player p = getPlayer(uuid);
            if(p != null)
                sound.playSound(p);
        }
        for(UUID uuid : team2.getPlayers()) {
            Player p = getPlayer(uuid);
            if(p != null)
                sound.playSound(p);
        }
    }

    public void startBridgeCountdown(int countdown1, Runnable run, final String scoreText0) {
        final String scoreText = scoreText0 == null ? "" : scoreText0;

        this.countdown = countdown1;
        countdownRunnable = new BukkitRunnable() {
            @Override
            public void run() {
                if(countdown > 0) {

                    if(config.isCageTitleEnabled()) {
                        String countdownText = ChatColor.GRAY + "Cages open in " + config.getCageTitleColorFormat().getColor(countdown) +
                                countdown + 's' + ChatColor.GRAY + "...";
                        title(scoreText, countdownText, config.getCageTitleTime());
                    }
                    if(config.isCageFightMessageEnabled()) {
                        String countdownText = ChatColor.GREEN + "Cages open in " + config.getCageMessageColorFormat().getColor(countdown) +
                                countdown + ChatColor.GREEN + "s...";
                        broadcastMessage(countdownText);
                    }

                    playSound(config.getCageCountdownSound());
                }else {

                    if(config.isCageFightTitleEnabled())
                        title("", ChatColor.GREEN + "Fight!", config.getCageFightTitleTime());
                    if(config.isCageFightMessageEnabled())
                        broadcastMessage(ChatColor.GREEN + "Fight!");

                    playSound(config.getCageOpenSound());
                    countdown = 0;
                    run.run();
                    cancel();
                    countdownRunnable = null;
                }
                countdown--;
            }
        };
        countdownRunnable.runTaskTimer(plugin, 0, 20);
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("uuid", uniqueId);
        map.put("name", arenaName);
        map.put("map_name", mapName);
        map.put("world", worldName);
        map.put("box", arenaBox);
        map.put("join_location", joinLocation);
        map.put("void_y", voidY);

        Map<String, Object> teamMap = new HashMap<>();
        teamMap.put("1", team1);
        teamMap.put("2", team2);
        map.put("team", teamMap);


        Map<String, Object> buildMap = new HashMap<>();

        Map<String, Object> placeMap = new HashMap<>();
        placeMap.put("min_y", placeMinY);
        placeMap.put("max_y", placeMaxY);
        placeMap.put("disallow_boxes", disallowPlace);

        Map<String, Object> breakMap = new HashMap<>();
        breakMap.put("allow_boxes", allowBreak);
        breakMap.put("disallow_boxes", disallowBreak);

        buildMap.put("place", placeMap);
        buildMap.put("break", breakMap);
        map.put("build", buildMap);

        return map;
    }
    public void save() {
        if(file == null)
            logger.warning("An error occurred while saving an arena: Contact the author. Error no file registered");
        else {
            YamlConfiguration config = new YamlConfiguration();
            config.set("version", VERSION);
            config.set("arena", serialize());
            try {
                config.save(file);
            }catch(IOException e) {
                e.printStackTrace();
                logger.warning("IOException error occurred while saving an error");
            }
        }
    }
    public static BridgeArena deserialize(Map<String, Object> map) {
        BridgeArena arena = new BridgeArena();
        arena.uniqueId = UUID.fromString((String) map.get("uuid"));
        arena.arenaName = (String) map.get("name");
        arena.mapName = (String) map.get("map_name");
        arena.worldName = (String) map.get("world");
        arena.arenaBox = (BoundingBox) map.get("box");
        arena.joinLocation = (Location) map.get("join_location");
        arena.voidY = (int) map.get("void_y");

        Map<String, Object> buildMap = (Map<String, Object>) map.get("build");
        Map<String, Object> placeMap = (Map<String, Object>) buildMap.get("place");
        Map<String, Object> breakMap = (Map<String, Object>) buildMap.get("break");

        arena.placeMinY = (int) placeMap.get("min_y");
        arena.placeMaxY = (int) placeMap.get("max_y");
        arena.disallowPlace = (List<BoundingBox>) placeMap.get("disallow_boxes");

        arena.allowBreak = (List<BoundingBox>) breakMap.get("allow_boxes");
        arena.disallowBreak = (List<BoundingBox>) breakMap.get("disallow_boxes");

        Map<Object, Object> teamMap = (Map<Object, Object>) map.get("team");
        // for some reason, strings are being converted to integers
        // Map<String, Object> ---> Map<Object, Object>

        arena.team1 = (BridgeTeam) teamMap.getOrDefault(1, teamMap.get("1"));
        arena.team2 = (BridgeTeam) teamMap.getOrDefault(2, teamMap.get("2"));

        int b = TheBridge.getInstance().getBridgeConfig().getPlaceBoxShrink();
        if(b > 0) {
            // + for min, - for max
            boolean hasY = arena.arenaBox.getMinY() != null && arena.arenaBox.getMaxY() != null;
            arena.placeBox = new BoundingBox(
                    arena.arenaBox.getMinX() + b, hasY ? arena.arenaBox.getMinY() + b : null, arena.arenaBox.getMinZ() + b,
                    arena.arenaBox.getMaxX() - b, hasY ? arena.arenaBox.getMaxY() - b : null, arena.arenaBox.getMaxZ() - b);
        }else arena.placeBox = arena.arenaBox;

        if(!arena.isReady()) {
            Logger logger = arena.getLogger();
            if(logger == null) {
                TheBridge.getInstance().getLogger().warning("[Arena/"+arena.arenaName+"] Arena or config is incomplete or misconfigured. (Arena is not ready for play)");
            }else
                logger.warning("Arena or config is incomplete or misconfigured. (Arena is not ready for play)");
        }

        return arena;
    }
}