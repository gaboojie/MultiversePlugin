package org.gabooj.commands.group;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.commands.SubCommand;
import org.gabooj.scope.ScopeMeta;
import org.gabooj.worlds.WorldManager;
import org.gabooj.worlds.WorldMeta;

import java.util.List;

public class AddGroupCommand implements SubCommand {

    private final JavaPlugin plugin;
    private final WorldManager worldManager;
    private final GroupCommandHandler commandHandler;

    public AddGroupCommand(JavaPlugin plugin, WorldManager worldManager, GroupCommandHandler commandHandler) {
        this.plugin = plugin;
        this.worldManager = worldManager;
        this.commandHandler = commandHandler;
    }

    @Override
    public String name() {
        return "add";
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
        return "A command to add a world to a world group. Use '/group add <group name> <world name>' to add a world to a group.";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Use '/group add <group name> <world name>' to add a world to a group.");
            return;
        }

        String groupName = args[0].toLowerCase();
        String worldName = args[1];

        // Ensure that the group already exists
        if (!worldManager.scopeManager.doesScopeNameExist(groupName)) {
            sender.sendMessage(ChatColor.RED + "That group does not exist!");
            return;
        }
        // Get scope
        ScopeMeta scopeMeta = worldManager.scopeManager.getScopeByName(groupName);

        // Ensure that the world exists
        WorldMeta meta = worldManager.getWorldMetaByID(worldName);
        if (meta == null) {
            sender.sendMessage(ChatColor.RED + "That world name does not exist!");
            return;
        }

        // Ensure that the world isn't already a member of that group
        if (meta.getScopeID().equalsIgnoreCase(scopeMeta.getScopeId())) {
            sender.sendMessage(ChatColor.RED + "That world is already a member of that group!");
            return;
        }

        // Ensure that the world doesn't already have group settings
        if (!meta.getScopeID().equals(meta.getWorldID())) {
            sender.sendMessage(ChatColor.RED + "That world is already a member of group: " + meta.getScopeID() + "!");
            return;
        }

        // Add default spawn location to first world
        if (scopeMeta.getSpawnLocation().spawnWorldID == null) {
            scopeMeta.getSpawnLocation().spawnWorldID = meta.getWorldID();
            worldManager.scopeManager.saveScopes();
        }

        // Remove scopeMeta if it contains the world
        if (worldManager.scopeManager.doesScopeExist(meta.getWorldID())) {
            ScopeMeta prevWorldScopeMeta = worldManager.scopeManager.getScopeByID(meta.getWorldID());
            worldManager.scopeManager.removeScope(prevWorldScopeMeta, false);
        }

        // Add world to group
        scopeMeta.addWorld(meta);
        meta.setScopeID(scopeMeta.getScopeId());
        sender.sendMessage(ChatColor.GOLD + "Added world '" + worldName + "' to group: '" + scopeMeta.getName() + "'.");
        worldManager.saveWorldMetaDatas();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            return worldManager.scopeManager.getAllGroupNames();
        } else {
            return worldManager.getWorldIDs();
        }
    }
}
