package org.gabooj.commands.group;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.commands.SubCommand;
import org.gabooj.scope.ScopeMeta;
import org.gabooj.worlds.WorldManager;
import org.gabooj.worlds.WorldMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GameRuleConfigCommand implements SubCommand {

    private final JavaPlugin plugin;
    private final WorldManager worldManager;
    private final GroupCommandHandler commandHandler;

    public GameRuleConfigCommand(JavaPlugin plugin, WorldManager worldManager, GroupCommandHandler commandHandler) {
        this.plugin = plugin;
        this.worldManager = worldManager;
        this.commandHandler = commandHandler;
    }

    @Override
    public String name() {
        return "gamerule";
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
                A command to get or set gamerules for a group of worlds!
                Use '/group gamerule get <group name> <gamerule name>' to get a value.
                Use '/group gamerule set <group name> <gamerule name> <gamerule value>' to set a value.
                """;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // Handle too little args
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


        // Parse GameRule
        String gameRuleName = args[2];
        GameRule<?> gameRule = getGameRuleByName(gameRuleName);
        if (gameRule == null) {
            sender.sendMessage(ChatColor.RED + "No game rule with name: '" + gameRuleName + "' exists!");
            return;
        }

        // Parse get/set
        String action = args[0];
        if (action.equalsIgnoreCase("get")) {
            getGameRuleCommand(sender, scopeMeta, gameRule);
        } else if (action.equalsIgnoreCase("set")) {
            if (args.length < 4) {
                sender.sendMessage(ChatColor.RED + "You must specify a value for the GameRule!");
                return;
            }

            // Parse value
            String valueArg = args[3];
            Object value;
            if (gameRule.getType() == Boolean.class) {
                value = Boolean.parseBoolean(valueArg);
            } else {
                value = Integer.parseInt(valueArg);
            }

            setGameRuleCommand(sender, scopeMeta, gameRule, value);
        } else {
            sender.sendMessage(ChatColor.RED + "Your subcommand must be 'get' or 'set'!");
        }
    }

    @SuppressWarnings("unchecked")
    public <T> void setGameRuleCommand(CommandSender sender, ScopeMeta scopeMeta, GameRule<?> gameRule, Object value) {
        Set<WorldMeta> worldMetas = scopeMeta.getWorlds();
        if (worldMetas.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No worlds exist in '" + scopeMeta.getName() + "', so no game rules exist.");
            return;
        }

        // Cast to correct type
        sender.sendMessage(ChatColor.YELLOW + "Setting GameRule '" + gameRule.getKey() + "' = " + value + " for all worlds in scope '" + scopeMeta.getName() + "'.");
        GameRule<T> typed = (GameRule<T>) gameRule;
        T castValue = (T) value;

        // Update GameRules
        for (WorldMeta worldMeta : worldMetas) {
            if (!worldMeta.isLoaded()) {
                boolean didLoad = worldManager.loadWorldFromMetaData(worldMeta);
                if (!didLoad) {
                    sender.sendMessage(ChatColor.RED + "Could not load world: '" + worldMeta.getWorldID() + "', so it's gamerule could not be updated!");
                    continue;
                }
            }
            try {
                World world = worldMeta.getWorld();
                world.setGameRule(typed, castValue);
                sender.sendMessage(ChatColor.GOLD + "- World: '" + world.getName() + "' set to " + value + ".");
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + "Invalid value for this game rule!");
            }
        }
    }

    public void getGameRuleCommand(CommandSender sender, ScopeMeta scopeMeta, GameRule<?> gameRule) {
        Set<WorldMeta> worldMetas = scopeMeta.getWorlds();
        if (worldMetas.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No worlds exist in '" + scopeMeta.getName() + "', so no game rules exist.");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "GameRule '" + gameRule.getKey() + "' in group '" + scopeMeta.getName() + "':");
        for (WorldMeta worldMeta : worldMetas) {
            World world = worldMeta.getWorld();

            if (world == null) {
                sender.sendMessage(ChatColor.GRAY + "- " + worldMeta.getWorldID() + ": (world not loaded)");
                continue;
            }

            Object value = world.getGameRuleValue(gameRule);
            sender.sendMessage(ChatColor.GOLD + "- " + world.getName() + ChatColor.GOLD + " = " + value);
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
                return getAllGameRuleNames().stream()
                        .filter(name -> name.startsWith(args[2].toLowerCase()))
                        .toList();
            case 4:
                if (!args[0].equalsIgnoreCase("set")) return List.of();
                GameRule<?> rule = getGameRuleByName(args[2]);
                if (rule == null) return List.of();
                if (rule.getType() == Boolean.class) {
                    return List.of("true", "false");
                } else { // Integer gamerule
                    return List.of("0", "1", "2", "3");
                }
            default:
                return List.of();
        }
    }

    public static GameRule<?> getGameRuleByName(String input) {
        NamespacedKey key;

        try {
            key = NamespacedKey.fromString(input);
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (key != null && key.getNamespace().isEmpty()) {
            key = NamespacedKey.minecraft(key.getKey());
        }
        if (key == null) return null;


        Registry<GameRule<?>> gameRules = RegistryAccess.registryAccess().getRegistry(RegistryKey.GAME_RULE);
        if (gameRules == null) return null;

        return gameRules.get(key);
    }

    private List<String> getAllGameRuleNames() {
        Registry<GameRule<?>> gameRules =
                RegistryAccess.registryAccess().getRegistry(RegistryKey.GAME_RULE);

        if (gameRules == null) return List.of();

        List<String> names = new ArrayList<>();

        for (GameRule<?> rule : gameRules) {
            NamespacedKey key = rule.getKey();

            if (key.getNamespace().equals("minecraft")) {
                names.add(key.getKey());
            }
        }

        return names;
    }
}
