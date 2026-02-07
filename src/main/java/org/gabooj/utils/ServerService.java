package org.gabooj.utils;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.OfflinePlayer;
import org.bukkit.Server;

public class ServerService {

    public static OfflinePlayer getOfflinePlayerByName(String name, Server server) {
        for (OfflinePlayer player : server.getOfflinePlayers()) {
            if (player.getName() == null) continue;
            if (player.getName().equalsIgnoreCase(name)) return player;
        }
        return null;
    }

    public static List<OfflinePlayer> getValidOfflinePlayers(Server server) {
        List<OfflinePlayer> players = new ArrayList<>();
        for (OfflinePlayer player : server.getOfflinePlayers()) {
            if (player.getName() != null) players.add(player);
        }
        return players;
    }

}
