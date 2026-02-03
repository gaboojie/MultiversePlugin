package org.gabooj.commands.group;

import org.bukkit.command.CommandSender;
import org.gabooj.commands.SubCommand;
import org.gabooj.scope.ScopeMeta;
import org.gabooj.utils.Messager;
import org.gabooj.worlds.WorldManager;
import org.gabooj.worlds.WorldMeta;

import java.util.List;

public class RemoveGroupCommand implements SubCommand {

    private final WorldManager worldManager;

    public RemoveGroupCommand(WorldManager worldManager) {
        this.worldManager = worldManager;
    }

    @Override
    public String name() {
        return "remove";
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
        return "A command to remove a world from a world group. Use '/group remove <group name> <world name>' to remove a world from a group.";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Messager.sendWarningMessage(sender, "Use '/group remove <group name> <world name>' to remove a world from a group.");
            return;
        }

        String groupName = args[0].toLowerCase();
        String worldName = args[1];

        // Determine if world exists
        WorldMeta meta = worldManager.getWorldMetaByID(worldName);
        if (meta == null) {
            Messager.sendWarningMessage(sender, "That world name does not exist!");
            return;
        }

        // Determine if group exists
        if (!worldManager.scopeManager.doesScopeNameExist(groupName)) {
            Messager.sendWarningMessage(sender, "That group does not exist!");
            return;
        }
        ScopeMeta scopeMeta = worldManager.scopeManager.getScopeByName(groupName);

        // Ensure that we are not removing an ungrouped world
        if (scopeMeta.getScopeId().equalsIgnoreCase(worldName)) {
            Messager.sendWarningMessage(sender, "You cannot remove a world from it's default group or it will be auto created again!");
            return;
        }

        // Determine if group exists
        if (!meta.getScopeID().equalsIgnoreCase(scopeMeta.getScopeId())) {
            Messager.sendWarningMessage(sender, "That group does not contain that world!");
            return;
        }

        // Remove world in ScopeMeta
        scopeMeta.removeWorld(meta);

        // Remove world from group
        meta.setScopeID(null);

        // Create new group for world, so it still has a group
        worldManager.scopeManager.createScope(meta.getWorldID());

        Messager.sendInfoMessage(sender, "Removed world '" + worldName + "' from group: '" + scopeMeta.getName() + "'.");
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