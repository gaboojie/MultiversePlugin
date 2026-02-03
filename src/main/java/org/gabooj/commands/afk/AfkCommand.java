package org.gabooj.commands.afk;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.players.afk.AfkData;
import org.gabooj.players.afk.AfkManager;
import org.gabooj.utils.Messager;
import org.jetbrains.annotations.NotNull;

public class AfkCommand implements CommandExecutor {

    public AfkCommand(JavaPlugin plugin) {
        plugin.getCommand("afk").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            Messager.sendWarningMessage(sender, "You must be a player to execute this command.");
            return true;
        }

        AfkData data = AfkManager.getPlayerAfkData(player);
        Location current = player.getLocation();
        data.lastBlockX = current.getBlockX();
        data.lastBlockY = current.getBlockY();
        data.lastBlockZ = current.getBlockZ();
        data.lastWorld = current.getWorld().getName();
        data.lastMovedMillis = System.currentTimeMillis();
        if (data.afk) {
            // Set to no longer afk
            data.afk = false;
            AfkManager.onLeavingAfk(player);
        } else {
            // Set to afk
            data.afk = true;
            AfkManager.onStartingAfk(player);
        }
        return true;
    }

}
