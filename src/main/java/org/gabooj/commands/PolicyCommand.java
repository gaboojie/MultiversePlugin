package org.gabooj.commands;

import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.WorldManager;
import org.gabooj.WorldMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class PolicyCommand implements SubCommand {

    private final JavaPlugin plugin;
    private final WorldManager worldManager;
    private final CommandHandler commandHandler;

    public enum WorldPolicy {

        NAME("Name", List.of("<new name>")),
        VISIBLE("Visible", List.of("true","false")),
        AUTOLOAD("AutoLoad", List.of("true","false")),
        GAMEMODE("GameMode", List.of("creative","survival","spectator","adventure")),
        HARDCORE("Hardcore", List.of("true","false")),
        FORCESPAWN("ForceSpawn", List.of("true","false")),
        DIFFICULTY("Difficulty", List.of("peaceful","easy","normal","hard")),
        SPAWN("Spawn", null);

        private final String name;
        private final List<String> values;

        WorldPolicy(String name, List<String> values) {
            this.name = name;
            this.values = values;
        }

        public String getName() { return name; }
        public List<String> getValues() { return values; }

        public static WorldPolicy fromName(String input) {
            for (WorldPolicy p : values()) {
                if (p.name.equalsIgnoreCase(input)) return p;
            }
            return null;
        }
    }

    public PolicyCommand(JavaPlugin plugin, WorldManager worldManager, CommandHandler commandHandler) {
        this.plugin = plugin;
        this.worldManager = worldManager;
        this.commandHandler = commandHandler;
    }

    @Override
    public String name() {
        return "policy";
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
            A command to change the mutable policies of a world.
            Use /world policy get <world_name> <policy name> to get the value of a policy.
            Use /world policy set <world_name> <policy name> <value> to update a policy.
            ===
            Policy Name | Possible Values
            - Name: Single word name
            - Visible: true/false
            - AutoLoad: true/false
            - GameMode: Creative/Survival/Spectator/Adventure
            - Hardcore: true/false
            - ForceSpawn: true/false
            - Difficulty: Peaceful/Easy/Normal/Hard
            - Spawn: * Sets to your current location *
            """;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // Handle /world policy get
        if (args[0].equalsIgnoreCase("get")) {
            worldPolicyGetCommand(sender, args);
            return;
        }

        // Handle /world policy set
        if (args[0].equalsIgnoreCase("set")) {
            worldPolicySetCommand(sender, args);
            return;
        }

        // Unrecognized command
        sender.sendMessage(ChatColor.RED + args[0] + " is not a recognized command.");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        switch (args.length) {
            case 1:
                return List.of("get", "set");
            case 2:
                return worldManager.getWorldNames().stream().toList();
            case 3:
                return Arrays.stream(WorldPolicy.values()).map(p -> p.name).toList();
            case 4:
                if (!args[0].equalsIgnoreCase("set")) return List.of();

                WorldPolicy policy = WorldPolicy.fromName(args[2]);
                if (policy == null) return List.of();

                List<String> values = policy.getValues();
                if (values == null) return List.of();
                return values;
            default:
                return List.of();
        }
    }

    public void worldPolicySetCommand(CommandSender sender, String[] args) {
        // Handle /world policy set
        if (args.length == 1 || args.length == 2) {
            sender.sendMessage(ChatColor.GOLD + "Use /world policy set <world_name> <policy name> <value>\nPossible policies are: Visible, Name, AutoLoad, GameMode, HardCore, ForceSpawn, Difficulty, & Spawn.");
            return;
        }

        // Get world
        Collection<WorldMeta> worlds = worldManager.worlds.values();
        WorldMeta worldToUse = null;
        for (WorldMeta meta : worlds) {
            if (meta.worldName.equalsIgnoreCase(args[1])) {
                worldToUse = meta;
                break;
            }
        }

        // No world found
        if (worldToUse == null) {
            sender.sendMessage(ChatColor.RED + args[1] + " is not the name of a created world.");
            return;
        }

        // Set respective policy
        setPolicy(sender, args, worldToUse);
    }

    private void setPolicy(CommandSender sender, String[] args, WorldMeta worldToUse) {
        String policy = args[2].toLowerCase();
        if (policy.equalsIgnoreCase("spawn")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "You must be a player to execute this command.");
                return;
            }
            // Update spawn location to player's position
            Location loc = player.getLocation();
            worldToUse.spawnLocX = loc.getX();
            worldToUse.spawnLocY = loc.getY();
            worldToUse.spawnLocZ = loc.getZ();
            worldToUse.spawnLocPitch = loc.getPitch();
            worldToUse.spawnLocYaw = loc.getYaw();
            player.sendMessage(ChatColor.GOLD + "Set spawn to your location.");
            worldManager.saveWorlds(plugin);
        } else {
            // Handle no value given case
            if (args.length < 4) {
                sender.sendMessage(ChatColor.RED + "You need to specify a value for your policy, i.e. /world policy set <world name> <policy name> <policy value>");
                return;
            }
            String value = args[3].toLowerCase();

            // Parse value and update policy
            if (policy.equalsIgnoreCase("gamemode")) {
                try {
                    worldToUse.gameMode = GameMode.valueOf(value.toUpperCase());
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(ChatColor.RED + "GameMode must be creative, survival, adventure, or spectator.");
                    return;
                }
            } else if (policy.equalsIgnoreCase("name")) {
                if (worldManager.doesWorldNameExist(value)) {
                    sender.sendMessage(ChatColor.RED + "That world name is already taken!");
                    return;
                } else {
                    worldToUse.worldName = value;
                }
            } else if (policy.equalsIgnoreCase("hardcore")) {
                worldToUse.doHardcore = Boolean.parseBoolean(value);
            } else if (policy.equalsIgnoreCase("forcespawn")) {
                worldToUse.forceSpawn = Boolean.parseBoolean(value);
            } else if (policy.equalsIgnoreCase("autoload")) {
                worldToUse.autoLoad = Boolean.parseBoolean(value);
            } else if (policy.equalsIgnoreCase("difficulty")) {
                try {
                    worldToUse.difficulty = Difficulty.valueOf(value.toUpperCase());
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(ChatColor.RED + "Difficulty must be peaceful, easy, normal, or hard.");
                    return;
                }
            } else if (policy.equalsIgnoreCase("visible")) {
                worldToUse.visible = Boolean.parseBoolean(value);
            } else {
                sender.sendMessage(ChatColor.RED + "Unknown policy: " + policy + ".");
                return;
            }
            sender.sendMessage(ChatColor.GOLD + "Updated policy.");
            worldManager.saveWorlds(plugin);
        }
    }

    public void worldPolicyGetCommand(CommandSender sender, String[] args) {
        // Handle /world policy get
        if (args.length == 1 || args.length == 2) {
            sender.sendMessage(ChatColor.GOLD + "Use /world policy get <world_name> <policy>\nPossible policies are: Visible, Name, AutoLoad, GameMode, HardCore, ForceSpawn, Difficulty, & Spawn.");
            return;
        }

        // Get world
        Collection<WorldMeta> worlds = worldManager.worlds.values();
        WorldMeta worldToUse = null;
        for (WorldMeta meta : worlds) {
            if (meta.worldName.equalsIgnoreCase(args[1])) {
                worldToUse = meta;
                break;
            }
        }

        // No world found
        if (worldToUse == null) {
            sender.sendMessage(ChatColor.RED + args[1] + " is not the name of a created world.");
            return;
        }

        // Get respective policy
        String message = getPolicy(args, worldToUse);
        sender.sendMessage(message);
    }

    @NotNull
    private static String getPolicy(String[] args, WorldMeta worldToUse) {
        String policy = args[2].toLowerCase();
        String message = "";
        switch (policy) {
            case "visible" -> message = ChatColor.GOLD + "Visible: " + worldToUse.visible;
            case "name" -> message = ChatColor.GOLD + "Name: " + worldToUse.worldName;
            case "autoload" -> message = ChatColor.GOLD + "Auto Load: " + worldToUse.autoLoad;
            case "gamemode" -> message = ChatColor.GOLD + "GameMode: " + worldToUse.gameMode;
            case "hardcore" -> message = ChatColor.GOLD + "Hardcore: " + worldToUse.doHardcore;
            case "forcespawn" -> message = ChatColor.GOLD + "Force Spawn: " + worldToUse.forceSpawn;
            case "difficulty" -> message = ChatColor.GOLD + "Difficulty: " + worldToUse.difficulty;
            case "spawn" -> message = ChatColor.GOLD + "Spawn Location: (" + worldToUse.spawnLocX + ", " + worldToUse.spawnLocY + ", " + worldToUse.spawnLocZ + ") | Yaw: " + worldToUse.spawnLocYaw + " Pitch: " + worldToUse.spawnLocPitch;
            default -> message = ChatColor.RED + "Unknown policy: " + policy + ".";
        }
        return message;
    }

}
