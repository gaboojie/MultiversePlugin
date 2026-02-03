package org.gabooj.services;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.gabooj.utils.Messager;
import org.gabooj.worlds.WorldManager;
import org.gabooj.worlds.WorldMeta;

public class PlayerMoveService {

    public static final int DEFAULT_POLLING_TICKS = 20;
    public static final int DEFAULT_TPA_WARMUP_SECONDS = 5;

    private static final Map<UUID, PendingTeleport> pendingTeleports = new ConcurrentHashMap<>();
    private static BukkitTask pollingTask;

    public static synchronized void addMoveScheduler(JavaPlugin plugin, WorldManager worldManager) {
        if (pollingTask != null && !pollingTask.isCancelled()) {
            return;
        }

        // Add polling task
        pollingTask = new BukkitRunnable() {
            @Override
            public void run() {
                tickTeleportWarmups(worldManager);
            }
        }.runTaskTimer(plugin, 1L, DEFAULT_POLLING_TICKS);
    }

    public static synchronized void onDisable() {
        if (pollingTask != null) {
            pollingTask.cancel();
            pollingTask = null;
        }
        pendingTeleports.clear();
    }

    public static void beginTeleportWarmup(
            Player player,
            Location destination,
            int warmupSeconds,
            Runnable onSuccess,
            Runnable onError
    ) {
        if (player == null || destination == null) {
            return;
        }

        Location start = player.getLocation();
        World destinationWorld = destination.getWorld();
        String destinationWorldName = destinationWorld != null ? destinationWorld.getName() : null;
        long executeAtMillis = System.currentTimeMillis() + (Math.max(0, warmupSeconds) * 1000L);

        pendingTeleports.put(
                player.getUniqueId(),
                new PendingTeleport(
                        start.getWorld() != null ? start.getWorld().getName() : null,
                        new Position(start.getX(), start.getY(), start.getZ()),
                        destinationWorldName,
                        new Position(destination.getX(), destination.getY(), destination.getZ()),
                        destination.getYaw(),
                        destination.getPitch(),
                        onSuccess,
                        onError,
                        executeAtMillis
                )
        );
    }

    public static void cancelTeleportWarmup(Player player, String message) {
        if (player == null) {
            return;
        }
        boolean removed = pendingTeleports.remove(player.getUniqueId()) != null;
        if (removed && message != null && !message.isEmpty()) {
            player.sendMessage(message);
        }
    }

    public static boolean hasTeleportWarmup(Player player) {
        return player != null && pendingTeleports.containsKey(player.getUniqueId());
    }

    private static void tickTeleportWarmups(WorldManager worldManager) {
        if (pendingTeleports.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, PendingTeleport>> iterator = pendingTeleports.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingTeleport> entry = iterator.next();
            UUID playerId = entry.getKey();
            PendingTeleport pending = entry.getValue();
            Player player = Bukkit.getPlayer(playerId);

            if (player == null || !player.isOnline() || player.isDead()) {
                iterator.remove();
                continue;
            }

            Location current = player.getLocation();
            if (hasMovedFromStart(pending.startWorldName, pending.startPosition, current)) {
                iterator.remove();
                runCallback(pending.onError);
                continue;
            }

            if (now >= pending.executeAtMillis) {
                iterator.remove();

                if (pending.destinationWorldName == null) {
                    runCallback(pending.onError);
                    continue;
                }
                WorldMeta meta = worldManager.getWorldMetaByID(pending.destinationWorldName);
                if (meta.isUnloading) {
                    Messager.sendWarningMessage(player, "You cannot teleport, because the world is unloading!");
                    continue;
                }
                if (!meta.isLoaded()) {
                    Messager.sendWarningMessage(player, "You cannot teleport, because the world is not loaded!");
                    continue;
                }

                // Return error if world DNE
                World destinationWorld = meta.getWorld();
                if (destinationWorld == null) {
                    runCallback(pending.onError);
                    continue;
                }

                Location destination = new Location(
                        destinationWorld,
                        pending.destinationPosition.x,
                        pending.destinationPosition.y,
                        pending.destinationPosition.z,
                        pending.destinationYaw,
                        pending.destinationPitch
                );
                player.teleport(destination);
                runCallback(pending.onSuccess);
            }
        }
    }

    private static void runCallback(Runnable callback) {
        if (callback == null) {
            return;
        }
        try {
            callback.run();
        } catch (Exception ignored) {
        }
    }

    private static boolean hasMovedFromStart(String startWorldName, Position start, Location current) {
        World currentWorld = current.getWorld();
        String currentWorldName = currentWorld != null ? currentWorld.getName() : null;
        if (startWorldName == null || currentWorldName == null || !startWorldName.equals(currentWorldName)) {
            return true;
        }
        return Double.compare(start.x, current.getX()) != 0
                || Double.compare(start.y, current.getY()) != 0
                || Double.compare(start.z, current.getZ()) != 0;
    }

    private static class PendingTeleport {
        private final String startWorldName;
        private final Position startPosition;
        private final String destinationWorldName;
        private final Position destinationPosition;
        private final float destinationYaw;
        private final float destinationPitch;
        private final Runnable onSuccess;
        private final Runnable onError;
        private final long executeAtMillis;

        private PendingTeleport(
                String startWorldName,
                Position startPosition,
                String destinationWorldName,
                Position destinationPosition,
                float destinationYaw,
                float destinationPitch,
                Runnable onSuccess,
                Runnable onError,
                long executeAtMillis
        ) {
            this.startWorldName = startWorldName;
            this.startPosition = startPosition;
            this.destinationWorldName = destinationWorldName;
            this.destinationPosition = destinationPosition;
            this.destinationYaw = destinationYaw;
            this.destinationPitch = destinationPitch;
            this.onSuccess = onSuccess;
            this.onError = onError;
            this.executeAtMillis = executeAtMillis;
        }
    }
}
