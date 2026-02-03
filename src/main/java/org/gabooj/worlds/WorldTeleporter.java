package org.gabooj.worlds;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.players.PlayerLocationSerializer;
import org.gabooj.players.PlayerTabManager;
import org.gabooj.scope.ScopeManager;
import org.gabooj.scope.ScopeMeta;
import org.gabooj.utils.Messager;

import net.kyori.adventure.text.format.NamedTextColor;

public class WorldTeleporter {

    public WorldManager worldManager;
    public JavaPlugin plugin;
    public ScopeManager scopeManager;

    public WorldTeleporter(WorldManager worldManager, JavaPlugin plugin) {
        this.worldManager = worldManager;
        this.plugin = plugin;
        this.scopeManager = worldManager.scopeManager;
    }

    private void teleportPlayerToLocation(Player player, Location location, boolean msgPlayer, boolean saveOldLocation) {
        // Save old scope at the old location
        if (saveOldLocation) {
            ScopeMeta currentScope = scopeManager.getScopeForWorld(player.getLocation().getWorld());
            scopeManager.savePlayerStateInScope(player, player.getLocation(), currentScope);
        }

        // Teleport player
        player.teleport(location);

        // Load new location
        ScopeMeta toMeta = scopeManager.getScopeForWorld(location.getWorld());
        scopeManager.savePlayerLastScope(player, toMeta);
        scopeManager.applyScopeToPlayer(player, toMeta);
        PlayerTabManager.updatePlayerTab(player);

        if (msgPlayer) {
            Messager.sendInfoMessage(player, "Teleported to " + toMeta.getName() + ".");
        }
    }

    public void teleportPlayerToDefaultSpawn(Player player, boolean msgPlayer, boolean saveOldLocation) {
        ScopeMeta scopeToUse = scopeManager.getDefaultScope();
        if (scopeToUse == null) {
            Messager.broadcastMessage(plugin.getServer(), "SEVERE ERROR: NO DEFAULT SCOPE EXISTS!", NamedTextColor.DARK_RED);
            return;
        }

        Location spawnLoc = scopeManager.getSpawnLocationOfScope(scopeToUse);
        if (spawnLoc == null) {
            Messager.broadcastMessage(plugin.getServer(), "SEVERE ERROR: COULD NOT TELEPORT PLAYER TO DEFAULT SCOPE: " + scopeToUse.getName() + "!", NamedTextColor.DARK_RED);
            return;
        }
        teleportPlayerToLocation(player, spawnLoc, msgPlayer, saveOldLocation);
    }

    private void evacuateToSpawn(Player player, boolean msgPlayer, boolean saveOldLocation) {
        if (msgPlayer) Messager.sendWarningMessage(player, "Evacuating you to the default server spawn...");
        teleportPlayerToDefaultSpawn(player, msgPlayer, saveOldLocation);
    }

    public void teleportPlayerToDefaultSpawnOfScope(Player player, ScopeMeta scopeMeta, boolean msgPlayer, boolean doEvacuateToSpawn, boolean saveOldLocation) {
        // Ensure that scope's spawn exists
        String defaultScopeWorldID = scopeMeta.getSpawnLocation().spawnWorldID;
        if (defaultScopeWorldID == null) {
            if (msgPlayer) Messager.sendWarningMessage(player, "Uh-oh! The spawn world of scope: '" + scopeMeta.getName() + "' does not exist.");
            if (doEvacuateToSpawn) evacuateToSpawn(player, msgPlayer, saveOldLocation);
            return;
        }

        // Ignore default spawn of scope if world is unloading
        WorldMeta meta = worldManager.getWorldMetaByID(defaultScopeWorldID);
        if (meta.isUnloading) {
            if (msgPlayer) Messager.sendWarningMessage(player, "The default spawn of world: " + scopeMeta.getScopeId() + " is currently being unloaded, so you could not be teleported.");
            if (doEvacuateToSpawn) evacuateToSpawn(player, msgPlayer, saveOldLocation);
            return;
        }

        // Load world if unloaded
        if (!meta.isLoaded()) {
            boolean didWorldLoad = worldManager.loadWorldFromMetaData(meta);
            if (!didWorldLoad) {
                if (msgPlayer) Messager.sendWarningMessage(player, "For some reason, the default spawn of world: " + scopeMeta.getScopeId() + " could not be loaded. Please contact an admin!");
                if (doEvacuateToSpawn) evacuateToSpawn(player, msgPlayer, saveOldLocation);
                return;
            }
        }

        // If spawn location fails, tell the player
        Location loc = scopeManager.getSpawnLocationOfScope(scopeMeta);
        if (loc == null) {
            if (msgPlayer) Messager.sendWarningMessage(player, "Uh-oh! The spawn world of scope: '" + scopeMeta.getName() + "' does not exist.");
            if (doEvacuateToSpawn) evacuateToSpawn(player, msgPlayer, saveOldLocation);
        } else {
            teleportPlayerToLocation(player, loc, msgPlayer, saveOldLocation);
        }
    }

