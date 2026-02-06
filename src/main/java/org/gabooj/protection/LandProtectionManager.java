package org.gabooj.protection;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.worlds.WorldManager;

public class LandProtectionManager {

    public WorldManager worldManager;
    public JavaPlugin plugin;
    public LandProtectionListener listener;

    public static final Map<UUID, Map<Long, Set<Integer>>> protectionsByWorld = new HashMap<>();

    public LandProtectionManager(JavaPlugin plugin, WorldManager worldManager) {
        this.plugin = plugin;
        this.worldManager = worldManager;
        this.listener = new LandProtectionListener(plugin, this);
    }

    public void onEnable() {
        LandProtectionSerializer.load(plugin);
        listener.onEnable();
    }

    public void onDisable() {
        LandProtectionSerializer.save(plugin);
    }

    public static long chunkKey(int x, int z) {
        return (((long) x) << 32) ^ (z & 0xffffffffL);
    }

    public static int sectionY(int blockY) {
        return blockY >> 4;
    }

    public boolean isClaimed(UUID worldId, int chunkX, int chunkZ, int sectionY) {
        Map<Long, Set<Integer>> worldClaims = protectionsByWorld.get(worldId);
        if (worldClaims == null) return false;

        Set<Integer> sections = worldClaims.get(chunkKey(chunkX, chunkZ));
        return sections != null && sections.contains(sectionY);
    }

    public boolean isClaimed(Location loc) {
        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;
        int sectionY = sectionY(loc.getBlockY());
        UUID worldId = loc.getWorld().getUID();
        return isClaimed(worldId, chunkX, chunkZ, sectionY);
    }

    public boolean isClaimed(Block block) {
        int chunkX = block.getX() >> 4;
        int chunkZ = block.getZ() >> 4;
        int sectionY = sectionY(block.getY());
        UUID worldId = block.getWorld().getUID();
        return isClaimed(worldId, chunkX, chunkZ, sectionY);
    }

}
