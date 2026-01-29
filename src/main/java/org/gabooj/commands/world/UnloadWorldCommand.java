package org.gabooj.commands.world;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.commands.SubCommand;
import org.gabooj.worlds.WorldManager;
import org.gabooj.worlds.WorldMeta;

import java.util.List;

public class UnloadWorldCommand implements SubCommand {

    private final JavaPlugin plugin;
    private final WorldManager worldManager;
    private final WorldCommandHandler commandHandler;

    public UnloadWorldCommand(JavaPlugin plugin, WorldManager worldManager, WorldCommandHandler commandHandler) {
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
        WorldMeta meta = worldManager.getWorldMetaByID(args[0]);

        // No world found
        if (meta == null) {
            sender.sendMessage(ChatColor.RED + args[0] + " is not the name of a world.");
            return;
        }

        // Raise error if trying to unload base world
        if (meta.isBaseWorld()) {
            sender.sendMessage(ChatColor.DARK_RED + "You CANNOT unload the base world.");
            return;
        }

        // Raise error if trying to unload the default world
        WorldMeta defaultWorldMeta = worldManager.getDefaultScopeWorld();
        if (defaultWorldMeta != null) {
            if (meta.getWorldID().equalsIgnoreCase(defaultWorldMeta.getWorldID())) {
                sender.sendMessage(ChatColor.DARK_RED + "The default world must always be loaded!");
                return;
            }
        }

        if (!meta.isLoaded()) {
            sender.sendMessage(ChatColor.RED + "That world is already unloaded!");
            return;
        }

        boolean didWorldUnload = worldManager.unloadWorld(meta);
        if (didWorldUnload) {
            sender.sendMessage(ChatColor.GOLD + "Successfully unloaded world: '" + meta.getWorldID() + "'.\n Please do not load this world within the next 10 seconds.");
        } else {
            sender.sendMessage(ChatColor.RED + "Uh-oh! Could not unload world: '" + meta.getWorldID() + "'.");
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return worldManager.getWorldIDs();
    }
}
