package me.bramar.thebridge.util.schematic;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.bramar.thebridge.TheBridge;
import me.bramar.thebridge.model.BoundingBox;
import me.bramar.thebridge.model.ChunkIntPair;
import me.bramar.thebridge.util.ModuleLogger;
import me.bramar.thebridge.util.nms.*;
import me.bramar.thebridge.util.nms.packet.NMSMultiBlockChangeInfo;
import me.bramar.thebridge.util.nms.packet.NMSMultiBlockChangePacket;
import org.bukkit.*;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;

// bramar2's Schematic API without WorldEdit
// - Properties -
// Medium NMS usage for: Fast block pasting, send chunk packets, getting entities
// Avoid WorldEdit / FAWE hard dependency
// Most actions able to be done asynchronously, is done asynchronously.
public class BSchematic {
    private static final TheBridge plugin = TheBridge.getInstance();
    private static final ModuleLogger logger = new ModuleLogger("Schematic");
    private static final NMSUtil nms = plugin.getNMSUtil();

    public static List<ChunkBlockInfo> getChunkInfos(World world, BoundingBox box) {
        int lastXB4 = Integer.MIN_VALUE; // continue on already inputted chunks
        List<ChunkBlockInfo> blockInfos = new ArrayList<>();

        for(int x = box.getMinX(); x <= box.getMaxX(); x++) {
            int shiftedX = x >> 4;
            if(shiftedX == lastXB4) continue; // chunk is already inputted
            lastXB4 = shiftedX;

            int lastZB4 = Integer.MIN_VALUE;
            for(int z = box.getMinZ(); z <= box.getMaxZ(); z++) {
                int shiftedZ = z >> 4;
                if(shiftedZ == lastZB4) continue;
                lastZB4 = shiftedZ;

                Chunk chunk = world.getChunkAt(shiftedX, shiftedZ);
                ChunkBlockInfo cbi = new ChunkBlockInfo(chunk.getChunkSnapshot(true, false, false), shiftedX, shiftedZ);
                blockInfos.add(cbi);
            }
        }

        return blockInfos;
    }

    private static void asyncSaveSchematic(BoundingBox box, List<ChunkBlockInfo> chunkInfos, SchemFile output) {
        int minY = box.getMinY();
        int maxY = box.getMaxY();
        for(ChunkBlockInfo cbi : chunkInfos) {
            SchemChunk sc = new SchemChunk();
            sc.x = cbi.chunkX;
            sc.z = cbi.chunkZ;
            for(int y = minY; y <= maxY; y++) {
                SchemY sy = new SchemY();
                sy.y = y;
                for(int x = 0; x < 16; x++) {
                    SchemX sx = new SchemX();
                    sx.x = x;
                    if(!box.inRangeX(16 * cbi.chunkX + x))
                        continue;
                    for(int z = 0; z < 16; z++) {
                        if(!box.inRangeZ(16 * cbi.chunkZ + z))
                            continue;
                        int type = cbi.snapshot.getBlockTypeId(x, y, z);
                        if(type == Material.AIR.getId()) continue; // don't save AIR
                        int data = cbi.snapshot.getBlockData(x, y, z);
                        SchemBlock sb = new SchemBlock();
                        sb.z = z;
                        sb.a = type;    sb.b = data;
                        sx.v.add(sb);
                    }
                    sy.v.add(sx);
                }
                sc.v.add(sy);
            }
            output.v.add(sc);
        }
    }

    // NOTE: This uses the normal SLOW method
    // Only supposed to be used for max 5k blocks or it will slow the server a lot
    // currently used for clearing boxes which should be < 250
    public static void clearBox(World world, BoundingBox box) {
        if(box.getMinY() == null || box.getMaxY() == null)
            return;
        NMSWorld nmsWorld = nms.getWorld(world);
        NMSBlockData air = nms.getBlockData(0, 0);
        for(int x = box.getMinX(); x <= box.getMaxX(); x++) {
            for(int y = box.getMinY(); y <= box.getMaxY(); y++) {
                for(int z = box.getMinZ(); z <= box.getMaxZ(); z++) {
                    if(world.getBlockAt(x, y, z).getType() != Material.AIR)
                        nmsWorld.setTypeAndData(x, y, z, air); // auto no physics (2)
                }
            }
        }
    }

