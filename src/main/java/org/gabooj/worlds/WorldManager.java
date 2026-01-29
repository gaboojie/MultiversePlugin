package org.gabooj.worlds;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.players.PlayerSerializer;
import org.gabooj.players.PlayerTabManager;
import org.gabooj.scope.ScopeManager;
import org.gabooj.scope.ScopeMeta;

import java.io.File;
import java.util.*;

public class WorldManager {

    private final JavaPlugin plugin;
    public Map<String, WorldMeta> worldMetas;
    public ScopeManager scopeManager;
    public WorldTeleporter worldTeleporter;

    public WorldManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.worldMetas = new HashMap<>();
        this.scopeManager = new ScopeManager(plugin, this);
        this.worldTeleporter = new WorldTeleporter(this, plugin);
    }

    public void onEnable() {
        // Load serialization keys
        PlayerSerializer.init(plugin);

        // Load worlds.yml
        WorldSerialization.loadWorlds(plugin, this);

        // Load scopes.yml
        scopeManager.loadScopes();

        // Update worlds list for all scopes
        scopeManager.loadWorldMetasToScopes();

        // Load any unregistered base worlds
        loadUnregisteredBaseWorlds();
        // Load autoloaded worlds
        loadAutoLoadedWorlds();

        // Load tab information
        PlayerTabManager.initScoreboard();
    }

    public void onDisable() {
        scopeManager.saveScopes();
        PlayerTabManager.restoreMainScoreboard();
    }

    public void saveWorldMetaDatas() {
        // Async task (do not access world data)
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            WorldSerialization.saveWorlds(this.worldMetas, this, plugin);
        });
    }

    private void loadUnregisteredBaseWorlds() {
        boolean shouldSaveNewBaseWorld = false;
        for (World world : Bukkit.getWorlds()) {
            if (scopeManager.defaultScopeID == null) {
                scopeManager.defaultScopeID = world.getName();
            }

            // Skip any base world already cached
            if (worldMetas.containsKey(world.getName())) {
                continue;
            }
            shouldSaveNewBaseWorld = true;

            // Load mandatory information
            WorldMeta meta = new WorldMeta(
               true, world.getName(), world.getEnvironment(), world.getWorldType(), world.getSeed(), world.canGenerateStructures()
            );

            // Guess other information
            meta.setDoAutoLoad(true);

            // Set world
            meta.setWorld(world);
            worldMetas.put(world.getName(), meta);
            scopeManager.createScope(world.getName());
            plugin.getServer().broadcastMessage("Auto registered base world: " + world.getName());
        }

        if (shouldSaveNewBaseWorld) {
            saveWorldMetaDatas();
        }
    }

    public WorldMeta getDefaultScopeWorld() {
        ScopeMeta scopeMeta = scopeManager.getDefaultScope();
        if (scopeMeta == null || scopeMeta.getSpawnLocation().spawnWorldID == null) return null;
        return getWorldMetaByID(scopeMeta.getSpawnLocation().spawnWorldID);
    }

    private void loadAutoLoadedWorlds() {
        for (WorldMeta meta : worldMetas.values()) {
            // Ignore base worlds (always loaded)
            if (meta.isBaseWorld()) {
                continue;
            }

            // Ignore world's with autoload set to false
            if (!meta.doAutoLoad()) {
                continue;
            }

            // Load world
            boolean didLoad = loadWorldFromMetaData(meta);
            if (!didLoad) {
                plugin.getServer().broadcastMessage(ChatColor.RED + "Could not load world that should be auto-loaded: " + meta.getWorldID());
            }
        }
    }

    public boolean unloadWorld(WorldMeta meta) {
        // Ensure that world can be unloaded
        if (meta.isBaseWorld()) return false;
        if (!meta.isLoaded()) return false;

        // Unload world from memory
        meta.setWorld(null);

        // Evacuate any players
        World world = Bukkit.getWorld(meta.getWorldID());
        World baseWorld = Bukkit.getWorlds().get(0);
        List<Player> players = world.getPlayers();
        for (Player player : players) {
            player.teleport(baseWorld.getSpawnLocation());
        }


        // Unload world
        return Bukkit.unloadWorld(meta.getWorldID(), true);
    }

    public boolean isInvalidName(String name) {
        return doesWorldIDExist(name) || scopeManager.doesScopeExist(name) || scopeManager.doesScopeNameExist(name);
    }

    public boolean loadWorldFromMetaData(WorldMeta meta) {
        WorldCreator creator = new WorldCreator(meta.getWorldID()).environment(meta.getEnvironment()).type(meta.getWorldType()).generateStructures(meta.isGenerateStructures());

        if (meta.getSeed() != 0) {
            creator.seed(meta.getSeed());
        }

        World world = Bukkit.createWorld(creator);
        meta.setWorld(world);
        return world != null;
    }

    public boolean doesWorldIDExist(String worldID) {
        for (WorldMeta meta : worldMetas.values()) {
            if (meta.getWorldID().equalsIgnoreCase(worldID)) {
                return true;
            }
        }
        return false;
    }

    public WorldMeta getWorldMetaByID(String name) {
        for (WorldMeta meta : worldMetas.values()) {
            if (meta.getWorldID().equalsIgnoreCase(name)) {
                return meta;
            }
        }
        return null;
    }

    public List<String> getWorldIDs() {
        List<String> worldNames = new ArrayList<>();
        for (WorldMeta meta : worldMetas.values()) {
            worldNames.add(meta.getWorldID());
        }
        return worldNames;
    }

    public boolean doesWorldFileExist(String worldName) {
        // Get the folder where all worlds live
        File worldFolder = new File(Bukkit.getServer().getWorldContainer(), worldName);
        return worldFolder.exists() && worldFolder.isDirectory();
    }
}
