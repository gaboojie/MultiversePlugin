package org.gabooj.commands;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.WorldManager;
import org.gabooj.WorldMeta;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CreateWorldCommand implements SubCommand {

    private final JavaPlugin plugin;
    private final WorldManager worldManager;
    private final CommandHandler commandHandler;

    public enum CreateFlag {
        ENV("-env", "-e", List.of("NORMAL", "NETHER", "END")),
        TYPE("-type", "-t", List.of("NORMAL", "FLAT", "LARGE_BIOMES", "AMPLIFIED")),
        GENERATOR("-generator", "-g", List.of("VANILLA", "VOID")),
        STRUCTURES("-doStructures", "-d", List.of("true", "false")),
        SEED("-seed", "-s", List.of("<seed>"));

        private final String key;
        private final String alias;
        private final List<String> values;

        CreateFlag(String key, String alias, List<String> values) {
            this.key = key;
            this.alias = alias;
            this.values = values;
        }

        public String getKey() {
            return key;
        }
        public List<String> getValues() {
            return values;
        }

        public static CreateFlag fromInput(String key) {
            for (CreateFlag flag : values()) {
                if (flag.key.equalsIgnoreCase(key)) return flag;
                if (flag.alias.equalsIgnoreCase(key)) return flag;
            }
            return null;
        }
    }

    public CreateWorldCommand(JavaPlugin plugin, WorldManager worldManager, CommandHandler commandHandler) {
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
        return false;
    }

    @Override
    public String description(CommandSender sender) {
        return """
        A command to create a new world. Usage:
        /world create <world_id> (id not world name)
        With the following flags:
        -env Normal/Nether/End
        -type Normal/Flat/Large_Biomes/Amplified
        -generator Vanilla/Void
        -seed <seed>
        -structures <boolean>
        """;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        String worldID = args[0];

        // Ensure that world ID does not already exist
        if (worldManager.doesWorldIDExist(worldID)) {
            sender.sendMessage(ChatColor.RED + "You cannot create world '" + worldID + "' as that world ID is already taken!");
            return;
        }

        // Ensure that world name does not already exist
        if (worldManager.doesWorldNameExist(worldID)) {
            sender.sendMessage(ChatColor.RED + "You cannot create world '" + worldID + "' as that world name is already taken!");
            return;
        }

        // Ensure that world name is not a command
        for (String command : commandHandler.commands.keySet()) {
            if (command.equalsIgnoreCase(worldID)) {
                sender.sendMessage(ChatColor.RED + "You cannot create world '" + worldID + "' as that world name is a command under /world.");
                return;
            }
        }

        // Ensure that world does not exist
        if (worldManager.doesWorldFileExist(worldID)) {
            sender.sendMessage(ChatColor.RED + "A world folder by the name '" + worldID + "' already exists! If you're trying to import a new world, use /world import!");
            return;
        }

        // Create default world
        WorldMeta meta = new WorldMeta(false, worldID);

        // Parse flags
        try {
            parseFlags(meta, args, 1);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
            return;
        }

        // If invalid types specified, let the player know and quit
        if (meta.environment != World.Environment.NORMAL && meta.type != WorldType.NORMAL) {
            sender.sendMessage(ChatColor.RED + "You can only specify a NORMAL world type with a different environment (i.e END/NETHER).");
            return;
        }
        if (meta.generator == WorldMeta.GeneratorType.VOID && meta.type != WorldType.NORMAL) {
            sender.sendMessage(ChatColor.RED + "You can only specify a NORMAL world type with a void generator (i.e. not AMPLIFIED/LARGE BIOMES/FLAT).");
            return;
        }

        // Create new world
        worldManager.worlds.put(meta.worldID, meta);
        boolean didWorldLoad = worldManager.loadWorld(meta);
        if (didWorldLoad) {
            sender.sendMessage(ChatColor.GOLD + "Successfully created new world: " + worldID + ".");
        } else {
            sender.sendMessage(ChatColor.RED + "Uh-oh! Could not create new world: '" + meta.worldID + "'.");
            worldManager.worlds.remove(meta.worldID);
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("<world name>");
        }
        int last = args.length - 1;

        // If cursor is at flag position, then suggest values for the flag
        CreateFlag flag = CreateFlag.fromInput(args[last - 1]);
        if (flag != null) {
            return flag.getValues().stream().toList();
        }

        // Otherwise, suggest which flags should be used
        Set<CreateFlag> used = new HashSet<>();
        for (int i = 1; i < last; i++) {
            CreateFlag f = CreateFlag.fromInput(args[i]);
            if (f != null) {
                used.add(f);
            }
        }
        return Arrays.stream(CreateFlag.values()).filter(f -> !used.contains(f)).map(CreateFlag::getKey).toList();
    }

    public void parseFlags(WorldMeta meta, String[] args, int startIndex) {
        for (int i = startIndex; i < args.length; i++) {
            String rawFlag = args[i];

            if (!rawFlag.startsWith("-")) {
                throw new IllegalArgumentException("Could not interpret flag: " + rawFlag + ", so no world was created.");
            }

            CreateFlag flag = CreateFlag.fromInput(rawFlag);
            if (flag == null) {
                throw new IllegalArgumentException("Could not interpret flag: " + rawFlag + ", so no world was created.");
            }

            if (i + 1 >= args.length) {
                throw new IllegalArgumentException("Missing value for flag: " + flag + ", so no world was created.");
            }

            String value = args[++i];
            switch (flag) {
                case ENV -> {
                    switch (value.toUpperCase()) {
                        case "END" -> meta.environment = World.Environment.THE_END;
                        case "NETHER" -> meta.environment = World.Environment.NETHER;
                        case "NORMAL" -> meta.environment = World.Environment.NORMAL;
                        default -> throw new IllegalArgumentException("Could not interpret environment value: " + value);
                    }
                }
                case TYPE -> {
                    try {
                        meta.type = WorldType.valueOf(value.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Could not interpret world type value: " + value);
                    }
                }
                case GENERATOR -> {
                    try {
                        meta.generator = WorldMeta.GeneratorType.valueOf(value.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Could not interpret generator value: " + value);
                    }
                }
                case SEED -> {
                    try {
                        meta.seed = Long.parseLong(value);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Seed must be a number");
                    }
                }
                case STRUCTURES -> meta.generateStructures = Boolean.parseBoolean(value);
                default -> throw new IllegalArgumentException("Unknown flag: " + flag + ", so no world was created.");
            }
        }
    }
}
