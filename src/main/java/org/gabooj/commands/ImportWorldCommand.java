package org.gabooj.commands;

import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.WorldManager;
import org.gabooj.WorldMeta;

import java.io.File;
import java.util.List;

public class ImportWorldCommand implements SubCommand {

    private final JavaPlugin plugin;
    private final WorldManager worldManager;
    private final CommandHandler commandHandler;

    public ImportWorldCommand(JavaPlugin plugin, WorldManager worldManager, CommandHandler commandHandler) {
        this.plugin = plugin;
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
            sender.sendMessage(ChatColor.RED + "No world folder could be found that exactly matches the name: '" + worldFolderName + "'.");
            return;
        }

        // Ensure that world ID does not already exist
        if (worldManager.doesWorldIDExist(worldFolderName)) {
            sender.sendMessage(ChatColor.RED + "You cannot import world '" + worldFolderName + "' as that world ID is already taken!");
            return;
        }

        // Ensure that world name does not already exist
        if (worldManager.doesWorldNameExist(worldFolderName)) {
            sender.sendMessage(ChatColor.RED + "You cannot import world '" + worldFolderName + "' as that world name is already taken!");
            return;
        }

        // Ensure that world name is not a command
        for (String command : commandHandler.commands.keySet()) {
            if (command.equalsIgnoreCase(worldFolderName)) {
                sender.sendMessage(ChatColor.RED + "You cannot import world '" + worldFolderName + "' as that world name is a command under /world.");
                return;
            }
        }

        // Ensure that world level dat exists
        File worldFolder = new File(Bukkit.getServer().getWorldContainer(), worldFolderName);
        File levelDat = new File(worldFolder, "level.dat");
        if (!levelDat.exists()) {
            sender.sendMessage(ChatColor.RED + "You cannot import the world, because it doesn't contain world data (level.dat)!");
            return;
        }

        // Attempt to load world to see if it exists
        WorldCreator creator = new WorldCreator(worldFolderName);
        World world = creator.createWorld();
        if (world == null) {
            sender.sendMessage(ChatColor.RED + "The server could not import world '" + worldFolderName + "' and cannot provide a good reason why!");
            return;
        }

        WorldMeta.GeneratorType generatorType = WorldMeta.GeneratorType.VANILLA;
        if (args.length > 1) {
            String type = args[1];
            if (type.equalsIgnoreCase("void")) {
                generatorType = WorldMeta.GeneratorType.VOID;
            } else if (!type.equalsIgnoreCase("vanilla")) {
                sender.sendMessage(ChatColor.RED + "You must specify a generator type of VOID or VANILLA to import this world!");
            }
        }

        WorldMeta meta = new WorldMeta(
                false, worldFolderName
        );
        meta.worldName = worldFolderName;
        meta.gameMode = GameMode.CREATIVE;
        meta.difficulty = world.getDifficulty();
        meta.autoLoad = false;
        meta.forceSpawn = false;
        meta.doHardcore = world.isHardcore();
        meta.type = world.getWorldType();
        if (meta.type == null) meta.type = WorldType.NORMAL;
        meta.generator = generatorType;
        meta.seed = world.getSeed();
        meta.status = WorldMeta.Status.LOADED;
        meta.generateStructures = world.canGenerateStructures();
        meta.environment = world.getEnvironment();
        meta.visible = true;

        Location spawn = world.getSpawnLocation();
        meta.spawnLocX = spawn.getX();
        meta.spawnLocY = spawn.getY();
        meta.spawnLocZ = spawn.getZ();
        meta.spawnLocYaw = spawn.getYaw();
        meta.spawnLocPitch = spawn.getPitch();
        worldManager.worlds.put(meta.worldID, meta);
        sender.sendMessage(ChatColor.GOLD + "Successfully imported world.");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("<world folder name>");
        } else return List.of("VOID", "VANILLA");
    }
}
