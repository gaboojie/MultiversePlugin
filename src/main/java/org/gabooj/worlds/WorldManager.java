package org.gabooj.worlds;

import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.gabooj.players.PlayerSerializer;
import org.gabooj.players.PlayerTabManager;
import org.gabooj.players.afk.AfkManager;
import org.gabooj.scope.ScopeManager;
import org.gabooj.scope.ScopeMeta;
import org.gabooj.utils.Messager;
import org.gabooj.worlds.presets.VoidChunkGenerator;

import net.kyori.adventure.text.format.NamedTextColor;

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

        // Initialize player tab manager with this world manager
        PlayerTabManager.worldManager = this;
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

        // Load afk information
        AfkManager.registerPollingTask(plugin);

        // Register world offloading
        registerUnloadWorldScheduler();
    }

    public void onDisable() {
        scopeManager.saveScopes();
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
            @SuppressWarnings("deprecation")
            WorldMeta meta = new WorldMeta(
               true, world.getName(), world.getEnvironment(), world.getWorldType(), world.getSeed(), world.canGenerateStructures()
            );

            // Guess other information
            meta.setDoAutoLoad(true);

            // Set world
            meta.setWorld(world);
            worldMetas.put(world.getName(), meta);
            scopeManager.createScope(world.getName());
            Messager.broadcastSuccessMessage(plugin.getServer(), "Auto registered base world: " + world.getName());
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
                Messager.broadcastWarningMessage(plugin.getServer(), "Could not load world that should be auto-loaded: " + meta.getWorldID());
            }
        }
    }

    public void unloadWorld(WorldMeta meta, Runnable onSuccess, Runnable onFailure) {
        // Ensure that world can be unloaded
        if (meta.isBaseWorld() || !meta.isLoaded() || !meta.getWorld().getPlayers().isEmpty()) {
            onFailure.run();
            return;
        }

        attemptWorldUnload(meta, onSuccess);
    }

    private void attemptWorldUnload(WorldMeta meta, Runnable onSuccess) {
        meta.isUnloading = true;
        new BukkitRunnable() {

            int postUnloadBuffer = -1;

            @Override
            public void run() {

                // Phase 1 — Wait until safe tick phase
                if (Bukkit.isTickingWorlds()) return;

                // Phase 2 — Try unload
                if (postUnloadBuffer == -1) {

                    World world = Bukkit.getWorld(meta.getWorldID());

                    if (world != null) {
                        boolean success = Bukkit.unloadWorld(world, true);
                        if (!success) return;
                        return;
                    }

                    // World is gone → start buffer
                    postUnloadBuffer = 60; // 60 ticks safe buffer
                    return;
                }

                // Phase 3 — Safety buffer ticks
                postUnloadBuffer--;

                if (postUnloadBuffer <= 0) {
                    meta.setWorld(null); // NOW safe
                    meta.isUnloading = false;
                    cancel();
                    onSuccess.run();
                }
            }

        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void registerUnloadWorldScheduler() {
        long UNLOAD_TIME_TICKS = 20L * 60;
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (WorldMeta meta : worldMetas.values()) {
                // Skip unloading any worlds that should not be unloaded
                if (!meta.isLoaded() || meta.isBaseWorld() || meta.doAutoLoad() || meta.isUnloading) continue;

                World world = Bukkit.getWorld(meta.getWorldID());
                if (world == null) continue;
                if (!world.getPlayers().isEmpty()) continue;

                // Unload worlds when possible
                Runnable onSuccess = () -> {
                    Messager.broadcastMessage(plugin.getServer(), "Unloaded world: " + world.getName(), NamedTextColor.GRAY);
                };
                Runnable onFailure = () -> {
                    Messager.broadcastWarningMessage(plugin.getServer(), "Could not unload world: " + world.getName());
                };
                Messager.broadcastMessage(plugin.getServer(), "Unloading empty world: " + world.getName(), NamedTextColor.GRAY);
                unloadWorld(meta, onSuccess, onFailure);
            }
        }, UNLOAD_TIME_TICKS, UNLOAD_TIME_TICKS);
    }

    public boolean isInvalidName(String name) {
        return doesWorldIDExist(name) || scopeManager.doesScopeExist(name) || scopeManager.doesScopeNameExist(name);
    }

    public boolean loadWorldFromMetaData(WorldMeta meta) {
        if (meta.isUnloading) return false;

        WorldCreator creator = new WorldCreator(meta.getWorldID()).environment(meta.getEnvironment());

        if (meta.getSeed() != 0) {
            creator.seed(meta.getSeed());
        }

        if (meta.getGeneratorType() == WorldMeta.GeneratorType.VOID) {
            creator.generator(new VoidChunkGenerator(meta.isGenerateStructures())).generateStructures(meta.isGenerateStructures());
        } else {
            // Generate using default settings
            creator.type(meta.getWorldType()).generateStructures(meta.isGenerateStructures());
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
