package org.gabooj.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.WorldManager;
import org.gabooj.WorldMeta;

import java.util.Collection;
import java.util.List;

public class DetailsCommand implements SubCommand {

    private final JavaPlugin plugin;
    private final WorldManager worldManager;
    private final CommandHandler commandHandler;

    public DetailsCommand(JavaPlugin plugin, WorldManager worldManager, CommandHandler commandHandler) {
        this.plugin = plugin;
        this.worldManager = worldManager;
        this.commandHandler = commandHandler;
    }

    @Override
    public String name() {
        return "details";
    }

    @Override
    public List<String> aliases() {
        return List.of("info");
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
        return "A command to show the details of a given world. Use /world details <world> to get verbose details about a world.";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
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

        // Show details about world
        sender.sendMessage(ChatColor.GOLD + worldToUse.toString());
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return worldManager.getWorldNames();
    }
}
