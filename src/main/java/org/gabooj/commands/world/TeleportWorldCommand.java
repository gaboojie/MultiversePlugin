package org.gabooj.commands.world;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.gabooj.commands.SubCommand;
import org.gabooj.scope.ScopeMeta;
import org.gabooj.utils.Messager;
import org.gabooj.worlds.WorldManager;
import org.gabooj.worlds.WorldMeta;

import java.util.List;

public class TeleportWorldCommand implements SubCommand {

    private final WorldManager worldManager;

    public TeleportWorldCommand(WorldManager worldManager) {
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
        return "A command to teleport yourself to a given world via '/world teleport <world name>'. Use /world list to get a list of every world to teleport to.";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // Ensure that sender is player
        if (!(sender instanceof Player player)) {
            Messager.sendWarningMessage(sender, "You must be a player to execute this command!");
            return;
        }

        if (args.length > 1) {
            // IGNORE FLAG -> SEND TO WORLD

            // Ensure that player is OP if manually teleporting to world
            if (!player.isOp()) {
                Messager.sendWarningMessage(player, "You must be an admin to manually teleport to a world using the -i/--ignoreGroup flag!");
                return;
            }

            // Parse flags
            String ignoreFlag = args[1];
            if (!ignoreFlag.equalsIgnoreCase("-i") && !ignoreFlag.equalsIgnoreCase("--ignoreGroup")) {
                Messager.sendWarningMessage(player, "Unknown flag: " + ignoreFlag + ". Use -i or --ignoreGroup to ignore the default group teleport location.");
                return;
            }

            // Verify if world exists
            WorldMeta meta = worldManager.getWorldMetaByID(args[0]);
            if (meta == null) {
                Messager.sendWarningMessage(player, "World '" + args[0] + "' does not exist!");
                return;
            }

            if (meta.isUnloading) {
                Messager.sendWarningMessage(player, "World '" + args[0] + "' is currently being unloaded!");
                return;
            }

            // Teleport player to spawn location of world
            worldManager.worldTeleporter.teleportPlayerToWorldSpawn(player, meta, true, false, true);
        } else {
            // SEND TO SCOPE

            // Verify if scope exists
            ScopeMeta scopeMeta = worldManager.scopeManager.getScopeByName(args[0]);
            if (scopeMeta == null) {
                Messager.sendWarningMessage(sender, "Group '" + args[0] + "' does not exist!");
                return;
            }

            // Teleport player to last known scope in that world
            worldManager.worldTeleporter.teleportPlayerToNewScope(player, scopeMeta, true, false, true, true);
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (sender.isOp()) {
            return worldManager.scopeManager.getAllGroupNames();
        } else {
            return worldManager.scopeManager.getAllVisibleGroupNames();
        }
    }
}
