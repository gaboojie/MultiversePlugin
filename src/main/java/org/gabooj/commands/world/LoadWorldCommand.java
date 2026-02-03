package org.gabooj.commands.world;

import org.bukkit.command.CommandSender;
import org.gabooj.commands.SubCommand;
import org.gabooj.utils.Messager;
import org.gabooj.worlds.WorldManager;
import org.gabooj.worlds.WorldMeta;

import java.util.List;

public class LoadWorldCommand implements SubCommand {

    private final WorldManager worldManager;

    public LoadWorldCommand(WorldManager worldManager) {
        this.worldManager = worldManager;
    }

    @Override
    public String name() {
        return "load";
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
        return "A command to load a world. Use /world load <world name> to load a world.";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // Get world
        WorldMeta meta = worldManager.getWorldMetaByID(args[0]);

        // No world found
        if (meta == null) {
            Messager.sendWarningMessage(sender, args[0] + " is not the name of a created world.");
            return;
        }

        // Raise error if trying to load base world
        if (meta.isBaseWorld()) {
            Messager.sendSevereWarningMessage(sender, "The base world is always loaded!");
            return;
        }

        // Raise error if being unloaded
        if (meta.isUnloading) {
            Messager.sendWarningMessage(sender, "That world is currently being unloaded!");
            return;
        }

        // Raise error if already loaded
        if (meta.isLoaded()) {
            Messager.sendWarningMessage(sender, "This world is already loaded!");
            return;
        }

        boolean didWorldLoad = worldManager.loadWorldFromMetaData(meta);
        if (didWorldLoad) {
            Messager.sendSuccessMessage(sender, "Successfully loaded world: '" + meta.getWorldID() + "'.");
        } else {
            Messager.sendWarningMessage(sender, "Uh-oh! Could not load world: '" + meta.getWorldID() + "'!");
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return worldManager.getWorldIDs();
    }
}
