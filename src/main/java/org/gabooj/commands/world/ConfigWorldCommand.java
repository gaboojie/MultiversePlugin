package org.gabooj.commands.world;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.commands.SubCommand;
import org.gabooj.worlds.WorldManager;
import org.gabooj.worlds.WorldMeta;

import java.util.*;

public class ConfigWorldCommand implements SubCommand {

    private final JavaPlugin plugin;
    private final WorldManager worldManager;
    private final WorldCommandHandler commandHandler;

    public enum WorldConfigPolicy {

        AUTOLOAD("AutoLoad", List.of("true", "false"));

        private final String name;
        private final List<String> values;

        WorldConfigPolicy(String name, List<String> values) {
            this.name = name;
            this.values = values;
        }

        public String getName() { return name; }
        public List<String> getValues() { return values; }

        public static ConfigWorldCommand.WorldConfigPolicy fromName(String input) {
            for (ConfigWorldCommand.WorldConfigPolicy p : values()) {
                if (p.name.equalsIgnoreCase(input)) return p;
            }
            return null;
        }
    }

    public ConfigWorldCommand(JavaPlugin plugin, WorldManager worldManager, WorldCommandHandler commandHandler) {
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
        return "A command to get or set the config of a world.\nTo get the config of a world, use /world config get <world> <config name>.\n To set the config of a world, use /world config set <world> <config name> <config value>";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + description(sender));
            return;
        }

        // Parse world
        String worldID = args[1];
        if (!worldManager.doesWorldIDExist(worldID)) {
            sender.sendMessage(ChatColor.RED + "No world with ID: '" + worldID + "' exists!");
            return;
        }
        WorldMeta meta = worldManager.getWorldMetaByID(worldID);

        // Parse config name
        String configName = args[2];
        if (WorldConfigPolicy.fromName(configName) == null) {
            sender.sendMessage(ChatColor.RED + "No config with name: '" + configName + "' exists!");
            return;
        }

        // Parse action
        String action = args[0];
        if (action.equalsIgnoreCase("get")) {
            handleGetCommand(sender, meta, configName);
        } else if(action.equalsIgnoreCase("set")) {
            if (args.length < 4) {
                sender.sendMessage(ChatColor.RED + "You must specify a value for your config name!");
                return;
            }
            String configValue = args[3];
            handleSetCommand(sender, meta, configName, configValue);
        } else {
            sender.sendMessage(ChatColor.RED + action + " was not a recognized subcommand. Your subcommand must be 'get' or 'set'.");
        }
    }

    public void handleGetCommand(CommandSender sender, WorldMeta meta, String configName) {
        if (configName.equalsIgnoreCase(WorldConfigPolicy.AUTOLOAD.name)) {
            boolean doAutoLoad = meta.doAutoLoad();
            String msg = ChatColor.GOLD + "DoAutoLoad for world: '" + meta.getWorldID() + "' is set to " + doAutoLoad + ".";
            sender.sendMessage(msg);
        } else {
            sender.sendMessage(ChatColor.RED + configName + " was not a recognized config name!");
        }
    }

    public void handleSetCommand(CommandSender sender, WorldMeta meta, String configName, String newValue) {
        if (configName.equalsIgnoreCase(WorldConfigPolicy.AUTOLOAD.name)) {
            // Handle changing default world autoload
            WorldMeta defaultWorldMeta = worldManager.getDefaultScopeWorld();
            if (defaultWorldMeta != null) {
                if (meta.getWorldID().equalsIgnoreCase(defaultWorldMeta.getWorldID())) {
                    sender.sendMessage(ChatColor.RED + "The default scope's world must always be loaded, so you cannot set it's autoload value!");
                    return;
                }
            }

            // Handle base world autoload
            if (meta.isBaseWorld()) {
                sender.sendMessage(ChatColor.DARK_RED + "All base worlds are always loaded, so you cannot set it's autoload value!");
                return;
            }

            // Update value
            boolean doAutoLoad = Boolean.parseBoolean(newValue);
            meta.setDoAutoLoad(doAutoLoad);
            String msg = ChatColor.GOLD + "DoAutoLoad for world: '" + meta.getWorldID() + "' is now set to " + doAutoLoad + ".";
            sender.sendMessage(msg);
            worldManager.saveWorldMetaDatas();
        } else {
            sender.sendMessage(ChatColor.RED + configName + " was not a recognized config name!");
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        switch (args.length) {
            case 1:
                return List.of("get", "set");
            case 2:
                return worldManager.getWorldIDs();
            case 3:
                return Arrays.stream(WorldConfigPolicy.values()).map(p -> p.name).toList();
            case 4:
                if (!args[0].equalsIgnoreCase("set")) return List.of();

                WorldConfigPolicy policy = WorldConfigPolicy.fromName(args[2]);
                if (policy == null) return List.of();

                List<String> values = policy.getValues();
                if (values == null) return List.of();
                return values;
            default:
                return List.of();
        }
    }
}
