package org.gabooj.commands.world;

import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.gabooj.commands.SubCommand;
import org.gabooj.utils.Messager;
import org.gabooj.worlds.WorldManager;
import org.gabooj.worlds.WorldMeta;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CreateWorldCommand implements SubCommand {

    private final WorldManager worldManager;
    private final WorldCommandHandler commandHandler;

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

    public class FlagArguments {

        public World.Environment environment = World.Environment.NORMAL;
        public WorldType worldType = WorldType.NORMAL;
        public WorldMeta.GeneratorType generatorType = WorldMeta.GeneratorType.VANILLA;
        public boolean doStructures = true;
        public long seed = 0;

        public FlagArguments() {}
    }

    public CreateWorldCommand(WorldManager worldManager, WorldCommandHandler commandHandler) {
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
        if (worldManager.isInvalidName(worldID)) {
            Messager.sendWarningMessage(sender, "You cannot create world '" + worldID + "' as that world ID is already taken with another world or group!");
            return;
        }

        // Ensure that world ID is not a command
        for (String command : commandHandler.commands.keySet()) {
            if (command.equalsIgnoreCase(worldID)) {
                Messager.sendWarningMessage(sender, "You cannot create world '" + worldID + "' as that world name is a command under /world.");
                return;
            }
        }

        // Ensure that world does not exist
        if (worldManager.doesWorldFileExist(worldID)) {
            Messager.sendWarningMessage(sender, "A world folder by the name '" + worldID + "' already exists! If you're trying to import a new world, use /world import!");
            return;
        }

        // Parse flags
        FlagArguments flagArguments = new FlagArguments();
        try {
            parseFlags(flagArguments, args, 1);
        } catch (IllegalArgumentException e) {
            Messager.sendWarningMessage(sender, e.getMessage());
            return;
        }

        // If invalid types specified, let the player know and quit
        if (flagArguments.environment != World.Environment.NORMAL && flagArguments.worldType != WorldType.NORMAL) {
            Messager.sendWarningMessage(sender, "You can only specify a NORMAL world type with a different environment (i.e END/NETHER).");
            return;
        }
        if (flagArguments.generatorType == WorldMeta.GeneratorType.VOID && flagArguments.worldType != WorldType.NORMAL) {
            Messager.sendWarningMessage(sender, "You can only specify a NORMAL world type with a void generator (i.e. not AMPLIFIED/LARGE BIOMES/FLAT).");
            return;
        }

        // Create meta data
        WorldMeta meta = new WorldMeta(
             false, worldID, flagArguments.environment, flagArguments.worldType, flagArguments.seed, flagArguments.doStructures
        );
        meta.setGeneratorType(flagArguments.generatorType);

        // Create new world
        boolean didWorldLoad = worldManager.loadWorldFromMetaData(meta);
        if (didWorldLoad) {
            Messager.sendSuccessMessage(sender, "Successfully created new world: " + worldID + ".");
            worldManager.worldMetas.put(worldID, meta);

            // Create new scope for this world
            worldManager.scopeManager.createScope(worldID);

            // Create spawn platform for void worlds
            if (meta.getGeneratorType() == WorldMeta.GeneratorType.VOID) {
                setUpVoidWorld(meta);
            }
        } else {
            Messager.sendWarningMessage(sender, "Uh-oh! Could not create new world: '" + meta.getWorldID() + "'.");
        }
    }

    public void setUpVoidWorld(WorldMeta worldMeta) {
        World world = worldMeta.getWorld();
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                world.getBlockAt(x, 63, z).setType(Material.BEDROCK);
            }
        }
        world.setSpawnLocation(0, 67, 0);
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

    public void parseFlags(FlagArguments flagArguments, String[] args, int startIndex) {
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
                        case "END" -> flagArguments.environment = World.Environment.THE_END;
                        case "NETHER" -> flagArguments.environment = World.Environment.NETHER;
                        case "NORMAL" -> flagArguments.environment = World.Environment.NORMAL;
                        default -> throw new IllegalArgumentException("Could not interpret environment value: " + value);
                    }
                }
                case TYPE -> {
                    try {
                        flagArguments.worldType = WorldType.valueOf(value.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Could not interpret world type value: " + value);
                    }
                }
                case GENERATOR -> {
                    try {
                        flagArguments.generatorType = WorldMeta.GeneratorType.valueOf(value.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Could not interpret generator value: " + value);
                    }
                }
                case SEED -> {
                    try {
                        flagArguments.seed = Long.parseLong(value);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Seed must be a number");
                    }
                }
                case STRUCTURES -> flagArguments.doStructures = Boolean.parseBoolean(value);
                default -> throw new IllegalArgumentException("Unknown flag: " + flag + ", so no world was created.");
            }
        }
    }
}
