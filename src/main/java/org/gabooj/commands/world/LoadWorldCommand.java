package org.gabooj.commands.world;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.commands.SubCommand;
import org.gabooj.worlds.WorldManager;
import org.gabooj.worlds.WorldMeta;

import java.util.List;

public class LoadWorldCommand implements SubCommand {

    private final JavaPlugin plugin;
    private final WorldManager worldManager;
    private final WorldCommandHandler commandHandler;

    public LoadWorldCommand(JavaPlugin plugin, WorldManager worldManager, WorldCommandHandler commandHandler) {
        this.plugin = plugin;
        this.worldManager = worldManager;
        this.commandHandler = commandHandler;
    }

    @Override
    public String name() {
        return "load";
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
        return "A command to load a world. Use /world load <world name> to load a world.";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // Get world
        WorldMeta meta = worldManager.getWorldMetaByID(args[0]);

        // No world found
        if (meta == null) {
            sender.sendMessage(ChatColor.RED + args[0] + " is not the name of a created world.");
            return;
        }

        // Raise error if trying to load base world
        if (meta.isBaseWorld()) {
            sender.sendMessage(ChatColor.DARK_RED + "The base world is always loaded!");
            return;
        }

        // Raise error if already loaded
        if (meta.isLoaded()) {
            sender.sendMessage(ChatColor.RED + "This world is already loaded!");
            return;
        }

        boolean didWorldLoad = worldManager.loadWorldFromMetaData(meta);
        if (didWorldLoad) {
            sender.sendMessage(ChatColor.GOLD + "Successfully loaded world: '" + meta.getWorldID() + "'.");
        } else {
            sender.sendMessage(ChatColor.RED + "Uh-oh! Could not load world: '" + meta.getWorldID() + "'!");
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return worldManager.getWorldIDs();
    }
}
