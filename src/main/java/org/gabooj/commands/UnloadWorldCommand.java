package org.gabooj.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.WorldManager;
import org.gabooj.WorldMeta;

import java.util.Collection;
import java.util.List;

public class UnloadWorldCommand implements SubCommand {

    private final JavaPlugin plugin;
    private final WorldManager worldManager;
    private final CommandHandler commandHandler;

    public UnloadWorldCommand(JavaPlugin plugin, WorldManager worldManager, CommandHandler commandHandler) {
        this.plugin = plugin;
        this.worldManager = worldManager;
        this.commandHandler = commandHandler;
    }

    @Override
    public String name() {
        return "unload";
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
        return "A command to unload a world. Use /world unload <world name> to unload a world.";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
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
            sender.sendMessage(ChatColor.RED + args[0] + " is not the name of a created world.");
            return;
        }

        // Raise error if trying to unload base world
        if (worldToUse.isBaseWorld) {
            sender.sendMessage(ChatColor.DARK_RED + "You CANNOT unload the base world.");
            return;
        }

        boolean didWorldUnload = worldManager.unloadWorld(worldToUse);
        if (didWorldUnload) {
            sender.sendMessage(ChatColor.GOLD + "Successfully unloaded world: '" + worldToUse.worldName + "'.");
        } else {
            sender.sendMessage(ChatColor.RED + "Uh-oh! Could not unload world: '" + worldToUse.worldName + "'.");
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return worldManager.getWorldNames();
    }
}
