package org.gabooj.commands.group;

import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.commands.SubCommand;
import org.gabooj.scope.ScopeMeta;
import org.gabooj.scope.SpawnLocation;
import org.gabooj.worlds.WorldManager;
import org.gabooj.worlds.WorldMeta;

import java.util.Arrays;
import java.util.List;

public class ConfigGroupCommand implements SubCommand {

    private final JavaPlugin plugin;
    private final WorldManager worldManager;
    private final GroupCommandHandler commandHandler;

    public enum GroupConfigPolicy {

        NAME("Name", List.of("<new name>")),
        VISIBLE("Visible", List.of("true","false")),
        GAMEMODE("GameMode", List.of("creative","survival","spectator","adventure")),
        HARDCORE("Hardcore", List.of("true","false")),
        FORCE_SPAWN("ForceSpawn", List.of("true","false")),
        DIFFICULTY("Difficulty", List.of("peaceful","easy","normal","hard")),
        FORCE_DEFAULT_WORLD("ForceDefaultWorld", List.of("true", "false")),
        DEFAULT("Default", null),
        SPAWN("Spawn", null);

        private final String name;
        private final List<String> values;

        GroupConfigPolicy(String name, List<String> values) {
            this.name = name;
            this.values = values;
        }

        public String getName() { return name; }
        public List<String> getValues() { return values; }

        public static GroupConfigPolicy fromName(String input) {
            for (GroupConfigPolicy p : values()) {
                if (p.name.equalsIgnoreCase(input)) return p;
            }
            return null;
        }

    }

    public ConfigGroupCommand(JavaPlugin plugin, WorldManager worldManager, GroupCommandHandler commandHandler) {
        this.plugin = plugin;
        this.worldManager = worldManager;
        this.commandHandler = commandHandler;
    }

