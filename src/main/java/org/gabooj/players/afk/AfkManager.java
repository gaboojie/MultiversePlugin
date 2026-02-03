package org.gabooj.players.afk;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.gabooj.players.PlayerTabManager;
import org.gabooj.players.chat.ChatManager;
import org.gabooj.players.chat.ChatSettings;
import org.gabooj.utils.Messager;

import net.kyori.adventure.text.format.NamedTextColor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AfkManager {
    private static final Map<UUID, AfkData> afkMap = new HashMap<>();

    private static final long AFK_TIME_SECONDS = 300;
    private static final long AFK_POLLING_SECONDS = 5;

    public static AfkData getPlayerAfkData(Player player) {
        return afkMap.computeIfAbsent(player.getUniqueId(), uuid -> {
            AfkData data = new AfkData();
            Location loc = player.getLocation();
            data.lastBlockX = loc.getBlockX();
            data.lastBlockY = loc.getBlockY();
            data.lastBlockZ = loc.getBlockZ();
            data.lastWorld = loc.getWorld().getName();
            data.lastMovedMillis = System.currentTimeMillis();
            data.afk = false;
            return data;
        });
    }

    public static void registerPollingTask(JavaPlugin plugin) {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();

                for (Player player : Bukkit.getOnlinePlayers()) {
                    AfkData data = AfkManager.getPlayerAfkData(player);
                    Location current = player.getLocation();

                    if (hasMoved(data, current)) {
                        // Update data
                        data.lastBlockX = current.getBlockX();
                        data.lastBlockY = current.getBlockY();
                        data.lastBlockZ = current.getBlockZ();
                        data.lastWorld = current.getWorld().getName();
                        data.lastMovedMillis = now;

                        if (data.afk) {
                            data.afk = false;
                            onLeavingAfk(player);
                        }
                    } else {
                        if (!data.afk && now - data.lastMovedMillis >= (AFK_TIME_SECONDS * 1000)) {
                            data.afk = true;
                            onStartingAfk(player);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L * AFK_POLLING_SECONDS);
    }

    public static void onLeavingAfk(Player player) {
        ChatSettings settings = ChatManager.getOrCreateChatSettings(player);
        Messager.broadcastMessage(player.getServer(), settings.nickname + " is no longer afk.", NamedTextColor.GRAY);
        PlayerTabManager.updatePlayerTab(player);
    }

    public static void onStartingAfk(Player player) {
        ChatSettings settings = ChatManager.getOrCreateChatSettings(player);
        Messager.broadcastMessage(player.getServer(), settings.nickname + " is afk.", NamedTextColor.GRAY);
        PlayerTabManager.updatePlayerTab(player);
    }

    public static boolean isAfk(Player player) {
        AfkData data = afkMap.get(player.getUniqueId());
        return data != null && data.afk;
    }

    public static void removePlayer(Player player) {
        afkMap.remove(player.getUniqueId());
    }

    private static boolean hasMoved(AfkData from, Location to) {
        if (from.lastBlockX != to.getBlockX() || from.lastBlockY != to.getBlockY() || from.lastBlockZ != to.getBlockZ()) return true;
        return !from.lastWorld.equals(to.getWorld().getName());
    }


}
