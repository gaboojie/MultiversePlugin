package org.gabooj.commands.world;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.commands.SubCommand;
import org.gabooj.scope.ScopeMeta;
import org.gabooj.worlds.WorldManager;
import org.gabooj.worlds.WorldMeta;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;

public class DeleteWorldCommand implements SubCommand {

    private final JavaPlugin plugin;
    private final WorldManager worldManager;
    private final WorldCommandHandler commandHandler;

    public DeleteWorldCommand(JavaPlugin plugin, WorldManager worldManager, WorldCommandHandler commandHandler) {
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
            sender.sendMessage(ChatColor.RED + args[0] + " is not the name of a created world.");
            return;
        }

        // Raise error if trying to delete base world
        if (meta.isBaseWorld()) {
            sender.sendMessage(ChatColor.DARK_RED + "You CANNOT delete the base world.");
            return;
        }

        // Ensure that player is not trying to delete any world that they are currently in
        if (sender instanceof Player player) {
            if (player.getWorld().getName().equals(meta.getWorldID())) {
                sender.sendMessage(ChatColor.DARK_RED + "You CANNOT delete a world you are currently in!");
                return;
            }
        }

        // Raise error if trying to delete the default world spawn
        String defaultScopeID = worldManager.scopeManager.defaultScopeID;
        if (defaultScopeID != null) {
            ScopeMeta defaultScopeMeta = worldManager.scopeManager.getScopeByID(defaultScopeID);
            if (defaultScopeMeta != null && defaultScopeMeta.getSpawnLocation().spawnWorldID != null && defaultScopeMeta.getSpawnLocation().spawnWorldID.equals(meta.getWorldID())) {
                sender.sendMessage(ChatColor.RED + "You cannot delete a world that is set as the default server spawn! Change the spawn location and then delete the world!");
                return;
            }
        }

        // Unload world before deleting
        if (meta.isLoaded()) {
            boolean didWorldUnload = worldManager.unloadWorld(meta);
            if (!didWorldUnload) {
                sender.sendMessage(ChatColor.RED + "Uh-oh! For some reason the world could not be unloaded, so the world was not deleted.");
                return;
            }
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
                            sender.sendMessage(ChatColor.RED + "For some reason, there was an IO exception in trying to delete the files in the folder. Aborting...");
                        }
                    });
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "For some reason, there was an IO exception in trying to delete the files in the folder. Aborting...");
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

        sender.sendMessage(ChatColor.GOLD + "Successfully deleted world.");
        worldManager.worldMetas.remove(meta.getWorldID());
        worldManager.saveWorldMetaDatas();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return worldManager.getWorldIDs();
    }
}
