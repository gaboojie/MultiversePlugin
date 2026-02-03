package org.gabooj.scope;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import com.destroystokyo.paper.event.player.PlayerSetSpawnEvent;

import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.commands.group.GameRuleConfigCommand;
import org.gabooj.commands.world.PlayerInfoWorldCommand;
import org.gabooj.players.PlayerInventorySerializer;
import org.gabooj.players.PlayerLocationSerializer;
import org.gabooj.players.PlayerMiscSerializer;
import org.gabooj.players.PlayerTabManager;
import org.gabooj.players.afk.AfkManager;
import org.gabooj.utils.Messager;
import org.gabooj.worlds.WorldManager;
import org.gabooj.worlds.WorldMeta;

import java.util.*;

public class ScopeListener implements Listener {

    private final WorldManager worldManager;
    private final JavaPlugin plugin;

    public ScopeListener(WorldManager worldManager, JavaPlugin plugin) {
        this.worldManager = worldManager;
        this.plugin = plugin;
    }

    public void onEnable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void onDisable() {

    }

    // PLAYER JOIN/QUIT/KICK HANDLING

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Clear player PDC data if queued
        if (PlayerInfoWorldCommand.pendingPlayerClear.remove(player.getUniqueId())) {
            PersistentDataContainer container = player.getPersistentDataContainer();
            // Copy keys to avoid concurrent modification
            Set<NamespacedKey> keys = new HashSet<>(container.getKeys());
            for (NamespacedKey key : keys) {
                container.remove(key);
            }
            Messager.sendSevereWarningMessage(player, "An admin requested that your PDC world data be cleared when you were offline. Data was just cleared.");
        }

