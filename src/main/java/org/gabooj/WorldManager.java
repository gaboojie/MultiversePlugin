package org.gabooj;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class WorldManager {

    private final JavaPlugin plugin;
    public Map<String, WorldMeta> worlds;
    private final Set<String> baseWorldIds = new HashSet<>();

    public WorldManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void onEnable() {
        loadWorlds();
    }

    public void onDisable() {
        saveWorlds(plugin);
    }

    public void loadWorlds() {
        // Load world data
        File worldFile = new File(plugin.getDataFolder(), "worlds.yml");
        worlds = WorldMeta.loadWorlds(worldFile);
        loadBaseWorldIDs();
        loadBaseWorld();
    }

    public World getWorldByMeta(WorldMeta meta) {
        return Bukkit.getWorld(meta.worldID);
    }

    public boolean doesWorldIDExist(String worldID) {
        for (WorldMeta meta : worlds.values()) {
            if (meta.worldID.equalsIgnoreCase(worldID)) {
                return true;
            }
        }
        for (String baseWorldID : baseWorldIds) {
            if (baseWorldID.equalsIgnoreCase(worldID)) {
                return true;
            }
        }
        return false;
    }

    public List<String> getWorldNames() {
        return worlds.keySet().stream().toList();
    }

    public List<String> getVisibleWorldNames() {
        List<String> worldMetas = new ArrayList<>();
        for (WorldMeta meta : worlds.values()) {
            if (meta.visible) worldMetas.add(meta.worldName);
        }
        return worldMetas;
    }

    public boolean doesWorldNameExist(String name) {
        for (WorldMeta meta : worlds.values()) {
            if (meta.worldName.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private void loadBaseWorldIDs() {
        for (World world : Bukkit.getWorlds()) {
            baseWorldIds.add(world.getName().toLowerCase());
        }
    }

    private void loadBaseWorld() {
        World baseWorld = Bukkit.getWorlds().get(0);
        if (!worlds.containsKey(baseWorld.getName())) {
            WorldMeta meta = new WorldMeta(
               true, baseWorld.getName()
            );
            meta.worldName = baseWorld.getName();
            meta.gameMode = GameMode.SURVIVAL;
            meta.difficulty = Difficulty.HARD;
            meta.autoLoad = true;
            meta.forceSpawn = false;
            meta.doHardcore = baseWorld.isHardcore();
            meta.type = baseWorld.getWorldType();
            meta.generator = WorldMeta.GeneratorType.VANILLA;
            meta.seed = baseWorld.getSeed();
            meta.status = WorldMeta.Status.LOADED;
            meta.generateStructures = baseWorld.canGenerateStructures();
            meta.environment = baseWorld.getEnvironment();
            meta.visible = true;

            Location spawn = baseWorld.getSpawnLocation();
            meta.spawnLocX = spawn.getX();
            meta.spawnLocY = spawn.getY();
            meta.spawnLocZ = spawn.getZ();
            meta.spawnLocYaw = spawn.getYaw();
            meta.spawnLocPitch = spawn.getPitch();
            worlds.put(meta.worldID, meta);
        } else {
            WorldMeta meta = worlds.get(baseWorld.getName());
            meta.status = WorldMeta.Status.LOADED;
        }
    }

    public void saveWorlds(JavaPlugin plugin) {
        File worldFile = new File(plugin.getDataFolder(), "worlds.yml");
        try {
            WorldMeta.saveWorlds(worldFile, worlds.values());
        } catch (IOException e) {
            plugin.getServer().broadcastMessage(ChatColor.RED + "\nBIG ERROR: COULD NOT SAVE WORLD DATA.!\n");
        }
    }

    public boolean unloadWorld(WorldMeta meta) {
        if (meta.isBaseWorld) return false;
        World world = Bukkit.getWorld(meta.worldID);
        World baseWorld = Bukkit.getWorlds().get(0);

        // Evacuate any players
        List<Player> players = world.getPlayers();
        for (Player player : players) {
            player.teleport(baseWorld.getSpawnLocation());
        }

        // Unload world
        boolean unloaded = Bukkit.unloadWorld(meta.worldID, true);

        // Update status
        if (unloaded) meta.status = WorldMeta.Status.UNLOADED;

        return unloaded;
    }



    public boolean loadWorld(WorldMeta meta) {
        WorldCreator creator = new WorldCreator(meta.worldID).environment(meta.environment).type(meta.type).generateStructures(meta.generateStructures);

        if (meta.seed != 0) {
            creator.seed(meta.seed);
        }

        World world = Bukkit.createWorld(creator);
        if (world == null) {
            return false;
        }

        // Apply policies
        applyWorldPolicies(world, meta);
        meta.status = WorldMeta.Status.LOADED;

        // Save policies
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            saveWorlds(plugin);
        });

        return true;
    }

    public void applyWorldPolicies(World world, WorldMeta meta) {
        world.setHardcore(meta.doHardcore);
        world.setDifficulty(meta.difficulty);
        Location loc = new Location(world, meta.spawnLocX, meta.spawnLocY, meta.spawnLocZ, (float) meta.spawnLocYaw, (float) meta.spawnLocPitch);
        world.setSpawnLocation(loc);
    }

    public void teleportPlayerToWorld(Player player, WorldMeta meta) {
        if (meta.status == WorldMeta.Status.LOADED) {
            World world = getWorldByMeta(meta);

            if (player.getWorld().equals(world)) {
                player.sendMessage(ChatColor.RED + "You are already in that world!");
                return;
            }

            if (world == null) {
                player.sendMessage(ChatColor.RED + "Somehow, the world is loaded but could not be retrieved. This is a big error.");
            } else {
                player.teleport(world.getSpawnLocation());
                player.sendMessage(ChatColor.GOLD + "Teleported to world '" + meta.worldName + "'.");
            }
        } else {
            // Attempt to load world
            boolean didWorldLoad = loadWorld(meta);
            if (didWorldLoad) {
                World world = getWorldByMeta(meta);

                if (player.getWorld().equals(world)) {
                    player.sendMessage(ChatColor.RED + "You are already in that world!");
                    return;
                }

                if (world == null) {
                    player.sendMessage(ChatColor.RED + "Somehow, the world is loaded but could not be retrieved. This is a big error.");
                } else {
                    player.teleport(world.getSpawnLocation());
                    player.sendMessage(ChatColor.GOLD + "Teleported to world '" + meta.worldName + "'.");
                }
            } else {
                player.sendMessage(ChatColor.RED + "That world could not be loaded!");
            }
        }
    }

    public boolean doesWorldFileExist(String worldName) {
        // Get the folder where all worlds live
        File worldFolder = new File(Bukkit.getServer().getWorldContainer(), worldName);
        return worldFolder.exists() && worldFolder.isDirectory();
    }
}
