package org.gabooj.commands.world;

import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.gabooj.commands.SubCommand;
import org.gabooj.utils.Messager;
import org.gabooj.worlds.WorldManager;
import org.gabooj.worlds.WorldMeta;

import java.io.File;
import java.util.List;

public class ImportWorldCommand implements SubCommand {

    private final WorldManager worldManager;
    private final WorldCommandHandler commandHandler;

    public ImportWorldCommand(WorldManager worldManager, WorldCommandHandler commandHandler) {
        this.worldManager = worldManager;
        this.commandHandler = commandHandler;
    }

    @Override
    public String name() {
        return "import";
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
        return "A command to import an already-created world by using '/world import <world folder name> <void/vanilla>' using the world folder directory of the server and 'void/vanilla' to specify if the world generator should be void or vanilla.";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        String worldFolderName = args[0];

        // Check to see if world folder actually exists
        if (!worldManager.doesWorldFileExist(worldFolderName)) {
            Messager.sendWarningMessage(sender, "No world folder could be found that exactly matches the name: '" + worldFolderName + "'.");
            return;
        }

        // Ensure that world ID does not already exist
        if (worldManager.isInvalidName(worldFolderName)) {
            Messager.sendWarningMessage(sender, "You cannot import world '" + worldFolderName + "' as that name is already taken by a group or world!");
            return;
        }

        // Ensure that world ID is not a command
        for (String command : commandHandler.commands.keySet()) {
            if (command.equalsIgnoreCase(worldFolderName)) {
                Messager.sendWarningMessage(sender, "You cannot import world '" + worldFolderName + "' as that world's ID is a command under /world.");
                return;
            }
        }

        // Ensure that world level dat exists
        File worldFolder = new File(Bukkit.getServer().getWorldContainer(), worldFolderName);
        File levelDat = new File(worldFolder, "level.dat");
        if (!levelDat.exists()) {
            Messager.sendWarningMessage(sender, "You cannot import the world, because it doesn't contain world data (level.dat)!");
            return;
        }

        // Attempt to load world to see if it exists
        WorldCreator creator = new WorldCreator(worldFolderName);
        World world = creator.createWorld();
        if (world == null) {
            Messager.sendWarningMessage(sender, "The server could not import world '" + worldFolderName + "' and cannot provide a good reason why!");
            return;
        }

        // Parse optional vanilla/void args
        WorldMeta.GeneratorType generatorType = WorldMeta.GeneratorType.VANILLA;
        if (args.length > 1) {
            String type = args[1];
            if (type.equalsIgnoreCase("void")) {
                generatorType = WorldMeta.GeneratorType.VOID;
            } else if (!type.equalsIgnoreCase("vanilla")) {
                Messager.sendWarningMessage(sender, "You must specify a generator type of VOID or VANILLA to import this world!");
                return;
            }
        }

        @SuppressWarnings("deprecation")
        WorldMeta meta = new WorldMeta(
                false, worldFolderName, world.getEnvironment(), world.getWorldType(), world.getSeed(), world.canGenerateStructures()
        );
        meta.setDoAutoLoad(false);
        meta.setGeneratorType(generatorType);
        meta.setWorld(world);

        worldManager.worldMetas.put(meta.getWorldID(), meta);
        worldManager.saveWorldMetaDatas();
        worldManager.scopeManager.createScope(meta.getWorldID());
        Messager.sendInfoMessage(sender, "Successfully imported world.");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("<world folder name>");
        } else return List.of("VOID", "VANILLA");
    }
}
