package org.gabooj.commands.group;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.commands.SubCommand;
import org.gabooj.scope.ScopeMeta;
import org.gabooj.worlds.WorldManager;
import org.gabooj.worlds.WorldMeta;

import java.util.List;
import java.util.Set;

public class DeleteGroupCommand implements SubCommand {

    private final JavaPlugin plugin;
    private final WorldManager worldManager;
    private final GroupCommandHandler commandHandler;

    public DeleteGroupCommand(JavaPlugin plugin, WorldManager worldManager, GroupCommandHandler commandHandler) {
        this.plugin = plugin;
        this.worldManager = worldManager;
        this.commandHandler = commandHandler;
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
            sender.sendMessage(ChatColor.RED + "That group does not exist!");
            return;
        }
        ScopeMeta meta = worldManager.scopeManager.getScopeByName(groupName);

        // Determine if worldMeta contains an ungrouped world
        WorldMeta worldMeta = worldManager.getWorldMetaByID(meta.getScopeId());
        if (worldMeta != null) {
            sender.sendMessage(ChatColor.RED + "You cannot delete that group as it is a default group and would be recreated if deleted!");
            return;
        }

        worldManager.scopeManager.removeScope(meta, true);
        sender.sendMessage(ChatColor.GOLD + "Deleted group: " + meta.getName() + ".");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return worldManager.scopeManager.getAllGroupNames();
    }
}