    public void teleportPlayerToWorldSpawn(Player player, WorldMeta worldMeta, boolean msgPlayer, boolean doEvacuateToSpawn, boolean saveOldLocation) {
        // Ignore world spawn if world is unloading
        if (worldMeta.isUnloading) {
            if (msgPlayer) Messager.sendWarningMessage(player, "The default spawn of world: " + worldMeta.getWorldID() + " is currently being unloaded, so you could not be teleported.");
            if (doEvacuateToSpawn) evacuateToSpawn(player, msgPlayer, saveOldLocation);
            return;
        }

        // Try to load world
        if (!worldMeta.isLoaded()) {
            boolean didWorldLoad = worldManager.loadWorldFromMetaData(worldMeta);
            if (!didWorldLoad) {
                if (msgPlayer) Messager.sendWarningMessage(player, "That world could not be loaded!");
                if (doEvacuateToSpawn) evacuateToSpawn(player, msgPlayer, saveOldLocation);
                return;
            }
        }

        // Teleport player to spawn location of world
        Location targetLoc = worldMeta.getWorld().getSpawnLocation();
        teleportPlayerToLocation(player, targetLoc, msgPlayer, saveOldLocation);
    }

    public void teleportPlayerToNewScope(Player player, ScopeMeta scopeMeta, boolean msgPlayer, boolean doEvacuateToSpawn, boolean enforceScopeChange, boolean saveOldLocation) {
        // Ensure that player is not trying to visit the same scope
        ScopeMeta currentPlayerScope = scopeManager.getCurrentPlayerScope(player);
        if (enforceScopeChange && currentPlayerScope.getScopeId().equals(scopeMeta.getScopeId())) {
            if (msgPlayer) Messager.sendWarningMessage(player, "You are already in '" + currentPlayerScope.getName() + "'!");
            return;
        }

        // If player has not visited the scope or force spawn is true, send player to the default spawn of the scope
        String worldID = PlayerLocationSerializer.getLastPlayerWorldInScope(player, scopeMeta, plugin);
        if (worldID == null || scopeMeta.getSpawnLocation().doForceSpawn) {
            // Send player to scope's default spawn
            teleportPlayerToDefaultSpawnOfScope(player, scopeMeta, msgPlayer, doEvacuateToSpawn, saveOldLocation);
            return;
        }

        // If world ID is not in the scope, send error message
        boolean contains = false;
        for (WorldMeta meta : scopeMeta.getWorlds()) {
            if (meta.getWorldID().equalsIgnoreCase(worldID)) {
                contains = true;
            }
        }
        if (!contains) {
            Messager.broadcastMessage(player.getServer(), "SEVERE ERROR: The scope for your world ID that was saved for your target scope does not exist in the target scope.\nSending you to default server spawn.", NamedTextColor.DARK_RED);
            teleportPlayerToDefaultSpawn(player, msgPlayer, saveOldLocation);
            return;
        }

        // Ignore teleporting if world is unloading
        WorldMeta worldMeta = worldManager.getWorldMetaByID(worldID);
        if (worldMeta.isUnloading) {
            if (msgPlayer) Messager.sendWarningMessage(player, "The world: " + scopeMeta.getScopeId() + " is currently being unloaded, so you could not be teleported.");
            if (doEvacuateToSpawn) evacuateToSpawn(player, msgPlayer, saveOldLocation);
            return;
        }

        // Try to load world if unloaded
        if (!worldMeta.isLoaded()) {
            boolean didWorldLoad = worldManager.loadWorldFromMetaData(worldMeta);
            if (!didWorldLoad) {
                if (msgPlayer) Messager.sendWarningMessage(player, "Uh-oh! The last world you were in '" + scopeMeta.getName() + "' could not be loaded.");
                if (doEvacuateToSpawn) evacuateToSpawn(player, msgPlayer, saveOldLocation);
                return;
            }
        }

        Location loc = PlayerLocationSerializer.getPlayerLocInScope(player, scopeMeta, plugin);
        if (loc == null) {
            // Teleport player to default spawn if last loc does not exist
            teleportPlayerToDefaultSpawnOfScope(player, scopeMeta, msgPlayer, doEvacuateToSpawn, saveOldLocation);
        } else {
            // Teleport player to last location
            teleportPlayerToLocation(player, loc, msgPlayer, saveOldLocation);
        }
    }

    public void handlePlayerJoinTeleportation(Player player) {
        // If force teleport to default scope is true or player does not have a previous scope, teleport player to default spawn
        ScopeMeta lastPlayerScope = PlayerLocationSerializer.getLastPlayerScope(player, worldManager);
        if (scopeManager.forceDefaultScope || lastPlayerScope == null) {
            teleportPlayerToDefaultSpawn(player, true, false);
            return;
        }

        // Otherwise, teleport to last known scope
        teleportPlayerToNewScope(player, lastPlayerScope, true, true, false, false);
    }

}
