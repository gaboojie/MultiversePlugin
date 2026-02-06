package org.gabooj.protection;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class LandProtectionSerializer {

    private static final String FILE_NAME = "claims.yml";

    public static void load(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            plugin.getLogger().warning("No claims.yml file exists!");
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        LandProtectionManager.protectionsByWorld.clear();

        for (String key : config.getKeys(false)) {
            UUID worldId;
            try {
                worldId = UUID.fromString(key);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Invalid world UUID in claims.yml: " + key);
                continue;
            }

            Map<Long, Set<Integer>> worldClaims = new HashMap<>();

            if (config.isList(key)) {
                // Legacy format: UUID: [chunkKey, chunkKey, ...]
                plugin.getLogger().warning("Legacy claims format detected for world " + key + ". Please migrate claims.yml.");
                continue;
            }

            ConfigurationSection worldSection = config.getConfigurationSection(key);
            if (worldSection == null) {
                plugin.getLogger().warning("Invalid claims format for world " + key + ". Expected a section.");
                continue;
            }

            for (String chunkKeyStr : worldSection.getKeys(false)) {
                long chunkKey;
                try {
                    chunkKey = Long.parseLong(chunkKeyStr);
                } catch (NumberFormatException ex) {
                    plugin.getLogger().warning("Invalid chunk key under world " + key + ": " + chunkKeyStr);
                    continue;
                }

                List<Integer> sections = worldSection.getIntegerList(chunkKeyStr);
                if (sections.isEmpty()) {
                    continue;
                }
                worldClaims.put(chunkKey, new HashSet<>(sections));
            }

            if (!worldClaims.isEmpty()) {
                LandProtectionManager.protectionsByWorld.put(worldId, worldClaims);
            }
        }
    }

    public static void save(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        FileConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, Map<Long, Set<Integer>>> entry : LandProtectionManager.protectionsByWorld.entrySet()) {
            Map<Long, Set<Integer>> claims = entry.getValue();
            if (claims == null || claims.isEmpty()) {
                continue;
            }
            String worldKey = entry.getKey().toString();
            for (Map.Entry<Long, Set<Integer>> chunkEntry : claims.entrySet()) {
                Set<Integer> sections = chunkEntry.getValue();
                if (sections == null || sections.isEmpty()) {
                    continue;
                }
                config.set(worldKey + "." + chunkEntry.getKey(), new ArrayList<>(sections));
            }
        }

        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save claims.yml");
            e.printStackTrace();
        }
    }
}
