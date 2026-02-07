package org.gabooj.scope;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.players.PlayerInventorySerializer;
import org.gabooj.players.PlayerLocationSerializer;
import org.gabooj.players.PlayerMiscSerializer;
import org.gabooj.utils.Messager;
import org.gabooj.worlds.WorldManager;
import org.gabooj.worlds.WorldMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ScopeManager {

    private final JavaPlugin plugin;
    private final Map<String, ScopeMeta> scopes = new HashMap<>();
    private final WorldManager worldManager;
    private final ScopeListener scopeListener;

    public String defaultScopeID = null;
    public boolean forceDefaultScope = false;

    public ScopeManager(JavaPlugin plugin, WorldManager worldManager) {
        this.plugin = plugin;
        this.worldManager = worldManager;
        this.scopeListener = new ScopeListener(worldManager, plugin);
        this.scopeListener.onEnable();
    }

    public boolean doesScopeExist(String scopeID) {
        for (ScopeMeta meta : scopes.values()) {
            if (meta.getScopeId().equalsIgnoreCase(scopeID)) return true;
        }
        return false;
    }

    public void removeScope(ScopeMeta meta, boolean createNewScopeForUngroupedWorlds) {
        // Remove default scope
        if (meta.getScopeId().equals(defaultScopeID)) {
            defaultScopeID = null;
            calculateNewDefaultSpawn();
            Messager.broadcastWarningMessage(plugin.getServer(), "Reset default spawn to '" + defaultScopeID  + "'.");
        }

        // Remove from map
        scopes.remove(meta.getScopeId());

        // Remove from any world meta
        for (WorldMeta worldMeta : worldManager.worldMetas.values()) {
            if (worldMeta.getScopeID() != null && worldMeta.getScopeID().equals(meta.getScopeId())) {
                worldMeta.setScopeID(null);

                // Make new scope for the world meta (as it was just deleted)
                if (createNewScopeForUngroupedWorlds) createScope(worldMeta.getWorldID());
            }
        }

        // Save scope data and world metadata
        saveScopes();
        worldManager.saveWorldMetaDatas();
    }

    private void calculateNewDefaultSpawn() {
        for (ScopeMeta scopeMeta : scopes.values()) {
            WorldMeta normalWorld = scopeMeta.getWorldMetaByEnvironment(World.Environment.NORMAL);
            if (normalWorld != null && normalWorld.isBaseWorld()) {
                defaultScopeID = scopeMeta.getScopeId();
                if (scopeMeta.getSpawnLocation().spawnWorldID == null) {
                    scopeMeta.getSpawnLocation().spawnWorldID = normalWorld.getWorldID();
                }
                return;
            }
        }
    }

    public ScopeMeta getScopeForWorld(World world) {
        WorldMeta worldMeta = worldManager.worldMetas.get(world.getName());
        return getScopeForWorldMeta(worldMeta);
    }

    public Map<String, ScopeMeta> getScopes() {
        return this.scopes;
    }

    public ScopeMeta getDefaultScope() {
        if (defaultScopeID == null) return null;
        return getScopeByID(defaultScopeID);
    }

    public void applyScopeToPlayer(Player player, ScopeMeta targetScope) {
        // Load player state in new scope
        loadPlayerStateInNewScope(player, targetScope);

        // Enforce scope policies
        player.setGameMode(targetScope.getGameMode());
    }

    public void savePlayerStateInScope(Player player, Location loc, ScopeMeta scopeMeta) {
        // Get last player location in scope (if DNE, do not save and tell server)
        PlayerLocationSerializer.savePlayerLocInScope(player, loc, scopeMeta, plugin);
        PlayerLocationSerializer.setPlayerLastScope(player, scopeMeta);
        PlayerInventorySerializer.saveInventory(player, scopeMeta, plugin);
        PlayerMiscSerializer.savePlayerState(player, scopeMeta, plugin);
    }

    public void savePlayerLastScope(Player player, ScopeMeta scopeMeta) {
        PlayerLocationSerializer.setPlayerLastScope(player, scopeMeta);
    }

    private void loadPlayerStateInNewScope(Player player, ScopeMeta targetScopeMeta) {
        PlayerInventorySerializer.loadInventory(player, targetScopeMeta, plugin);
        PlayerMiscSerializer.loadPlayerState(player, targetScopeMeta, plugin);
    }

    public Location getPlayerBedSpawnInScope(ScopeMeta meta, Player player) {
        // Ensure that world ID exists in player bed spawn
        String worldID = PlayerMiscSerializer.getPlayerBedWorldInScope(player, meta, plugin);
        if (worldID == null) return null;

        // Ensure that world is loaded
        WorldMeta worldMeta = worldManager.getWorldMetaByID(worldID);
        if (worldMeta == null || worldMeta.isUnloading) return null;
        if (!worldMeta.isLoaded()) {
            boolean didLoad = worldManager.loadWorldFromMetaData(worldMeta);
            if (!didLoad) return null;
        }
        Location loc = PlayerMiscSerializer.getPlayerBedLocInScope(player, meta, plugin);

        // Ensure that bed still exists
        if (!Tag.BEDS.isTagged(loc.getBlock().getType())) {
            Messager.sendWarningMessage(player, "Your bed in '" + meta.getName() + "' was destroyed!");
            PlayerMiscSerializer.clearPlayerBedLocInScope(player, meta, plugin);
            return null;
        }

        // Get safe new bed location
        return new Location(loc.getWorld(), loc.getBlockX()+0.5, loc.getBlockY()+1, loc.getBlockZ()+0.5);
    }

    public Location getPlayerSpawnLocationOfScope(ScopeMeta meta, Player player) {
        // Teleport to bed spawn if it exists
        Location bedSpawnLoc = getPlayerBedSpawnInScope(meta, player);
        if (bedSpawnLoc != null) {
            return bedSpawnLoc;
        }

        // Enforce player respawning in scope spawn location
        return getSpawnLocationOfScope(meta);
    }

    public Location getSpawnLocationOfScope(ScopeMeta meta) {
        SpawnLocation spawnLocation = meta.getSpawnLocation();
        if (spawnLocation.spawnWorldID == null) return null;
        WorldMeta worldMeta = worldManager.getWorldMetaByID(spawnLocation.spawnWorldID);
        if (!worldMeta.isLoaded() || worldMeta.isUnloading) return null;
        return new Location(worldMeta.getWorld(), spawnLocation.spawnX, spawnLocation.spawnY, spawnLocation.spawnZ, spawnLocation.spawnYaw, spawnLocation.spawnPitch);
    }

    public ScopeMeta getCurrentPlayerScope(Player player) {
        return getScopeForWorld(player.getWorld());
    }

    public ScopeMeta getScopeForWorldMeta(WorldMeta worldMeta) {
        String scopeId = resolveScopeId(worldMeta);
        return getScopeByID(scopeId);
    }

    private String resolveScopeId(WorldMeta worldMeta) {
        if (worldMeta.getScopeID() != null) {
            return worldMeta.getScopeID(); // explicit (group)
        }
        return worldMeta.getWorldID(); // implicit (ungrouped)
    }

    public List<String> getAllGroupNames() {
        List<String> groupNames = new ArrayList<>();
        for (ScopeMeta scopeMeta : scopes.values()) {
            groupNames.add(scopeMeta.getName());
        }
        return groupNames;
    }

    public List<String> getAllVisibleGroupNames() {
        List<String> visibleGroupNames = new ArrayList<>();
        for (ScopeMeta scopeMeta : scopes.values()) {
            if (scopeMeta.isVisible()) visibleGroupNames.add(scopeMeta.getName());
        }
        return visibleGroupNames;
    }

    public ScopeMeta getScopeByName(String scopeName) {
        for (ScopeMeta scopeMeta : scopes.values()) {
            if (scopeMeta.getName().equalsIgnoreCase(scopeName)) return scopeMeta;
        }
        return null;
    }

    public boolean doesScopeNameExist(String scopeName) {
        for (ScopeMeta scopeMeta : scopes.values()) {
            if (scopeMeta.getName().equalsIgnoreCase(scopeName)) return true;
        }
        return false;
    }

    public ScopeMeta getScopeByID(String scopeID) {
        return scopes.get(scopeID);
    }

    public ScopeMeta createScope(String scopeID) {
        ScopeMeta newScope = new ScopeMeta(scopeID);
        // Default scope values
        newScope.setDifficulty(Difficulty.NORMAL);
        newScope.setDoHardcore(false);
        newScope.setGameMode(GameMode.CREATIVE);
        // If world manager contains a world meta with this scope, update its values
        WorldMeta worldMeta = worldManager.getWorldMetaByID(scopeID);
        if (worldMeta == null) {
            // Making new scope with no world data attached
            newScope.setSpawnLocation(new SpawnLocation(true, 0, 100, 0, 0, 0, null));
        } else {
            // Making scope with world data attached

            // Update scope <=> world tracking
            newScope.addWorld(worldMeta);
            worldMeta.setScopeID(newScope.getScopeId());

            // Add spawn location
            if (worldMeta.isLoaded()) {
                Location loc = worldMeta.getWorld().getSpawnLocation();
                SpawnLocation spawnLoc = new SpawnLocation(true, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(), worldMeta.getWorldID());
                newScope.setSpawnLocation(spawnLoc);
            } else {
                // If world is not loaded, default to random values
                newScope.setSpawnLocation(new SpawnLocation(true, 0, 100, 0, 0, 0, scopeID));
            }
            worldManager.saveWorldMetaDatas();
        }
        scopes.put(scopeID, newScope);
        worldManager.scopeManager.saveScopes();
        return newScope;
    }

    public void loadScopes() {
        FileConfiguration cfg = loadYaml(this.plugin);
        ConfigurationSection scopesSec = cfg.getConfigurationSection("scopes");
        if (scopesSec == null) return;

        for (String scopeId : scopesSec.getKeys(false)) {
            ConfigurationSection sec = scopesSec.getConfigurationSection(scopeId);
            if (sec == null) continue;

            ScopeMeta scope = new ScopeMeta(scopeId);

            // Gamemode
            if (sec.contains("gamemode")) {
                scope.setGameMode(GameMode.valueOf(sec.getString("gamemode")));
            }

            // Visible
            scope.setVisible(sec.getBoolean("visible", true));

            // Name
            scope.setName(sec.getString("name", scopeId));

            // Hardcore
            scope.setDoHardcore(sec.getBoolean("hardcore", false));

            // Difficulty
            if (sec.contains("difficulty")) {
                scope.setDifficulty(Difficulty.valueOf(sec.getString("difficulty")));
            }

            // Spawn
            ConfigurationSection config = sec.getConfigurationSection("spawn");
            if (sec.contains("spawn") && config != null) {
                scope.setSpawnLocation(SpawnLocation.readSpawn(config));
            }
            scopes.put(scopeId, scope);

            // Warps
            ConfigurationSection warpsSec = sec.getConfigurationSection("warps");
            if (warpsSec != null) {
                for (String warpName : warpsSec.getKeys(false)) {
                    ConfigurationSection w = warpsSec.getConfigurationSection(warpName);
                    if (w == null) continue;

                    String worldId = w.getString("world");
                    if (worldId == null) continue; // skip invalid

                    Warp warp = new Warp(warpName,
                            w.getDouble("x"),
                            w.getDouble("y"),
                            w.getDouble("z"),
                            (float) w.getDouble("yaw"),
                            (float) w.getDouble("pitch"),
                            worldId
                    );

                    scope.getWarps().add(warp);
                }
            }
        }
    }

    public void loadWorldMetasToScopes() {
        for (WorldMeta worldMeta : worldManager.worldMetas.values()) {
            ScopeMeta scope = getScopeForWorldMeta(worldMeta);
            scope.addWorld(worldMeta);
        }
    }

    public void saveScopes() {
        FileConfiguration cfg = new YamlConfiguration();

        for (ScopeMeta scope : scopes.values()) {
            String path = "scopes." + scope.getScopeId();

            cfg.set(path + ".visible", scope.isVisible());
            cfg.set(path + ".name", scope.getName());

            cfg.set(path + ".gamemode",
                    scope.getGameMode() != null ? scope.getGameMode().name() : null);

            cfg.set(path + ".hardcore", scope.doHardcore());

            cfg.set(path + ".difficulty",
                    scope.getDifficulty() != null ? scope.getDifficulty().name() : null);

            // Save spawn
            if (scope.getSpawnLocation() != null) {
                SpawnLocation.writeSpawn(cfg.createSection(path + ".spawn"), scope.getSpawnLocation());
            } else {
                cfg.set(path + ".spawn", null);
            }

            // Save warps
            if (!scope.getWarps().isEmpty()) {
                cfg.createSection(path + ".warps");

                for (Warp warp : scope.getWarps()) {
                    String wPath = path + ".warps." + warp.name;

                    cfg.set(wPath + ".world", warp.worldID);
                    cfg.set(wPath + ".x", warp.x);
                    cfg.set(wPath + ".y", warp.y);
                    cfg.set(wPath + ".z", warp.z);
                    cfg.set(wPath + ".yaw", warp.yaw);
                    cfg.set(wPath + ".pitch", warp.pitch);
                }
            } else {
                cfg.set(path + ".warps", null);
            }
        }

        saveYaml(plugin, cfg);
    }

    private static FileConfiguration loadYaml(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "scopes.yml");

        if (!file.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException("Could not create scopes.yml file!", e);
            }
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    private static void saveYaml(JavaPlugin plugin, FileConfiguration config) {
        File file = new File(plugin.getDataFolder(), "scopes.yml");

        try {
            config.save(file);
        } catch (IOException e) {
            throw new RuntimeException("Could not save scopes.yml file!", e);
        }
    }

    public boolean doPlayerScopesMatch(Player player1, Player player2) {
        WorldMeta meta1 = worldManager.getWorldMetaByID(player1.getWorld().getName());
        WorldMeta meta2 = worldManager.getWorldMetaByID(player2.getWorld().getName());

        return meta1.getScopeID().equals(meta2.getScopeID());
    }

}
