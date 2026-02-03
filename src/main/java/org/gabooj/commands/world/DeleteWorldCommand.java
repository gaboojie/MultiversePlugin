package org.gabooj.commands.world;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.gabooj.commands.SubCommand;
import org.gabooj.scope.ScopeMeta;
import org.gabooj.utils.Messager;
import org.gabooj.worlds.WorldManager;
import org.gabooj.worlds.WorldMeta;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;

public class DeleteWorldCommand implements SubCommand {

    private final WorldManager worldManager;

    public DeleteWorldCommand(WorldManager worldManager) {
        this.worldManager = worldManager;
    }

    @Override
    public String name() {
        return "delete";
    }

    @Override
    public List<String> aliases() {
        return List.of("remove");
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
        return "A command to delete a world by /world delete <world name>. This does not delete the data, but deletes the plugin's ability to recognize the world. You cannot delete the default world.";
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

        // Unload world before deleting
        if (meta.isLoaded()) {
            Messager.sendWarningMessage(sender, "The world must be unloaded before you can delete it!");
            return;
        }

        // Raise error if trying to delete base world
        if (meta.isBaseWorld()) {
            Messager.sendSevereWarningMessage(sender, "You CANNOT delete the base world.");
            return;
        }

        // Raise error if trying to delete the default world spawn
        String defaultScopeID = worldManager.scopeManager.defaultScopeID;
        if (defaultScopeID != null) {
            ScopeMeta defaultScopeMeta = worldManager.scopeManager.getScopeByID(defaultScopeID);
            if (defaultScopeMeta != null && defaultScopeMeta.getSpawnLocation().spawnWorldID != null && defaultScopeMeta.getSpawnLocation().spawnWorldID.equals(meta.getWorldID())) {
                Messager.sendWarningMessage(sender, "You cannot delete a world that is set as the default server spawn! Change the spawn location and then delete the world!");
                return;
            }
        }

        // If world unloading, do not delete
        if (meta.isUnloading) {
            Messager.sendWarningMessage(sender, "The world is currently being unloaded, so you cannot delete it (yet)!");
            return;
        }

        // Delete folder and contents
        try {
            File worldDir = new File(
                    Bukkit.getWorldContainer(),
                    meta.getWorldID()
            ).getCanonicalFile();

            Files.walk(worldDir.toPath())
                    .sorted(Comparator.reverseOrder()) // children first
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            Messager.sendWarningMessage(sender, "For some reason, there was an IO exception in trying to delete the files in the folder. Aborting...");
                        }
                    });
        } catch (Exception e) {
            Messager.sendWarningMessage(sender, "For some reason, there was an IO exception in trying to delete the files in the folder. Aborting...");
            return;
        }

        // Detach any groups
        if (meta.getScopeID() != null) {
            ScopeMeta scopeMeta = worldManager.scopeManager.getScopeByID(meta.getScopeID());
            scopeMeta.removeWorld(meta);

            // Remove scope spawn if set to this world
            if (scopeMeta.getSpawnLocation().spawnWorldID != null && scopeMeta.getSpawnLocation().spawnWorldID.equals(meta.getWorldID())) {
                scopeMeta.getSpawnLocation().spawnWorldID = null;
            }

            // Delete group if scope has no more worlds and meta's scope ID is this world's scope ID
            if (scopeMeta.getWorlds().isEmpty()) {
                if (scopeMeta.getScopeId().equals(meta.getWorldID())) {
                    worldManager.scopeManager.removeScope(scopeMeta, false);
                }
            }

            // Ensure that world meta no longer has scope ID and save
            meta.setScopeID(null);
            worldManager.scopeManager.saveScopes();
        }

        Messager.sendSuccessMessage(sender, "Successfully deleted world.");
        worldManager.worldMetas.remove(meta.getWorldID());
        worldManager.saveWorldMetaDatas();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return worldManager.getWorldIDs();
    }
}