    @Override
    public String name() {
        return "config";
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
        return "A command to get or set the config of a group.\nTo get the config of a group, use /group config get <group name> <config name>.\n To set the config of a group, use /group config set <group name> <config name> <config value>";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + description(sender));
            return;
        }

        // Parse group
        String groupName = args[1];
        if (!worldManager.scopeManager.doesScopeNameExist(groupName)) {
            sender.sendMessage(ChatColor.RED + "No group with name: '" + groupName + "' exists!");
            return;
        }
        ScopeMeta scopeMeta = worldManager.scopeManager.getScopeByName(groupName);

        // Parse config name
        String configName = args[2];
        if (ConfigGroupCommand.GroupConfigPolicy.fromName(configName) == null) {
            sender.sendMessage(ChatColor.RED + "No config with name: '" + configName + "' exists!");
            return;
        }

        // Parse action
        String action = args[0];
        if (action.equalsIgnoreCase("get")) {
            handleGetCommand(sender, scopeMeta, configName);
        } else if(action.equalsIgnoreCase("set")) {
            handleSetCommand(sender, scopeMeta, configName, args);
        } else {
            sender.sendMessage(ChatColor.RED + action + " was not a recognized subcommand. Your subcommand must be 'get' or 'set'.");
        }
    }

    public void handleGetCommand(CommandSender sender, ScopeMeta meta, String configName) {
        String messageToSend = ChatColor.GOLD + "";
        switch (configName.toLowerCase()) {
            case "default" -> {
                if (meta.getSpawnLocation().spawnWorldID == null) {
                    messageToSend += "Default world: None";
                } else {
                    messageToSend += "Default world: " + meta.getSpawnLocation().spawnWorldID;
                }
            }
            case "forcedefaultworld" -> messageToSend += "Force Default World: " + worldManager.scopeManager.forceDefaultScope;
            case "visible" -> messageToSend += "Visible: " + meta.isVisible();
            case "name" -> messageToSend += "Name: " + meta.getName();
            case "gamemode" -> messageToSend += "GameMode: " + meta.getGameMode();
            case "hardcore" -> messageToSend += "Hardcore: " + meta.doHardcore();
            case "forcespawn" -> messageToSend += "Force Spawn: " + meta.getSpawnLocation().doForceSpawn;
            case "difficulty" -> messageToSend += "Difficulty: " + meta.getDifficulty();
            case "spawn" -> {
                SpawnLocation loc = meta.getSpawnLocation();
                messageToSend += "Spawn Location: (" + loc.spawnX + ", " + loc.spawnY + ", " + loc.spawnZ + ") | Yaw: " + loc.spawnYaw + " Pitch: " + loc.spawnPitch + " in world: " + loc.spawnWorldID;
            }
            default -> messageToSend = ChatColor.RED + "Unknown config name: " + configName;
        }
        sender.sendMessage(messageToSend);
    }

    public void handleSetCommand(CommandSender sender, ScopeMeta meta, String configName, String[] args) {
        // Handle configs that do not have a value
        if (configName.equalsIgnoreCase(GroupConfigPolicy.DEFAULT.name)) {
            setDefaultServerSpawn(sender, meta);
            return;
        } else if (configName.equalsIgnoreCase(GroupConfigPolicy.SPAWN.name)) {
            setScopeSpawn(sender, meta);
            return;
        }

        // Handle configs that do have a value
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "You must specify a value for your config name!");
            return;
        }
        String configValue = args[3];

        if (configName.equalsIgnoreCase(GroupConfigPolicy.NAME.name)) {
            setName(sender, meta, configName, configValue);
        } else if (configName.equalsIgnoreCase(GroupConfigPolicy.FORCE_DEFAULT_WORLD.name)) {
            setForceDefaultWorld(sender, configValue);
        } else if (configName.equalsIgnoreCase(GroupConfigPolicy.FORCE_SPAWN.name)) {
            setForceSpawnInScope(sender, meta, configValue);
        } else if (configName.equalsIgnoreCase(GroupConfigPolicy.HARDCORE.name)) {
            setDoHardcoreInScope(sender, meta, configValue);
        } else if (configName.equalsIgnoreCase(GroupConfigPolicy.DIFFICULTY.name)) {
            setDifficultyInScope(sender, meta, configValue);
        } else if (configName.equalsIgnoreCase(GroupConfigPolicy.VISIBLE.name)) {
            setVisibilityInScope(sender, meta, configValue);
        } else if (configName.equalsIgnoreCase(GroupConfigPolicy.GAMEMODE.name)) {
            setGameModeInScope(sender, meta, configValue);
        } else {
            sender.sendMessage(ChatColor.RED + "Unknown config name: " + configName);
        }
    }

    private void setGameModeInScope(CommandSender sender, ScopeMeta meta, String value) {
        try {
            GameMode gameMode = GameMode.valueOf(value.toUpperCase());
            meta.setGameMode(gameMode);
            worldManager.scopeManager.saveScopes();
            sender.sendMessage(ChatColor.GOLD + "Set GameMode in scope: '" + meta.getName() + "' to " + gameMode + ".");
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "GameMode must be creative, survival, adventure, or spectator.");
        }
    }

    private void setVisibilityInScope(CommandSender sender, ScopeMeta meta, String value) {
        boolean isVisible = Boolean.parseBoolean(value);
        meta.setVisible(isVisible);
        worldManager.scopeManager.saveScopes();
        sender.sendMessage(ChatColor.GOLD + "Set visibility in scope: '" + meta.getName() + "' to " + isVisible + ".");
    }

    private void setDifficultyInScope(CommandSender sender, ScopeMeta meta, String value) {
        try {
            Difficulty difficulty = Difficulty.valueOf(value.toUpperCase());
            meta.setDifficulty(difficulty);
            worldManager.scopeManager.saveScopes();
            sender.sendMessage(ChatColor.GOLD + "Set difficulty in scope: '" + meta.getName() + "' to " + difficulty + ".");
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Difficulty must be peaceful, easy, normal, or hard.");
        }
    }

    private void setDoHardcoreInScope(CommandSender sender, ScopeMeta meta, String value) {
        boolean doHardcore = Boolean.parseBoolean(value);
        meta.setDoHardcore(doHardcore);
        worldManager.scopeManager.saveScopes();
        sender.sendMessage(ChatColor.GOLD + "Set do hardcore in scope: '" + meta.getName() + "' to " + doHardcore + ".");
    }

    private void setForceSpawnInScope(CommandSender sender, ScopeMeta meta, String value) {
        boolean forceSpawn = Boolean.parseBoolean(value);
        meta.getSpawnLocation().doForceSpawn = forceSpawn;
        worldManager.scopeManager.saveScopes();
        sender.sendMessage(ChatColor.GOLD + "Set force spawn in scope: '" + meta.getName() + "' to " + forceSpawn + ".");
    }

    private void setForceDefaultWorld(CommandSender sender, String value) {
        // Try to load the default world if set to true
        boolean forceDefaultScope = Boolean.parseBoolean(value);
        if (forceDefaultScope) {
            // Ensure that spawn exists in the default world
            WorldMeta defaultWorldMeta = worldManager.getDefaultScopeWorld();
            if (defaultWorldMeta == null) {
                sender.sendMessage(ChatColor.DARK_RED + "The current default world for the server: '" + worldManager.scopeManager.defaultScopeID + "' does not have a spawn location. Set a spawn location before you enable ForceDefaultWorld!");
                return;
            }

            // Try to load world
            if (!defaultWorldMeta.isLoaded()) {
                boolean didLoadWorld = worldManager.loadWorldFromMetaData(defaultWorldMeta);
                if (!didLoadWorld) {
                    sender.sendMessage(ChatColor.DARK_RED + "For some reason, the default world for that scope could not be loaded, so it could not be set as the default world.");
                    return;
                }
            }
            defaultWorldMeta.setDoAutoLoad(true);
        }
        worldManager.scopeManager.forceDefaultScope = forceDefaultScope;
        worldManager.saveWorldMetaDatas();
        sender.sendMessage(ChatColor.GOLD + "Set force default world to " + forceDefaultScope + ".");
    }

    private void setScopeSpawn(CommandSender sender, ScopeMeta meta) {
        // Ensure that player is using this world
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "You must be a player to execute this command!");
            return;
        }

        // Ensure that the current world is in the scope
        WorldMeta worldMetaToUse = worldManager.getWorldMetaByID(player.getWorld().getName());
        if (!meta.getWorlds().contains(worldMetaToUse)) {
            sender.sendMessage(ChatColor.RED + "You must be in a world of the group: '" + meta.getName() + "' to update it's spawn location!");
            return;
        }

        Location loc = player.getLocation();
        SpawnLocation spawnLocation = new SpawnLocation(meta.getSpawnLocation().doForceSpawn, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(), loc.getWorld().getName());
        meta.setSpawnLocation(spawnLocation);
        player.sendMessage(ChatColor.GOLD + "Set spawn to your current location and orientation for: " + meta.getName() + ".");
        worldManager.scopeManager.saveScopes();
    }

    private void setDefaultServerSpawn(CommandSender sender, ScopeMeta meta) {
        // If no spawn is set for the scope, tell the player
        SpawnLocation loc = meta.getSpawnLocation();
        if (loc.spawnWorldID == null) {
            sender.sendMessage(ChatColor.RED + "There is no default spawn location for group: " + meta.getName() + ". There needs to be one to set the server's default spawn to here!");
            return;
        }

        // Load world if unloaded
        if (worldManager.scopeManager.forceDefaultScope) {
            WorldMeta spawnScopeMeta = worldManager.getWorldMetaByID(loc.spawnWorldID);
            spawnScopeMeta.setDoAutoLoad(true);
            if (!spawnScopeMeta.isLoaded()) {
                boolean didWorldLoad = worldManager.loadWorldFromMetaData(spawnScopeMeta);
                if (!didWorldLoad) {
                    sender.sendMessage(ChatColor.RED + "For some reason, the soon-to-be default world in scope: '" + meta.getName() + "' (World: " + spawnScopeMeta.getWorldID() + ") could not be loaded, so the default world could not be changed to this world!");
                    return;
                }
            }
        }

        // Set default scope
        worldManager.scopeManager.defaultScopeID = meta.getScopeId();
        worldManager.saveWorldMetaDatas();
        sender.sendMessage(ChatColor.GOLD + "Updated default scope to '" + meta.getName() + "'.");
    }

    private void setName(CommandSender sender, ScopeMeta meta, String configName, String value) {
        if (worldManager.isInvalidName(value)) {
            sender.sendMessage(ChatColor.RED + "That name is already taken!");
        } else {
            meta.setName(value);
            worldManager.scopeManager.saveScopes();
            sender.sendMessage(ChatColor.GOLD + "Updated scope: '" + meta.getName() + "' with new name: " + value);
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        switch (args.length) {
            case 1:
                return List.of("get", "set");
            case 2:
                return worldManager.scopeManager.getAllGroupNames();
            case 3:
                return Arrays.stream(ConfigGroupCommand.GroupConfigPolicy.values()).map(p -> p.name).toList();
            case 4:
                if (!args[0].equalsIgnoreCase("set")) return List.of();

                ConfigGroupCommand.GroupConfigPolicy policy = ConfigGroupCommand.GroupConfigPolicy.fromName(args[2]);
                if (policy == null) return List.of();

                List<String> values = policy.getValues();
                if (values == null) return List.of();
                return values;
            default:
                return List.of();
        }
    }
}