    // Automatic chunk-refresh using PacketPlayOutMultiBlockChange
    // onDone is fired in the Main thread
    public static void pasteSchematic(World bukkitWorld, String schemName, Consumer<SPasteOutput> onDone) {
        try {
            File file = new File(
                    plugin.getDataFolder(), "maps/" + schemName
            );
            if(!file.exists()) {
                logger.warning("Unable to paste a non-existing schematic/bridge map: '" + schemName + "'");
                return;
            }
            long t1 = System.currentTimeMillis();
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
            SchemFile schemFile = SchemFile.deserialize1(bis);
            bis.close();
            fis.close();

            NMSWorld nmsWorld = nms.getWorld(bukkitWorld);

            final Map<XYZCoords, int[]> blocksMap = new HashMap<>();
            // a map of non-air blocks
            // ASYNC to get blocks, so it doesnt lag main thread
            final Map<SchemChunk, ChunkSnapshot> chunkSnapshots = new HashMap<>();
            for(SchemChunk schemChunk : schemFile.v) {
                chunkSnapshots.put(schemChunk, bukkitWorld.getChunkAt(schemChunk.x, schemChunk.z).getChunkSnapshot());
            }
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                Integer minX = null;
                Integer maxX = null;
                Integer minZ = null;
                Integer maxZ = null;
                for(SchemChunk schemChunk : schemFile.v) {
                    int cX = schemChunk.x;
                    int cZ = schemChunk.z;
                    ChunkSnapshot snapshot = chunkSnapshots.get(schemChunk);

                    for(SchemY schemY : schemChunk.v) {
                        List<XYZCoords> exempts = new ArrayList<>();
                        int y = schemY.y;
                        for(SchemX schemX : schemY.v) {
                            int pcX = schemX.x;
                            int x = cX * 16 + schemX.x;
                            for(SchemBlock schemBlock : schemX.v) {
                                int z = cZ * 16 + schemBlock.z;
                                int pcZ = schemBlock.z;

                                if(minZ == null || z < minZ) minZ = z;
                                if(maxZ == null || z > maxZ) maxZ = z;

                                int typeId = snapshot.getBlockTypeId(pcX, y, pcZ);
                                int data = snapshot.getBlockData(pcX, y, pcZ);
                                XYZCoords t = new XYZCoords(x, y, z);

                                if(typeId == schemBlock.a && data == schemBlock.b) {
                                    exempts.add(t);
                                    continue;
                                }
                                blocksMap.put(t, new int[] {schemBlock.a, schemBlock.b});
                            }
                            if(minX == null || x < minX) minX = x;
                            if(maxX == null || x > maxX) maxX = x;
                        }
                        // Set placed blocks to AIR
                        // because the above will only replace replaced/broken
                        for(int tx = 0; tx < 16; tx++) {
                            for(int tz = 0; tz < 16; tz++) {
                                // tx, tz = per-chunk coords 0-15
                                int x = cX * 16 + tx;
                                int z = cZ * 16 + tz;
                                XYZCoords t = new XYZCoords(x, y, z);
                                if(exempts.remove(t))
                                    continue;
                                if(!blocksMap.containsKey(t)) {
                                    int typeId = snapshot.getBlockTypeId(tx, y, tz);
                                    if(typeId != Material.AIR.getId()) // not AIR
                                        blocksMap.put(t, new int[] {0,0});
                                }
                                // If the schematic does not have it or the block is in it but was not the same based on not exempt,
                                // set the block to AIR
                            }
                        }
                    }
                }
                Integer finalMinX = minX;
                Integer finalMaxX = maxX;
                Integer finalMinZ = minZ;
                Integer finalMaxZ = maxZ;
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                    long t2 = System.currentTimeMillis();
                    int minY = schemFile.ay;
                    int maxY = schemFile.by;
                    Map<ChunkIntPair, NMSChunk> chunkMap = new HashMap<>();
                    Map<NMSChunk, List<StructMultiBlockChangeInfo>> multiBlockMap = new HashMap<>();

                    for(int y = minY; y <= maxY; y++) {
                        for(int x = finalMinX; x <= finalMaxX; x++) {
                            for(int z = finalMinZ; z <= finalMaxZ; z++) {
                                NMSChunk nmsChunk;
                                ChunkIntPair pair = new ChunkIntPair(x >> 4, z >> 4);
                                if(chunkMap.containsKey(pair))
                                    nmsChunk = chunkMap.get(pair);
                                else {
                                    chunkMap.put(pair, nmsChunk = nmsWorld.getChunkAt(x >> 4, z >> 4));
                                }
                                XYZCoords coords = new XYZCoords(x, y, z);
                                if(blocksMap.containsKey(coords)) {
                                    int[] i = blocksMap.get(coords);
                                    int id = i[0];
                                    int data = i[1];
//                                    BlockPosition pos = new BlockPosition(x, y, z);
                                    if(!multiBlockMap.containsKey(nmsChunk))
                                        multiBlockMap.put(nmsChunk, new ArrayList<>());

                                    NMSBlockData bd = nms.getBlockData(id, data);
                                    short location = (short) ((x & 15) << 12 | (z & 15) << 8 | y);
                                    multiBlockMap.get(nmsChunk).add(new StructMultiBlockChangeInfo(location, bd));

                                    nmsChunk.setBlockData(x, y, z, bd);
//                                    nmsChunk.a(pos, ibd); // setBlockState() [MOJANG] | setBlockData() [SPIGOT]
                                }
                            }
                        }
                    }

                    helperUpdateClients(chunkMap.values(), multiBlockMap, nmsWorld);

                    chunkMap = null;
                    blocksMap.clear();
                    multiBlockMap.clear();
                    multiBlockMap = null;

                    logger.fine("Successfully pasted schematic " + schemName);
                    if(onDone != null) {
                        long current = System.currentTimeMillis();
                        long totalTime = current - t1;
                        long mainTime = current - t2;
                        onDone.accept(new SPasteOutput(totalTime, mainTime));
                    }
                }, 0);
            }, 0);
        }catch(Exception e1) {
            e1.printStackTrace();
            logger.warning("Failed to paste schematic/bridge map");
        }
    }
    // helper util
    private static void helperUpdateClients(Collection<NMSChunk> chunkList,
                                     Map<NMSChunk, List<StructMultiBlockChangeInfo>> multiBlockMap,
                                     NMSWorld nmsWorld) {
        int viewDistance = Bukkit.getViewDistance() << 4;
        Map<NMSChunk, NMSMultiBlockChangePacket> blockPackets = new HashMap<>();
        for(NMSChunk chunk : chunkList) {
            if(!multiBlockMap.containsKey(chunk))
                continue;
            List<StructMultiBlockChangeInfo> list = multiBlockMap.get(chunk);
            if(list.isEmpty())
                continue;
            for(NMSEntityPlayer ep : nmsWorld.getPlayers()) {
                int blocksX = (chunk.getLocX() + 1) << 4;
                int blocksZ = (chunk.getLocZ() + 1) << 4;
                double distanceX = Math.abs(ep.getLocX() - blocksX);
                double distanceZ = Math.abs(ep.getLocZ() - blocksZ);
                if(distanceX <= viewDistance && distanceZ <= viewDistance) {
                    if(!blockPackets.containsKey(chunk)) {
                        // create packet
                        NMSMultiBlockChangePacket packet;
                        try {
                            packet = nms.newMultiBlockChange();
                            NMSMultiBlockChangeInfo[] infoArray = new NMSMultiBlockChangeInfo[list.size()];
                            for(int i = 0; i < list.size(); i++) {
                                StructMultiBlockChangeInfo struct = list.get(i);
                                infoArray[i] = packet.newInfo(struct.loc, struct.blockData);
                            }
                            packet.setChunk(chunk.getLocX(), chunk.getLocZ());
                            packet.setInfo(infoArray);
                        }catch(Exception e2) {
                            e2.printStackTrace();
                            logger.severe("Failed to update client blocks (Reflection error). Block desyncs might happen!");
                            logger.severe("Make sure you are on 1.8 - 1.12 with Java 8 or above. If you are, contact the author along with the error.");
                            continue;
                        }
                        blockPackets.put(chunk, packet);
                    }
                    blockPackets.get(chunk).sendPacket(ep);
                }
            }
        }
    }


    public static void saveSchematic(String schemName, World world, BoundingBox box, Runnable onFail, final Consumer<SSaveOutput> onSuccess) {
        plugin.currentlySavingSchem = true;
        long t1 = System.currentTimeMillis();
        final List<ChunkBlockInfo> chunkInfos = getChunkInfos(world, box);
        List<Thread> threads = new ArrayList<>();
        final SchemFile output = new SchemFile();
        output.w = world.getName();
        output.ay = box.getMinY(); output.by = box.getMaxY();

        int amountOfThreads = 5;
        if(chunkInfos.size() <= amountOfThreads) {
            // 1 thread async
            logger.info("Saving new schematic: " + schemName + ", at 1 async thread");
            Thread thread = new Thread(() -> asyncSaveSchematic(box, chunkInfos, output));
            threads.add(thread);
            Thread waiter = new Thread(new SchemSaveWaiter(t1, schemName, output, threads, onFail, onSuccess));
            waiter.start();
        }else {
            logger.info("Saving new schematic: " + schemName + ", at "+amountOfThreads+" async threads");
            int size = chunkInfos.size();

            List<List<ChunkBlockInfo>> divisions = new ArrayList<>();
            int ia = size / amountOfThreads;
            int ic = 0;
            for(int i = 0; i < amountOfThreads; i++) {
                divisions.add(
                        new ArrayList<>(chunkInfos.subList(ic, ic + ia))
                );
                ic += ia;
            }
            List<ChunkBlockInfo> subList = new ArrayList<>(chunkInfos.subList(ic, size));
            if(!subList.isEmpty())
                for(int i = 0; i < divisions.size(); i++) {
                    if(!(i < subList.size()))
                        break;
                    List<ChunkBlockInfo> l = divisions.get(i);
                    l.add(subList.get(i));
                }

            // starting threads
            for(int i = 0; i < amountOfThreads; i++) {
                int finalI = i;
                Thread thread = new Thread(() -> asyncSaveSchematic(box, divisions.get(finalI), output));
                threads.add(thread);
            }
            Thread waiter = new Thread(new SchemSaveWaiter(t1, schemName, output, threads, onFail, onSuccess));
            waiter.start();
        }
    }

    public static void convert(String schemName) {
        File file = new File(
                plugin.getDataFolder(), "maps/" + schemName
        );
        if(!file.exists()) {
            logger.warning("Unable to paste a non-existing schematic/bridge map: '" + schemName + "'");
            return;
        }
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            SchemFile schemFile = (SchemFile) ois.readObject();
            ois.close();
            fis.close();
            StringBuilder builder = new StringBuilder();
            schemFile.serialize1(builder);
            FileOutputStream fos = new FileOutputStream(new File(plugin.getDataFolder(), "maps/" + schemName + ".converted"));
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            bos.write(builder.toString().getBytes());
            bos.close();
            fos.close();
        }catch(Exception e1) {
            e1.printStackTrace();
            throw new RuntimeException(e1);
        }
    }

    @AllArgsConstructor
    private static class StructMultiBlockChangeInfo {
        private final short loc;
        private final NMSBlockData blockData;
    }

    @AllArgsConstructor @Getter
    public static class SSaveOutput {
        private final long totalTime;
        private final double kbSize, mbSize;
    }

    @AllArgsConstructor @Getter
    public static class SPasteOutput {
        private final long totalTime, mainTime;
    }


    @AllArgsConstructor @EqualsAndHashCode
    private static class XYZCoords {
        private int x, y, z;
    }

    // Java-serialized Schematic objects

    //

    private static class SchemSaveWaiter implements Runnable {
        final long t1;
        List<Thread> threads;
        SchemFile output;
        String schemName;
        final Runnable onFail;
        final Consumer<SSaveOutput> onSuccess;
        private SchemSaveWaiter(long t1,
                                String schemName,
                                SchemFile output,
                                List<Thread> threads,
                                final Runnable onFail,
                                final Consumer<SSaveOutput> onSuccess) {
            this.t1 = t1;
            this.schemName = schemName;
            this.output = output;
            this.threads = threads;
            this.onFail = onFail;
            this.onSuccess = onSuccess;
        }
        private void err(Runnable onFail, List<Thread> threads) {
            for(Thread thread : threads) {
                if(thread.isAlive()) thread.interrupt();
            }
            if(onFail != null)
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, onFail, 1);
        }

        @Override
        public void run() {
            for(Thread thread : threads) {
                thread.start();
            }
            try {
                for(Thread thread : threads) {
                    thread.join();
                }
            }catch(InterruptedException e1) {
                plugin.currentlySavingSchem = false;
                e1.printStackTrace();
                logger.warning("[ASYNC] Failed to save schematic: One/multiple threads were interrupted");
                err(onFail, threads);threads = null; output = null; schemName = null;
                return;
            }catch(Exception e1) {
                plugin.currentlySavingSchem = false;
                e1.printStackTrace();
                logger.warning("[ASYNC] Failed to save schematic: One/multiple threads had errors");
                logger.warning("[ASYNC] If this error keeps happening, report it to the author!");
                err(onFail, threads);threads = null; output = null; schemName = null;
                return;
            }
            File file = new File(plugin.getDataFolder(), "maps/" + schemName);
            FileOutputStream fos = null;
            ObjectOutputStream oos = null;
            output.v = new HashSet<>(output.v);
            try {
                fos = new FileOutputStream(file);
                oos = new ObjectOutputStream(fos);
                oos.writeObject(output);
                oos.close();
                fos.close();
                logger.fine("[ASYNC] Saved schematic/map to " + schemName);
                final double kbSize = file.length() / 1024d;
                final double mbSize = kbSize / 1024d;
                final long time = System.currentTimeMillis() - t1;
                final Consumer<SSaveOutput> consumer = onSuccess;
                if(consumer != null)
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                        SSaveOutput output = new SSaveOutput(time, kbSize, mbSize);
                        consumer.accept(output);
                    }, 0);
            }catch(Exception e1) {
                plugin.currentlySavingSchem = false;
                try {
                    if(oos != null) oos.close();
                    if(fos != null) fos.close();
                }catch(IOException ignored) {}
                e1.printStackTrace();
                logger.warning("[ASYNC] Failed to save schematic: Unable to save to disk");
                logger.warning("[ASYNC] If this keeps happening, contact the author!");
                if(onFail != null)
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, onFail, 1);
            }
            threads = null; output = null; schemName = null;
            plugin.currentlySavingSchem = false;
        }
    }

    @AllArgsConstructor
    private static class ChunkBlockInfo {
        ChunkSnapshot snapshot;
        int chunkX, chunkZ;
    }
}
