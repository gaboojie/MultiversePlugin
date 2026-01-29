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
import org.gabooj.worlds.WorldMeta;

import java.util.List;
import java.util.Set;

public class TeleportWarpCommand implements SubCommand {

    private final JavaPlugin plugin;
    private final WorldManager worldManager;
    private final WarpCommandHandler commandHandler;

    public TeleportWarpCommand(JavaPlugin plugin, WorldManager worldManager, WarpCommandHandler commandHandler) {
        this.plugin = plugin;
        this.worldManager = worldManager;
        this.commandHandler = commandHandler;
    }

    @Override
    public String name() {
        return "teleport";
    }

    @Override
    public List<String> aliases() {
        return List.of("tp");
    }

    @Override
    public boolean needsOp() {
        return false;
    }

    @Override
    public boolean needsToBePlayer() {
        return true;
    }

    @Override
    public String description(CommandSender sender) {
       return "A command to teleport you to a warp in your world. Use '/warp teleport <warp name>' to teleport to a warp and '/warp list' to list all available warps.";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        String warpName = args[0];

        // Ensure that warp exists
        ScopeMeta meta = worldManager.scopeManager.getCurrentPlayerScope(player);
        Warp warp = meta.getWarpByName(warpName);
        if (warp == null) {
            player.sendMessage(ChatColor.RED + "No warp exists with the name: '" + warp.name + "'!");
            return;
        }

        // Load world if unloaded
        WorldMeta worldMeta = worldManager.getWorldMetaByID(warp.worldID);
        if (!worldMeta.isLoaded()) {
            boolean didWorldLoad = worldManager.loadWorldFromMetaData(worldMeta);
            if (!didWorldLoad) {
                player.sendMessage(ChatColor.RED + "Uh-oh! The world associated with that warp could not be loaded!");
                return;
            }
        }

        // Get location
        Location loc = new Location(worldMeta.getWorld(), warp.x, warp.y, warp.z, warp.yaw, warp.pitch);
        player.teleport(loc);
        player.sendMessage(ChatColor.GOLD + "Teleported to warp: " + warp.name + ".");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            ScopeMeta meta = worldManager.scopeManager.getCurrentPlayerScope(player);
            return meta.warps.stream().map(m -> m.name).toList();
        } else return List.of();
    }
}
