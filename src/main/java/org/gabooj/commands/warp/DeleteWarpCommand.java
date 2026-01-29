package org.gabooj.commands.warp;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.commands.SubCommand;
import org.gabooj.scope.ScopeMeta;
import org.gabooj.scope.Warp;
import org.gabooj.worlds.WorldManager;

import java.util.List;

public class DeleteWarpCommand implements SubCommand {

    private final JavaPlugin plugin;
    private final WorldManager worldManager;
    private final WarpCommandHandler commandHandler;

    public DeleteWarpCommand(JavaPlugin plugin, WorldManager worldManager, WarpCommandHandler commandHandler) {
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
        return "A command to delete a warp. Use '/warp delete <group name> <warp name>' to delete a warp.";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        // Handle not enough arguments
        if (args.length < 2) {
            player.sendMessage(ChatColor.GOLD + description(sender));
            return;
        }

        // Ensure that the group exists
        String groupName = args[0].toLowerCase();
        if (!worldManager.scopeManager.doesScopeNameExist(groupName)) {
            sender.sendMessage(ChatColor.RED + "That group does not exist!");
            return;
        }
        ScopeMeta scopeMeta = worldManager.scopeManager.getScopeByName(groupName);

        // Ensure that warp is not null
        String warpName = args[1].toLowerCase();
        Warp warp = scopeMeta.getWarpByName(warpName);
        if (warp == null) {
            sender.sendMessage(ChatColor.RED + "No warp with name: '" + warpName + "' exists!");
            return;
        }

        // Delete warp
        scopeMeta.warps.remove(warp);
        worldManager.scopeManager.saveScopes();
        player.sendMessage(ChatColor.GOLD + "Successfully removed warp: '" + warpName + "' in group: " + scopeMeta.getName() + ".");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return worldManager.scopeManager.getAllGroupNames();
        } else return List.of("<warp name>");
    }
}
