package org.gabooj.players;

import io.papermc.paper.persistence.PersistentDataContainerView;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.scope.ScopeMeta;
import org.gabooj.worlds.WorldManager;

public class PlayerLocationSerializer {

    public static void savePlayerLocInScope(Player player, Location loc, ScopeMeta scopeMeta, JavaPlugin plugin) {
        PersistentDataContainer scopeContainer = PlayerSerializer.getOrCreateScopePDC(player, scopeMeta, plugin);

        // Save location
        PersistentDataContainer locContainer = scopeContainer.getAdapterContext().newPersistentDataContainer();
        locContainer.set(PlayerSerializer.LOC_X_KEY, PersistentDataType.DOUBLE, loc.getX());
        locContainer.set(PlayerSerializer.LOC_Y_KEY, PersistentDataType.DOUBLE, loc.getY());
        locContainer.set(PlayerSerializer.LOC_Z_KEY, PersistentDataType.DOUBLE, loc.getZ());
        locContainer.set(PlayerSerializer.LOC_YAW_KEY, PersistentDataType.FLOAT, loc.getYaw());
        locContainer.set(PlayerSerializer.LOC_PITCH_KEY, PersistentDataType.FLOAT, loc.getPitch());
        locContainer.set(PlayerSerializer.LOC_WORLD_KEY, PersistentDataType.STRING, loc.getWorld().getName());
        scopeContainer.set(PlayerSerializer.LOC_KEY, PersistentDataType.TAG_CONTAINER, locContainer);

        PlayerSerializer.savePrimaryPDC(player, scopeMeta, plugin, scopeContainer);
    }

    public static void setPlayerLastScope(Player player, ScopeMeta scope) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        if (scope != null) {
            pdc.set(PlayerSerializer.LAST_SCOPE_KEY, PersistentDataType.STRING, scope.getScopeId());
        }
    }

    public static ScopeMeta getLastPlayerScope(Player player, WorldManager worldManager) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        if (!pdc.has(PlayerSerializer.LAST_SCOPE_KEY, PersistentDataType.STRING)) return null;

        String scopeID = pdc.get(PlayerSerializer.LAST_SCOPE_KEY, PersistentDataType.STRING);
        if (scopeID == null) return null;
        return worldManager.scopeManager.getScopeByID(scopeID);
    }

    public static ScopeMeta getLastOfflinePlayerScope(OfflinePlayer player, WorldManager worldManager) {
        PersistentDataContainerView pdc = player.getPersistentDataContainer();
        if (!pdc.has(PlayerSerializer.LAST_SCOPE_KEY, PersistentDataType.STRING)) return null;

        String scopeID = pdc.get(PlayerSerializer.LAST_SCOPE_KEY, PersistentDataType.STRING);
        if (scopeID == null) return null;
        return worldManager.scopeManager.getScopeByID(scopeID);
    }

    public static String getLastPlayerWorldInScope(Player player, ScopeMeta scopeMeta, JavaPlugin plugin) {
        // Get world container
        PersistentDataContainer scopeContainer = PlayerSerializer.getScopePDC(player, scopeMeta, plugin);
        if (scopeContainer == null) return null;

        // Get location container
        PersistentDataContainer locContainer = scopeContainer.get(PlayerSerializer.LOC_KEY, PersistentDataType.TAG_CONTAINER);
        if (locContainer == null) return null;

        // Read worldID
        return locContainer.get(PlayerSerializer.LOC_WORLD_KEY, PersistentDataType.STRING);
    }

    public static String getLastOfflinePlayerWorldInScope(OfflinePlayer player, ScopeMeta scopeMeta, JavaPlugin plugin) {
        // Get world container
        PersistentDataContainer scopeContainer = PlayerSerializer.getScopePDC(player, scopeMeta, plugin);
        if (scopeContainer == null) return null;

        // Get location container
        PersistentDataContainer locContainer = scopeContainer.get(PlayerSerializer.LOC_KEY, PersistentDataType.TAG_CONTAINER);
        if (locContainer == null) return null;

        // Read worldID
        return locContainer.get(PlayerSerializer.LOC_WORLD_KEY, PersistentDataType.STRING);
    }

    public static Location getPlayerLocInScope(Player player, ScopeMeta scopeMeta, JavaPlugin plugin) {
        // Get scope container
        PersistentDataContainer scopeContainer = PlayerSerializer.getScopePDC(player, scopeMeta, plugin);
        if (scopeContainer == null) return null;

        // Get location container
        PersistentDataContainer locContainer = scopeContainer.get(PlayerSerializer.LOC_KEY, PersistentDataType.TAG_CONTAINER);
        if (locContainer == null) return null;

        // Read coords
        Double x = locContainer.get(PlayerSerializer.LOC_X_KEY, PersistentDataType.DOUBLE);
        Double y = locContainer.get(PlayerSerializer.LOC_Y_KEY, PersistentDataType.DOUBLE);
        Double z = locContainer.get(PlayerSerializer.LOC_Z_KEY, PersistentDataType.DOUBLE);
        Float yaw = locContainer.get(PlayerSerializer.LOC_YAW_KEY, PersistentDataType.FLOAT);
        Float pitch = locContainer.get(PlayerSerializer.LOC_PITCH_KEY, PersistentDataType.FLOAT);
        String worldID = locContainer.get(PlayerSerializer.LOC_WORLD_KEY, PersistentDataType.STRING);

        // If any value is null, abort
        if (x == null || y == null || z == null || yaw == null || pitch == null || worldID == null) return null;

        // Attempt to get world
        World world = Bukkit.getWorld(worldID);
        if (world == null) return null;
        return new Location(world, x, y, z, yaw, pitch);
    }


}
