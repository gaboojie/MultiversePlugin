package org.gabooj.worlds;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.gabooj.scope.ScopeMeta;
import org.gabooj.scope.SpawnLocation;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class WorldMeta {

    public enum Status {
        LOADED, UNLOADED
    }

    public enum GeneratorType {
        VANILLA, VOID
    }

    // Nullable data
    private World world;

    // Immutable data
    private final boolean isBaseWorld;
    private final String worldID;
    private final World.Environment environment;
    private final WorldType worldType;
    private final long seed;
    private final boolean generateStructures;

    // Mutable data
    private String scopeID;
    private boolean doAutoLoad = false;
    private GeneratorType generatorType;

    public WorldMeta(boolean isBaseWorld, String worldID, World.Environment environment, WorldType worldType, long seed, boolean generateStructures) {
        this.isBaseWorld = isBaseWorld;
        this.worldID = worldID;
        this.scopeID = null;

        this.environment = Objects.requireNonNullElse(environment, World.Environment.NORMAL);
        this.worldType = Objects.requireNonNullElse(worldType, WorldType.NORMAL);
        this.generatorType = GeneratorType.VANILLA;
        this.seed = seed;
        this.generateStructures = generateStructures;

        this.world = null;
    }

    public String getScopeID() {
        return scopeID;
    }

    public void setScopeID(String scopeID) {
        this.scopeID = scopeID;
    }

    public World getWorld() {
        return world;
    }

    public boolean isLoaded() {
        return this.world != null;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public boolean isBaseWorld() {
        return isBaseWorld;
    }

    public String getWorldID() {
        return worldID;
    }

    public World.Environment getEnvironment() {
        return environment;
    }

    public WorldType getWorldType() {
        return worldType;
    }

    public long getSeed() {
        return seed;
    }

    public boolean isGenerateStructures() {
        return generateStructures;
    }

    public boolean doAutoLoad() {
        return doAutoLoad;
    }

    public void setDoAutoLoad(boolean doAutoLoad) {
        this.doAutoLoad = doAutoLoad;
    }

    public GeneratorType getGeneratorType() {
        return generatorType;
    }

    public void setGeneratorType(GeneratorType generatorType) {
        this.generatorType = generatorType;
    }

    public String toString(WorldManager manager) {
        String worldInfo = "Details of World '" + this.worldID + "':\n";
        worldInfo += "===\n";
        worldInfo += "World Data:\n";
        worldInfo += "- World Loaded: " + this.isLoaded() + "\n";
        worldInfo += "- Base World: " + this.isBaseWorld + "\n";
        worldInfo += "- Load world on start-up: " + this.doAutoLoad + "\n";
        worldInfo += "===\n";
        ScopeMeta scopeMeta = manager.scopeManager.getScopeForWorldMeta(this);
        SpawnLocation loc = scopeMeta.getSpawnLocation();
        worldInfo += "Group Data:\n";
        worldInfo += "- Scope ID: " + scopeMeta.getScopeId() + "\n";
        worldInfo += "- Scope Name: " + scopeMeta.getName() + "\n";
        worldInfo += "- Visibility: " + scopeMeta.isVisible() + "\n";
        worldInfo += "- Do Force Spawn: " + loc.doForceSpawn + "\n";

        // Handle null spawnWorldID
        if (loc.spawnWorldID == null) {
            worldInfo += "- Spawn Location: None\n";
        } else {
            worldInfo += "- Spawn Location: (" + loc.spawnX + ", " + loc.spawnY + ", " + loc.spawnZ + ") | Yaw: " + loc.spawnYaw + " Pitch: " + loc.spawnPitch + " in world: " + loc.spawnWorldID + "\n";
        }

        worldInfo += "- GameMode: " + scopeMeta.getGameMode() + "\n";
        worldInfo += "- Difficulty: " + scopeMeta.getDifficulty() + "\n";
        worldInfo += "- Do hardcore: " + scopeMeta.doHardcore() + "\n";
        worldInfo += "===\n";
        worldInfo += "Immutable Data:\n";
        worldInfo += "- World Type: " + this.worldType + "\n";
        worldInfo += "- Environment: " + this.environment + "\n";
        worldInfo += "- Generator Type: " + this.generatorType + "\n";
        worldInfo += "- Seed: " + this.seed + "\n";
        worldInfo += "- Generate Structures: " + this.generateStructures + "\n";
        worldInfo += "===";
        return worldInfo;
    }
}