package org.gabooj.scope;

import org.bukkit.configuration.ConfigurationSection;

public class SpawnLocation {

    public double spawnX, spawnY, spawnZ;
    public float spawnYaw, spawnPitch;
    public String spawnWorldID;
    public boolean doForceSpawn;

    public SpawnLocation(boolean doForceSpawn, double spawnX, double spawnY, double spawnZ, float spawnYaw, float spawnPitch, String spawnWorldID) {
        this.spawnX = spawnX;
        this.spawnY = spawnY;
        this.spawnZ = spawnZ;
        this.spawnYaw = spawnYaw;
        this.spawnPitch = spawnPitch;
        this.spawnWorldID = spawnWorldID;
        this.doForceSpawn = doForceSpawn;
    }

    public static void writeSpawn(ConfigurationSection section, SpawnLocation spawn) {
        section.set("world", spawn.spawnWorldID);
        section.set("x", spawn.spawnX);
        section.set("y", spawn.spawnY);
        section.set("z", spawn.spawnZ);
        section.set("yaw", spawn.spawnYaw);
        section.set("pitch", spawn.spawnPitch);
        section.set("force", spawn.doForceSpawn);
    }

    public static SpawnLocation readSpawn(ConfigurationSection section) {
        return new SpawnLocation(section.getBoolean("force", false),
                section.getDouble("x", 0),
                section.getDouble("y", 100),
                section.getDouble("z", 0),
                (float) section.getDouble("yaw", 0),
                (float) section.getDouble("pitch", 0),
                section.getString("world"));
    }
}
