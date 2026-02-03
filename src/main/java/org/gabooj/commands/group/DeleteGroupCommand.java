package org.gabooj.commands.group;

import org.bukkit.command.CommandSender;
import org.gabooj.commands.SubCommand;
import org.gabooj.scope.ScopeMeta;
import org.gabooj.utils.Messager;
import org.gabooj.worlds.WorldManager;
import org.gabooj.worlds.WorldMeta;

import java.util.List;

public class DeleteGroupCommand implements SubCommand {

    private final WorldManager worldManager;

    public DeleteGroupCommand(WorldManager worldManager) {
        this.worldManager = worldManager;
    }

    @Override
    public String name() {
        return "delete";
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
        return "A command to delete a world group. Use '/group delete <group name>' to delete a world group.";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        String groupName = args[0].toLowerCase();
        if (!worldManager.scopeManager.doesScopeNameExist(groupName)) {
            Messager.sendWarningMessage(sender, "That group does not exist!");
            return;
        }
        ScopeMeta meta = worldManager.scopeManager.getScopeByName(groupName);

        // Determine if worldMeta contains an ungrouped world
        WorldMeta worldMeta = worldManager.getWorldMetaByID(meta.getScopeId());
        if (worldMeta != null) {
            Messager.sendWarningMessage(sender, "You cannot delete that group as it is a default group and would be recreated if deleted!");
            return;
        }

        worldManager.scopeManager.removeScope(meta, true);
        Messager.sendInfoMessage(sender, "Deleted group: " + meta.getName() + ".");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return worldManager.scopeManager.getAllGroupNames();
    }
}