        // Handle teleport & persist player state
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isDead()) {
                player.spigot().respawn();
            } else {
                worldManager.worldTeleporter.handlePlayerJoinTeleportation(player);
            }
        }, 1L);
    }

    @EventHandler
    public void onPlayerKickEvent(PlayerKickEvent event) {
        handlePlayerLeaveEvent(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        handlePlayerLeaveEvent(event.getPlayer());
    }

    private void handlePlayerLeaveEvent(Player player) {
        ScopeManager scopeManager = worldManager.scopeManager;

        // Save player state
        ScopeMeta meta = scopeManager.getScopeForWorld(player.getWorld());

        // Do not save player state on death
        if (!player.isDead()) scopeManager.savePlayerStateInScope(player, player.getLocation(), meta);

        // Remove player UUID
        AfkManager.removePlayer(player);
    }

    // PLAYER DEATH & RESPAWN CYCLE

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        ScopeManager scopeManager = worldManager.scopeManager;
        Player player = event.getPlayer();
        ScopeMeta scopeMeta = scopeManager.getScopeForWorld(player.getWorld());

        // Handle keep inventory
        GameRule<?> rule = GameRuleConfigCommand.getGameRuleByName("keep_inventory");
        @SuppressWarnings("unchecked")
        Boolean keepInv = (rule != null && rule.getType() == Boolean.class) ? player.getWorld().getGameRuleValue((GameRule<Boolean>) rule) : false;
        if (Boolean.FALSE.equals(keepInv)) {
            // Clear inventory state if keepInv is off
            PlayerInventorySerializer.clearInventoryStateOnDeath(player, scopeMeta, plugin);
            PlayerMiscSerializer.clearStateOnDeath(player, scopeMeta, plugin);
        } else {
            // Save inventory state if keepInv is on
            PlayerInventorySerializer.saveInventory(player, scopeMeta, plugin);
            PlayerMiscSerializer.savePlayerState(player, scopeMeta, plugin);
        }

        // Save player location as respawn location
        Location loc = scopeManager.getPlayerSpawnLocationOfScope(scopeMeta, player);
        if (loc == null) {
            return;
        }
        loc.setYaw(0);
        loc.setPitch(0);
        PlayerLocationSerializer.savePlayerLocInScope(player, loc, scopeMeta, plugin);
    }

    @EventHandler
    public void onPlayerRespawnEvent(PlayerRespawnEvent event) {
        ScopeManager scopeManager = worldManager.scopeManager;
        Player player = event.getPlayer();
        World deathWorld = player.getWorld();
        ScopeMeta meta = scopeManager.getScopeForWorld(deathWorld);

        Location loc = scopeManager.getPlayerSpawnLocationOfScope(meta, player);
        if (loc != null) {
            event.setRespawnLocation(loc);
        } else {
            Messager.broadcastMessage(plugin.getServer(), "SEVERE ERROR: Server could not find proper respawn location!", NamedTextColor.DARK_RED);
        }
    }


    @EventHandler
    public void onPostRespawn(PlayerPostRespawnEvent event) {
        ScopeManager scopeManager = worldManager.scopeManager;
        Player player = event.getPlayer();
        ScopeMeta scope = scopeManager.getScopeForWorld(player.getWorld());

        // Load player data
        scopeManager.applyScopeToPlayer(player, scope);
        PlayerTabManager.updatePlayerTab(player);
    }

    // PORTAL LOGIC

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        World fromWorld = event.getFrom().getWorld();
        ScopeManager scopeManager = worldManager.scopeManager;

        ScopeMeta scope = scopeManager.getScopeForWorld(fromWorld);

        // Ensure dimension exists
        Location vanillaTarget = event.getTo();
        World.Environment targetEnv = vanillaTarget.getWorld().getEnvironment();
        WorldMeta targetWorldMeta = scope.getWorldMetaByEnvironment(targetEnv);
        if (targetWorldMeta == null || targetWorldMeta.isUnloading) {
            event.setCancelled(true);
            Messager.sendWarningMessage(player, "This dimension is not available.");
            return;
        }

        World targetWorld = targetWorldMeta.getWorld();
        if (targetWorld == null) {
            // Target world is not loaded yet; attempt to load
            boolean didLoad = worldManager.loadWorldFromMetaData(targetWorldMeta);
            if (!didLoad) {
                event.setCancelled(true);
                Messager.sendWarningMessage(player, "Uh-oh! The dimension could not be loaded. Please contact an administrator.");
                return;
            }
            targetWorld = targetWorldMeta.getWorld();
        }

        // Keep vanilla portal coordinates, orientation, and scaling
        Location redirected = new Location(
                targetWorld,
                vanillaTarget.getX(),
                vanillaTarget.getY(),
                vanillaTarget.getZ(),
                vanillaTarget.getYaw(),
                vanillaTarget.getPitch()
        );

        // Set the target location — vanilla will handle portal linking/creation
        event.setTo(redirected);
        PlayerTabManager.updatePlayerTab(event.getPlayer(), scope, targetWorldMeta);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityPortal(EntityPortalEvent event) {
        if (event.getEntity() instanceof Player) return; // Ignore players

        // Ignore null location events
        if (event.getTo() == null) {
            event.setCancelled(true);
            return;
        }

        // Ignore portal linkage if target environment DNE
        World fromWorld = event.getFrom().getWorld();
        ScopeMeta scope = worldManager.scopeManager.getScopeForWorld(fromWorld);
        World.Environment targetEnv = event.getTo().getWorld().getEnvironment();
        WorldMeta targetWorldMeta = scope.getWorldMetaByEnvironment(targetEnv);
        if (targetWorldMeta == null || targetWorldMeta.isUnloading) {
            event.setCancelled(true);
            return;
        }

        // Load world if unloaded
        World targetWorld = targetWorldMeta.getWorld();
        if (targetWorld == null) {
            // Target world is not loaded yet; attempt to load
            boolean didLoad = worldManager.loadWorldFromMetaData(targetWorldMeta);
            if (!didLoad) {
                event.setCancelled(true);
                return;
            }
            targetWorld = targetWorldMeta.getWorld();
        }

        // Update location to the correct world
        Location to = event.getTo();
        event.setTo(new Location(
                targetWorld,
                to.getX(),
                to.getY(),
                to.getZ(),
                to.getYaw(),
                to.getPitch()
        ));
    }

    // SAVE PLAYER BED EVENT

    @EventHandler
    public void onSpawnChange(PlayerSetSpawnEvent event) {
        // Only handle bed setting
        event.setNotifyPlayer(false);
        Player player = event.getPlayer();
        if (event.getCause() != PlayerSetSpawnEvent.Cause.BED) {
            if (event.getCause() == PlayerSetSpawnEvent.Cause.RESPAWN_ANCHOR)
                Messager.sendWarningMessage(player, "Your spawn can only be set by beds!");
            return;
        }

        // Get clicked bed block via Ray Casting
        Block clickedBlock = player.getTargetBlockExact(5);
        if (clickedBlock == null || !(Tag.BEDS.isTagged(clickedBlock.getType()))) {
            Messager.sendWarningMessage(player, "For some reason, your bed spawn could not be saved!");
            return;
        }

        // Save bed location
        ScopeMeta scopeMeta = worldManager.scopeManager.getScopeForWorld(player.getWorld());
        PlayerMiscSerializer.savePlayerBedLocInScope(player, clickedBlock.getLocation(), scopeMeta, plugin);
        Messager.sendInfoMessage(player, "Saved your spawn in '" + scopeMeta.getName() + "' world.");
    }
}
