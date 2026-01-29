package org.gabooj.commands.group;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.commands.SubCommand;
import org.gabooj.worlds.WorldManager;

import java.util.List;

public class CreateGroupCommand implements SubCommand {

    private final JavaPlugin plugin;
    private final WorldManager worldManager;
    private final GroupCommandHandler commandHandler;

    public CreateGroupCommand(JavaPlugin plugin, WorldManager worldManager, GroupCommandHandler commandHandler) {
        this.plugin = plugin;
        this.worldManager = worldManager;
        this.commandHandler = commandHandler;
    }

    @Override
    public String name() {
        return "create";
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
        return "A command to create a new world group. Use '/group create <group name>' to make a new world group.";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        String groupName = args[0].toLowerCase();

        if (worldManager.isInvalidName(groupName)) {
            sender.sendMessage(ChatColor.RED + "That group name cannot exist as it is the same name as a world's ID, group ID, or group name!");
            return;
        }
        worldManager.scopeManager.createScope(groupName);
        worldManager.scopeManager.saveScopes();
        sender.sendMessage(ChatColor.GOLD + "Created new group: " + groupName + ".");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of("<group name>");
    }
}
