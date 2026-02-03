package org.gabooj.commands.world;

import org.bukkit.command.CommandSender;
import org.gabooj.commands.SubCommand;
import org.gabooj.utils.Messager;
import org.gabooj.worlds.WorldManager;
import org.gabooj.worlds.WorldMeta;

import java.util.List;

public class UnloadWorldCommand implements SubCommand {

    private final WorldManager worldManager;

    public UnloadWorldCommand(WorldManager worldManager) {
        this.worldManager = worldManager;
    }

    @Override
    public String name() {
        return "unload";
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
        return "A command to unload a world. Use /world unload <world name> to unload a world.";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // Get world
        WorldMeta meta = worldManager.getWorldMetaByID(args[0]);

        // No world found
        if (meta == null) {
            Messager.sendWarningMessage(sender, args[0] + " is not the name of a world.");
            return;
        }

        // Raise error if trying to unload base world
        if (meta.isBaseWorld()) {
            Messager.sendSevereWarningMessage(sender, "You CANNOT unload the base world.");
            return;
        }

        // Raise error if trying to unload the default world
        WorldMeta defaultWorldMeta = worldManager.getDefaultScopeWorld();
        if (defaultWorldMeta != null) {
            if (meta.getWorldID().equalsIgnoreCase(defaultWorldMeta.getWorldID())) {
                Messager.sendSevereWarningMessage(sender, "The default world must always be loaded!");
                return;
            }
        }

        // Cannot unload an unloading world
        if (meta.isUnloading) {
            Messager.sendWarningMessage(sender, "That world is already being unloaded!");
            return;
        }

        // Cannot unload an unloaded world
        if (!meta.isLoaded()) {
            Messager.sendWarningMessage(sender, "That world is already unloaded!");
            return;
        }

        // Cannot unload a world with players in it
        if (!meta.getWorld().getPlayers().isEmpty()) {
            Messager.sendWarningMessage(sender, "You cannot unload a world if there are players in it!");
            return;
        }

        // Try to unload world
        Messager.sendInfoMessage(sender, "Attempting to unload: '" + meta.getWorldID() + "' world...");
        Runnable onSuccess = () -> Messager.sendSuccessMessage(sender, "Successfully unloaded world: '" + meta.getWorldID() + "'.");
        Runnable onFailure = () -> Messager.sendWarningMessage(sender, "Uh-oh! Could not unload world: '" + meta.getWorldID() + "'.");
        worldManager.unloadWorld(meta, onSuccess, onFailure);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return worldManager.getWorldIDs();
    }
}
