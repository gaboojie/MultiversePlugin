package org.gabooj.commands.world;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.commands.SubCommand;
import org.gabooj.players.PlayerLocationSerializer;
import org.gabooj.scope.ScopeMeta;
import org.gabooj.utils.Messager;
import org.gabooj.worlds.WorldManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerInfoWorldCommand implements SubCommand {

    private final JavaPlugin plugin;
    private final WorldManager worldManager;

    public static Set<UUID> pendingPlayerClear = ConcurrentHashMap.newKeySet();

    public PlayerInfoWorldCommand(JavaPlugin plugin, WorldManager worldManager) {
        this.plugin = plugin;
        this.worldManager = worldManager;
    }

    @Override
    public String name() {
        return "playerInfo";
    }

    @Override
    public List<String> aliases() {
        return List.of();
    }

    @Override
    public boolean needsOp() {
        return true;
    }

    @Override
    public boolean needsToBePlayer() {
        return false;
    }

    @Override
    public String description(CommandSender sender) {
        return "A command to get or clear player world info from a player. Use /world playerInfo <get/clear> <player name> to get or clear the player world information from a player.";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 1) {
            Messager.sendWarningMessage(sender, "Use /world playerInfo <get/clear> <player name>.");
            return;
        }

        String playerName = args[1];
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        if (player.getName() == null || (!player.hasPlayedBefore() && !player.isOnline())) {
            Messager.sendWarningMessage(sender, "No (online or offline) players by the name: '" + playerName + "' exist!");
            return;
        }

        String action = args[0];
        if (action.equalsIgnoreCase("get")) {
            sendGetInfoToSender(sender, player);
        } else if(action.equalsIgnoreCase("clear")) {
            clearInfo(sender, player);
        } else {
            Messager.sendWarningMessage(sender, action + " was not a recognized subcommand. Your subcommand must be 'get' or 'clear'.");
        }
    }

    public void clearInfo(CommandSender sender, OfflinePlayer offlinePlayer) {
        if (offlinePlayer.isOnline()) {
            Player player = (Player) offlinePlayer;
            PersistentDataContainer container = player.getPersistentDataContainer();

            // Copy keys to avoid concurrent modification
            Set<NamespacedKey> keys = new HashSet<>(container.getKeys());
            for (NamespacedKey key : keys) {
                container.remove(key);
            }

            Messager.sendSuccessMessage(sender, "Cleared all persistent world data from the player.");
        } else {
            pendingPlayerClear.add(offlinePlayer.getUniqueId());
            Messager.sendSuccessMessage(sender, "Player is offline. Data will be cleared on next login (if no restarts occur).");
        }
    }

    public void sendGetInfoToSender(CommandSender sender, OfflinePlayer player) {
        String msg = "";

        ScopeMeta scopeMeta = PlayerLocationSerializer.getLastOfflinePlayerScope(player, worldManager);
        if (scopeMeta != null) {
            msg += "Last scope: " + scopeMeta.getScopeId() + "\n";
        } else {
            msg += "Last scope: None\n";
        }

        for (ScopeMeta scopeMetaInstance : worldManager.scopeManager.getScopes().values()) {
            String worldID = PlayerLocationSerializer.getLastOfflinePlayerWorldInScope(player, scopeMetaInstance, plugin);
            if (worldID == null) worldID = "None";
            msg += "- Scope '" + scopeMetaInstance.getScopeId() + "' has last world: '" + worldID + "'\n";
        }

        Messager.sendInfoMessage(sender, msg);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("get", "clear");
        } else return plugin.getServer().getOnlinePlayers().stream().map(Player::getName).toList();
    }
}
