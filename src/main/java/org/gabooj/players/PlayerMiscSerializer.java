package org.gabooj.players;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.gabooj.scope.ScopeMeta;

public class PlayerMiscSerializer {

    public static void savePlayerBedLocInScope(Player player, Location loc, ScopeMeta scopeMeta, JavaPlugin plugin) {
        PersistentDataContainer scopeContainer = PlayerSerializer.getOrCreateScopePDC(player, scopeMeta, plugin);

        scopeContainer.set(PlayerSerializer.BED_X_KEY, PersistentDataType.INTEGER, loc.getBlockX());
        scopeContainer.set(PlayerSerializer.BED_Y_KEY, PersistentDataType.INTEGER, loc.getBlockY());
        scopeContainer.set(PlayerSerializer.BED_Z_KEY, PersistentDataType.INTEGER, loc.getBlockZ());
        scopeContainer.set(PlayerSerializer.BED_WORLD_KEY, PersistentDataType.STRING, loc.getWorld().getName());

        PlayerSerializer.savePrimaryPDC(player, scopeMeta, plugin, scopeContainer);
    }

    public static void clearPlayerBedLocInScope(Player player, ScopeMeta scopeMeta, JavaPlugin plugin) {
        PersistentDataContainer scopeContainer = PlayerSerializer.getOrCreateScopePDC(player, scopeMeta, plugin);

        scopeContainer.remove(PlayerSerializer.BED_X_KEY);
        scopeContainer.remove(PlayerSerializer.BED_Y_KEY);
        scopeContainer.remove(PlayerSerializer.BED_Z_KEY);
        scopeContainer.remove(PlayerSerializer.BED_WORLD_KEY);

        PlayerSerializer.savePrimaryPDC(player, scopeMeta, plugin, scopeContainer);
    }

    public static Location getPlayerBedLocInScope(Player player, ScopeMeta scopeMeta, JavaPlugin plugin) {
        PersistentDataContainer scopeContainer = PlayerSerializer.getScopePDC(player, scopeMeta, plugin);
        if (scopeContainer == null) return null;

        // Read coords
        Integer x = scopeContainer.get(PlayerSerializer.BED_X_KEY, PersistentDataType.INTEGER);
        Integer y = scopeContainer.get(PlayerSerializer.BED_Y_KEY, PersistentDataType.INTEGER);
        Integer z = scopeContainer.get(PlayerSerializer.BED_Z_KEY, PersistentDataType.INTEGER);
        String worldID = scopeContainer.get(PlayerSerializer.BED_WORLD_KEY, PersistentDataType.STRING);

        // If any value is null, abort
        if (x == null || y == null || z == null || worldID == null) return null;

        // Attempt to get world
        World world = Bukkit.getWorld(worldID);
        if (world == null) return null;
        return new Location(world, x, y, z);
    }

    public static String getPlayerBedWorldInScope(Player player, ScopeMeta scopeMeta, JavaPlugin plugin) {
        PersistentDataContainer scopeContainer = PlayerSerializer.getScopePDC(player, scopeMeta, plugin);
        if (scopeContainer == null) return null;
        return scopeContainer.get(PlayerSerializer.BED_WORLD_KEY, PersistentDataType.STRING);
    }

    public static void clearStateOnDeath(Player player, ScopeMeta scopeMeta, JavaPlugin plugin) {
        PersistentDataContainer scopeContainer = PlayerSerializer.getOrCreateScopePDC(player, scopeMeta, plugin);

        // Update player state on death
        scopeContainer.remove(PlayerSerializer.FIRE_TICKS_KEY);
        scopeContainer.remove(PlayerSerializer.POTION_EFFECTS_KEY);
        scopeContainer.remove(PlayerSerializer.XP_KEY);
        scopeContainer.remove(PlayerSerializer.LEVEL_KEY);
        scopeContainer.set(PlayerSerializer.HEALTH_KEY, PersistentDataType.DOUBLE, 20.0);
        scopeContainer.set(PlayerSerializer.HUNGER_KEY, PersistentDataType.INTEGER, 20);
        scopeContainer.set(PlayerSerializer.SATURATION_KEY, PersistentDataType.FLOAT, 5f);

        // Ensure root is updated (important!)
        PlayerSerializer.savePrimaryPDC(player, scopeMeta, plugin, scopeContainer);
    }

    public static void savePlayerHealth(Player player, ScopeMeta scopeMeta, JavaPlugin plugin, double healthToSave) {
        PersistentDataContainer scopeContainer = PlayerSerializer.getOrCreateScopePDC(player, scopeMeta, plugin);

        scopeContainer.set(PlayerSerializer.HEALTH_KEY, PersistentDataType.DOUBLE, healthToSave);

        PlayerSerializer.savePrimaryPDC(player, scopeMeta, plugin, scopeContainer);
    }

