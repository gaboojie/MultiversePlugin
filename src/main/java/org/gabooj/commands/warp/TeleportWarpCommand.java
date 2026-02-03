package org.gabooj.commands.warp;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.gabooj.commands.SubCommand;
import org.gabooj.scope.ScopeMeta;
import org.gabooj.scope.Warp;
import org.gabooj.services.PlayerMoveService;
import org.gabooj.utils.Messager;
import org.gabooj.worlds.WorldManager;
import org.gabooj.worlds.WorldMeta;
import java.util.List;

public class TeleportWarpCommand implements SubCommand {

    private final WorldManager worldManager;

    public TeleportWarpCommand(WorldManager worldManager) {
        this.worldManager = worldManager;
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
        if (warp == null || warp.worldID == null) {
            Messager.sendWarningMessage(player, "No warp exists with the name: '" + warpName + "'!");
            return;
        }

        // If world is unloading
        WorldMeta worldMeta = worldManager.getWorldMetaByID(warp.worldID);
        if (worldMeta == null) {
            Messager.sendWarningMessage(player, "No warps exist with that name.");
            return;
        }

        if (worldMeta.isUnloading) {
            Messager.sendWarningMessage(player, "Uh-oh! The world is currently being unloaded. Try again in 30 seconds.");
            return;
        }

        // Load world if unloaded
        if (!worldMeta.isLoaded()) {
            boolean didWorldLoad = worldManager.loadWorldFromMetaData(worldMeta);
            if (!didWorldLoad) {
                Messager.sendWarningMessage(player, "Uh-oh! The world associated with that warp could not be loaded!");
                return;
            }
        }

        // Get location
        Location loc = new Location(worldMeta.getWorld(), warp.x, warp.y, warp.z, warp.yaw, warp.pitch);
        PlayerMoveService.beginTeleportWarmup(
                player,
                loc,
                PlayerMoveService.DEFAULT_TPA_WARMUP_SECONDS,
                () -> {
                    Messager.sendSuccessMessage(player, "Teleported to warp: " + warp.name + ".");
                },
                () -> Messager.sendWarningMessage(player, "Teleport cancelled.")
        );
        Messager.sendInfoMessage(player, "Teleporting to warp: '" + warp.name + "' in 5 seconds... Do not move.");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            ScopeMeta meta = worldManager.scopeManager.getCurrentPlayerScope(player);
            return meta.warps.stream().map(m -> m.name).toList();
        } else return List.of();
    }
}
