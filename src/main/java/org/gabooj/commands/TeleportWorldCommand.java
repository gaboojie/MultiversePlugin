package org.gabooj.commands;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.WorldManager;
import org.gabooj.WorldMeta;

import java.util.Collection;
import java.util.List;

public class TeleportWorldCommand implements SubCommand {

    private final JavaPlugin plugin;
    private final WorldManager worldManager;
    private final CommandHandler commandHandler;

    public TeleportWorldCommand(JavaPlugin plugin, WorldManager worldManager, CommandHandler commandHandler) {
        this.plugin = plugin;
        this.worldManager = worldManager;
        this.commandHandler = commandHandler;
    }

    @Override
    public String name() {
        return "teleport";
    }

    @Override
    public List<String> aliases() {
        return List.of("tp");
    }

    @Override
    public boolean needsOp() {
        return false;
    }

    @Override
    public boolean needsToBePlayer() {
        return true;
    }

    @Override
    public String description(CommandSender sender) {
        return "A command to teleport yourself to a given world via '/world teleport <world name>'. Use /world list to get a list of every world to teleport to.";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // Ensure that sender is player
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "You must be a player to execute this command!");
            return;
        }

        // Get world
        Collection<WorldMeta> worlds = worldManager.worlds.values();
        WorldMeta worldToUse = null;
        for (WorldMeta meta : worlds) {
            if (meta.worldName.equalsIgnoreCase(args[0])) {
                worldToUse = meta;
                break;
            }
        }

        // No world found
        if (worldToUse == null) {
            player.sendMessage(ChatColor.RED + args[0] + " is not the name of a world or a /world command.");
            return;
        }

        // If world is not visible, ignore
        if (!worldToUse.visible) {
            player.sendMessage(ChatColor.RED + "That world is not visible.");
            return;
        }

        worldManager.teleportPlayerToWorld(player, worldToUse);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return worldManager.getVisibleWorldNames();
    }
}