    public static void savePlayerState(Player player, ScopeMeta scopeMeta, JavaPlugin plugin) {
        PersistentDataContainer scopeContainer = PlayerSerializer.getOrCreateScopePDC(player, scopeMeta, plugin);

        // --- Ender Chest ---
        scopeContainer.set(PlayerSerializer.ENDERCHEST_KEY, PersistentDataType.BYTE_ARRAY, PlayerSerializer.serialize(player.getEnderChest().getContents()));

        // --- Fire Ticks ---
        scopeContainer.set(PlayerSerializer.FIRE_TICKS_KEY, PersistentDataType.INTEGER, player.getFireTicks());

        // --- Potion Effects ---
        PotionEffect[] effects = player.getActivePotionEffects().toArray(new PotionEffect[0]);
        scopeContainer.set(PlayerSerializer.POTION_EFFECTS_KEY, PersistentDataType.BYTE_ARRAY, PlayerSerializer.serializePotionEffects(effects));

        // --- XP & Level ---
        scopeContainer.set(PlayerSerializer.XP_KEY, PersistentDataType.INTEGER, player.getTotalExperience());
        scopeContainer.set(PlayerSerializer.LEVEL_KEY, PersistentDataType.INTEGER, player.getLevel());

        // --- Health, Hunger, Saturation ---
        double health = player.getHealth();
        if (health < 1.0) health = 20.0;
        scopeContainer.set(PlayerSerializer.HEALTH_KEY, PersistentDataType.DOUBLE, health);
        scopeContainer.set(PlayerSerializer.HUNGER_KEY, PersistentDataType.INTEGER, player.getFoodLevel());
        scopeContainer.set(PlayerSerializer.SATURATION_KEY, PersistentDataType.FLOAT, player.getSaturation());

        // Ensure root is updated (important!)
        PlayerSerializer.savePrimaryPDC(player, scopeMeta, plugin, scopeContainer);
    }

    public static void loadPlayerState(Player player, ScopeMeta scopeMeta, JavaPlugin plugin) {
        PersistentDataContainer scopeContainer = PlayerSerializer.getScopePDC(player, scopeMeta, plugin);

        // Reset to default player data
        if (scopeContainer == null) {
            player.getEnderChest().clear();
            player.setFireTicks(0);
            player.clearActivePotionEffects();
            player.setLevel(0);
            player.setTotalExperience(0);
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setSaturation(5f);
            return;
        }

        // --- Ender Chest ---
        byte[] enderBytes = scopeContainer.get(PlayerSerializer.ENDERCHEST_KEY, PersistentDataType.BYTE_ARRAY);
        if (enderBytes != null) {
            player.getEnderChest().setContents(PlayerSerializer.deserialize(enderBytes) );
        } else {
            player.getEnderChest().clear();
        }

        // --- Fire Ticks ---
        Integer fireTicks = scopeContainer.get(PlayerSerializer.FIRE_TICKS_KEY, PersistentDataType.INTEGER);
        player.setFireTicks(fireTicks != null ? fireTicks : 0);

        // --- Potion Effects ---
        byte[] effectBytes = scopeContainer.get(PlayerSerializer.POTION_EFFECTS_KEY, PersistentDataType.BYTE_ARRAY);
        player.getActivePotionEffects().forEach(pe -> player.removePotionEffect(pe.getType()));
        if (effectBytes != null) {
            PotionEffect[] effects = PlayerSerializer.deserializePotionEffects(effectBytes);
            for (PotionEffect e : effects) player.addPotionEffect(e);
        }

        // --- XP & Level ---
        Integer xp = scopeContainer.get(PlayerSerializer.XP_KEY, PersistentDataType.INTEGER);
        Integer level = scopeContainer.get(PlayerSerializer.LEVEL_KEY, PersistentDataType.INTEGER);
        player.setLevel(level != null ? level : 0);
        player.setTotalExperience(xp != null ? xp : 0);

        // --- Health, Hunger, Saturation ---
        Double health = scopeContainer.get(PlayerSerializer.HEALTH_KEY, PersistentDataType.DOUBLE);
        Integer hunger = scopeContainer.get(PlayerSerializer.HUNGER_KEY, PersistentDataType.INTEGER);
        Float saturation = scopeContainer.get(PlayerSerializer.SATURATION_KEY, PersistentDataType.FLOAT);

        player.setHealth(health != null ? health : 20);
        player.setFoodLevel(hunger != null ? hunger : 20);
        player.setSaturation(saturation != null ? saturation : 5f);
    }
}
