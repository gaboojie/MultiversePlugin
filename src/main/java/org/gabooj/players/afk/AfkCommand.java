package org.gabooj.players.chat;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.commands.SubCommand;
import org.gabooj.players.afk.AfkManager;
import org.gabooj.worlds.WorldManager;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class AfkCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final WorldManager worldManager;

    public AfkCommand(JavaPlugin plugin, WorldManager worldManager) {
        this.plugin = plugin;
        this.worldManager = worldManager;

        plugin.getCommand("afk").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "You must be a player to execute this command.");
            return true;
        }

        boolean isAfk = AfkManager.isAfk(player);

        // Execute command
        String[] remaining = Arrays.copyOfRange(args, 1, args.length);
        sub.execute(sender, remaining);
        return true;
    }

}
