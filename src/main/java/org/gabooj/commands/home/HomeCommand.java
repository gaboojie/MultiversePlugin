package org.gabooj.commands.home;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.scope.ScopeMeta;
import org.gabooj.services.PlayerMoveService;
import org.gabooj.utils.Messager;
import org.gabooj.worlds.WorldManager;
import org.jetbrains.annotations.NotNull;

public class HomeCommand implements CommandExecutor {

    private final WorldManager worldManager;

    public HomeCommand(JavaPlugin plugin, WorldManager worldManager) {
        this.worldManager = worldManager;

        PluginCommand command = plugin.getCommand("home");
        if (command == null) {
            plugin.getLogger().warning("Command 'home' is not defined in plugin.yml");
            return;
        }
        command.setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            Messager.sendWarningMessage(sender, "You must be a player to execute this command.");
            return true;
        }

        // Ensure that player has a home location
        ScopeMeta scopeMeta = worldManager.scopeManager.getCurrentPlayerScope(player);
        Location loc = worldManager.scopeManager.getPlayerBedSpawnInScope(scopeMeta, player);
        if (loc == null) {
            Messager.sendWarningMessage(player, "You do not have a home set.");
            return true;
        }

        // Queue for teleport
        Runnable onSuccess = () -> Messager.sendSuccessMessage(player, "Teleported to your home.");
        Runnable onFailure = () -> Messager.sendWarningMessage(player, "You moved!");
        PlayerMoveService.beginTeleportWarmup(player, loc, PlayerMoveService.DEFAULT_TPA_WARMUP_SECONDS, onSuccess, onFailure);
        Messager.sendInfoMessage(player, "Teleporting to your home in 5 seconds... Do not move.");

        return true;
    }

}
