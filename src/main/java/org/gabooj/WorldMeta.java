package org.gabooj;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class WorldMeta {

    public enum Status {
        REGISTERED, LOADED, UNLOADED
    }

    public enum GeneratorType {
        VANILLA, VOID
    }

    public final boolean isBaseWorld;
    public final String worldID;

    public String worldName = "";
    public World.Environment environment = World.Environment.NORMAL;
    public WorldType type = WorldType.NORMAL;
    public GeneratorType generator = GeneratorType.VANILLA;
    public long seed = 0;
    public boolean generateStructures = true;
    public boolean visible = true;

    public boolean autoLoad = false, doHardcore = false, forceSpawn = true;
    public GameMode gameMode = GameMode.CREATIVE;
    public Difficulty difficulty = Difficulty.NORMAL;
    public double spawnLocX = 0, spawnLocY = 80, spawnLocZ = 0, spawnLocYaw = 0, spawnLocPitch = 0;

    public Status status = Status.REGISTERED;

    public WorldMeta(boolean isBaseWorld, String worldID) {
        this.isBaseWorld = isBaseWorld;
        this.worldID = worldID;
        this.worldName = worldID;
    }

    public static void saveWorlds(File file, Collection<WorldMeta> worlds) throws IOException {
        YamlConfiguration config = new YamlConfiguration();
        config.set("schema-version", 1);

        for (WorldMeta world : worlds) {
            String path = "worlds." + world.worldID;

            config.set(path + ".is-base-world", world.isBaseWorld);
            config.set(path + ".world-id", world.worldID);
            config.set(path + ".world-name", world.worldName);
            config.set(path + ".auto-load", world.autoLoad);
            config.set(path + ".visible", world.visible);

            config.set(path + ".environment", world.environment.name());
            config.set(path + ".world-type", world.type.name());
            config.set(path + ".generator", world.generator.name());
            config.set(path + ".seed", world.seed);
            config.set(path + ".generate-structures", world.generateStructures);

            config.set(path + ".gamemode", world.gameMode.name());
            config.set(path + ".difficulty", world.difficulty.name());
            config.set(path + ".hardcore", world.doHardcore);
            config.set(path + ".force-spawn", world.forceSpawn);

            config.set(path + ".spawn.x", world.spawnLocX);
            config.set(path + ".spawn.y", world.spawnLocY);
            config.set(path + ".spawn.z", world.spawnLocZ);
            config.set(path + ".spawn.yaw", world.spawnLocYaw);
            config.set(path + ".spawn.pitch", world.spawnLocPitch);
        }

        config.save(file);
    }

    public static Map<String, WorldMeta> loadWorlds(File file) {
        Map<String, WorldMeta> worlds = new HashMap<>();

        if (!file.exists()) return worlds;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("worlds");
        if (section == null) return worlds;

        for (String worldID : section.getKeys(false)) {
            String path = "worlds." + worldID;

            WorldMeta world = new WorldMeta(
                    config.getBoolean(path + ".is-base-world"),
                    worldID
            );

            world.worldName = config.getString(path + ".world-name");
            world.environment = World.Environment.valueOf(config.getString(path + ".environment"));
            world.type = WorldType.valueOf(config.getString(path + ".world-type"));
            world.generator = GeneratorType.valueOf(config.getString(path + ".generator"));
            world.seed = config.getLong(path + ".seed");
            world.generateStructures = config.getBoolean(path + ".generate-structures");

            world.autoLoad = config.getBoolean(path + ".auto-load", false);
            world.visible = config.getBoolean(path + ".visible", true);
            world.gameMode = GameMode.valueOf(config.getString(path + ".gamemode", "SURVIVAL"));
            world.difficulty = Difficulty.valueOf(config.getString(path + ".difficulty", "NORMAL"));
            world.doHardcore = config.getBoolean(path + ".hardcore", false);
            world.forceSpawn = config.getBoolean(path + ".force-spawn", false);

            world.spawnLocX = config.getDouble(path + ".spawn.x");
            world.spawnLocY = config.getDouble(path + ".spawn.y");
            world.spawnLocZ = config.getDouble(path + ".spawn.z");
            world.spawnLocYaw = config.getDouble(path + ".spawn.yaw");
            world.spawnLocPitch = config.getDouble(path + ".spawn.pitch");

            if (world.isBaseWorld) {
                world.status = Status.LOADED;
            } else {
                world.status = Status.UNLOADED;
            }

            worlds.put(worldID, world);
        }

        return worlds;
    }

    @Override
    public String toString() {
        String worldInfo = "Details of World '" + worldName + "':\n";
        worldInfo += "===\n";
        worldInfo += "Important Data:\n";
        worldInfo += "- World ID: " + this.worldID + "\n";
        worldInfo += "- World Status: " + this.status + "\n";
        worldInfo += "- Base World: " + this.isBaseWorld + "\n";
        worldInfo += "- Visible: " + this.visible + "\n";
        worldInfo += "===\n";
        worldInfo += "Immutable Data:\n";
        worldInfo += "- World Type: " + this.type + "\n";
        worldInfo += "- Environment: " + this.environment + "\n";
        worldInfo += "- Generator Type: " + this.generator + "\n";
        worldInfo += "- Seed: " + this.seed + "\n";
        worldInfo += "- Generate Structures: " + this.generateStructures + "\n";
        worldInfo += "===\n";
        worldInfo += "Mutable Data:\n";
        worldInfo += "- Load world on start-up: " + this.autoLoad + "\n";
        worldInfo += "- Hardcore: " + this.doHardcore + "\n";
        worldInfo += "- Default to spawn: " + this.forceSpawn + "\n";
        worldInfo += "- GameMode: " + this.gameMode + "\n";
        worldInfo += "- Difficulty: " + this.difficulty + "\n";
        worldInfo += "- Spawn Location: (" + this.spawnLocX + ", " + this.spawnLocY + ", " + this.spawnLocZ + ") | Yaw: " + this.spawnLocYaw + " Pitch: " + this.spawnLocPitch + "\n";
        worldInfo += "===";
        return worldInfo;
    }
}