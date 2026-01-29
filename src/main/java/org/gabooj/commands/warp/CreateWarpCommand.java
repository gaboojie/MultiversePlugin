package org.gabooj.commands.warp;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.commands.SubCommand;
import org.gabooj.commands.group.GroupCommandHandler;
import org.gabooj.scope.ScopeMeta;
import org.gabooj.scope.Warp;
import org.gabooj.worlds.WorldManager;

import java.util.List;

public class CreateWarpCommand implements SubCommand {

    private final JavaPlugin plugin;
    private final WorldManager worldManager;
    private final WarpCommandHandler commandHandler;

    public CreateWarpCommand(JavaPlugin plugin, WorldManager worldManager, WarpCommandHandler commandHandler) {
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
        return true;
    }

    @Override
    public String description(CommandSender sender) {
        return "A command to create a new warp at your location. Use '/warp create <warp name>' to make a new warp at your location.";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        // Get scope
        ScopeMeta scopeMeta = worldManager.scopeManager.getScopeForWorld(player.getWorld());

        // Handle warp already exists
        String warpName = args[0].toLowerCase();
        if (scopeMeta.doesWarpExist(warpName)) {
            player.sendMessage(ChatColor.RED + "A warp with name: '" + warpName + "' already exists!");
            return;
        }

        // Handle command already exists
        if(commandHandler.commands.containsKey(warpName)) {
            player.sendMessage(ChatColor.RED + "That is already a name of a warp command!");
            return;
        }

        // Create new warp
        Location loc = player.getLocation();
        Warp warp = new Warp(warpName, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(), loc.getWorld().getName());
        scopeMeta.warps.add(warp);
        worldManager.scopeManager.saveScopes();
        player.sendMessage(ChatColor.GOLD + "Successfully created warp: '" + warpName + "' at your location in group: " + scopeMeta.getName() + ".");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of("<warp name>");
    }
}
