package org.gabooj.worlds;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.utils.Messager;

import net.kyori.adventure.text.format.NamedTextColor;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class WorldSerialization {

    public static void loadWorlds(JavaPlugin plugin, WorldManager worldManager) {
        Map<String, WorldMeta> worlds = new HashMap<>();
        File file = new File(plugin.getDataFolder(), "worlds.yml");
        if (!file.exists()) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        // ---- GLOBAL CONFIG ----
        worldManager.scopeManager.defaultScopeID = config.getString("defaultScope", null);
        worldManager.scopeManager.forceDefaultScope = config.getBoolean("forceDefaultScope", false);

        ConfigurationSection worldsSection = config.getConfigurationSection("worlds");
        if (worldsSection == null) {
            return;
        }

        // ---- PER-WORLD CONFIG ----
        for (String worldID : worldsSection.getKeys(false)) {
            World.Environment env = World.Environment.valueOf(
                    worldsSection.getString(worldID + ".environment")
            );
            WorldType type = WorldType.valueOf(
                    worldsSection.getString(worldID + ".worldType")
            );
            long seed = worldsSection.getLong(worldID + ".seed");
            boolean generateStructures = worldsSection.getBoolean(worldID + ".generateStructures");
            boolean isBaseWorld = worldsSection.getBoolean(worldID + ".isBaseWorld");

            WorldMeta meta = new WorldMeta(
                    isBaseWorld, worldID, env, type, seed, generateStructures
            );

            meta.setScopeID(worldsSection.getString(worldID + ".scope", null));
            meta.setDoAutoLoad(worldsSection.getBoolean(worldID + ".doAutoLoad", false));
            meta.setGeneratorType(
                    WorldMeta.GeneratorType.valueOf(
                            worldsSection.getString(worldID + ".generatorType", "DEFAULT")
                    )
            );

            if (isBaseWorld) {
                World world = Bukkit.getWorld(worldID);
                meta.setWorld(world);
            }

            worlds.put(worldID, meta);
        }

        // Update worlds
        worldManager.worldMetas = worlds;
    }

    public static void saveWorlds(Map<String, WorldMeta> worlds, WorldManager worldManager, JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "worlds.yml");
        FileConfiguration config = new YamlConfiguration();

        // ---- Save global default ----
        config.set("defaultScope", worldManager.scopeManager.defaultScopeID);
        config.set("forceDefaultScope", worldManager.scopeManager.forceDefaultScope);

        // ---- Save per-world data under 'worlds:' section ----
        for (WorldMeta meta : worlds.values()) {
            String worldID = meta.getWorldID();
            String basePath = "worlds." + worldID + ".";

            // Immutable fields
            config.set(basePath + "environment", meta.getEnvironment().name());
            config.set(basePath + "worldType", meta.getWorldType().name());
            config.set(basePath + "seed", meta.getSeed());
            config.set(basePath + "generateStructures", meta.isGenerateStructures());
            config.set(basePath + "isBaseWorld", meta.isBaseWorld());

            // Mutable fields
            if (meta.getScopeID() != null) config.set(basePath + "scope", meta.getScopeID());
            config.set(basePath + "doAutoLoad", meta.doAutoLoad());
            config.set(basePath + "generatorType", meta.getGeneratorType().name());
        }

        // Save to disk
        try {
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            config.save(file);
        } catch (IOException e) {
            Messager.broadcastMessage(plugin.getServer(), "Could not save world file: worlds.yml", NamedTextColor.DARK_RED);
            e.printStackTrace();
        }
    }
}
