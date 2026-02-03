package org.gabooj.commands.group;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.gabooj.commands.SubCommand;
import org.gabooj.scope.ScopeMeta;
import org.gabooj.utils.Messager;
import org.gabooj.worlds.WorldManager;
import org.gabooj.worlds.WorldMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GameRuleConfigCommand implements SubCommand {

    private final WorldManager worldManager;

    public GameRuleConfigCommand(WorldManager worldManager) {
        this.worldManager = worldManager;
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
            Messager.sendInfoMessage(sender, description(sender));
            return;
        }

        // Parse group
        String groupName = args[1];
        if (!worldManager.scopeManager.doesScopeNameExist(groupName)) {
            Messager.sendWarningMessage(sender, "No group with name: '" + groupName + "' exists!");
            return;
        }
        ScopeMeta scopeMeta = worldManager.scopeManager.getScopeByName(groupName);


        // Parse GameRule
        String gameRuleName = args[2];
        GameRule<?> gameRule = getGameRuleByName(gameRuleName);
        if (gameRule == null) {
            Messager.sendWarningMessage(sender, "No game rule with name: '" + gameRuleName + "' exists!");
            return;
        }

        // Parse get/set
        String action = args[0];
        if (action.equalsIgnoreCase("get")) {
            getGameRuleCommand(sender, scopeMeta, gameRule);
        } else if (action.equalsIgnoreCase("set")) {
            if (args.length < 4) {
                Messager.sendWarningMessage(sender, "You must specify a value for the GameRule!");
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
            Messager.sendWarningMessage(sender, "Your subcommand must be 'get' or 'set'!");
        }
    }

    @SuppressWarnings("unchecked")
    public <T> void setGameRuleCommand(CommandSender sender, ScopeMeta scopeMeta, GameRule<?> gameRule, Object value) {
        Set<WorldMeta> worldMetas = scopeMeta.getWorlds();
        if (worldMetas.isEmpty()) {
            Messager.sendWarningMessage(sender, "No worlds exist in '" + scopeMeta.getName() + "', so no game rules exist.");
            return;
        }

        // Cast to correct type
        Messager.sendSuccessMessage(sender, "Setting GameRule '" + gameRule.getKey() + "' = " + value + " for all worlds in scope '" + scopeMeta.getName() + "'.");
        GameRule<T> typed = (GameRule<T>) gameRule;
        T castValue = (T) value;

        // Update GameRules
        for (WorldMeta worldMeta : worldMetas) {
            if (worldMeta.isUnloading) {
                Messager.sendWarningMessage(sender, "Could not update gamerule in world: '" + worldMeta.getWorldID() + "', because it is currently being unloaded!");
                return;
            }

            if (!worldMeta.isLoaded()) {
                boolean didLoad = worldManager.loadWorldFromMetaData(worldMeta);
                if (!didLoad) {
                    Messager.sendWarningMessage(sender, "Could not load world: '" + worldMeta.getWorldID() + "', so it's gamerule could not be updated!");
                    continue;
                }
            }
            try {
                World world = worldMeta.getWorld();
                world.setGameRule(typed, castValue);
                Messager.sendSuccessMessage(sender, "- World: '" + world.getName() + "' set to " + value + ".");
            } catch (IllegalArgumentException e) {
                Messager.sendWarningMessage(sender, "Invalid value for this game rule!");
            }
        }
    }

    public void getGameRuleCommand(CommandSender sender, ScopeMeta scopeMeta, GameRule<?> gameRule) {
        Set<WorldMeta> worldMetas = scopeMeta.getWorlds();
        if (worldMetas.isEmpty()) {
            Messager.sendWarningMessage(sender, "No worlds exist in '" + scopeMeta.getName() + "', so no game rules exist.");
            return;
        }

        Messager.sendSuccessMessage(sender, "GameRule '" + gameRule.getKey() + "' in group '" + scopeMeta.getName() + "':");
        for (WorldMeta worldMeta : worldMetas) {
            World world = worldMeta.getWorld();

            if (world == null) {
                Messager.messageSender(sender, "- " + worldMeta.getWorldID() + ": (world not loaded)", NamedTextColor.GRAY);
                continue;
            }

            Object value = world.getGameRuleValue(gameRule);
            Messager.sendSuccessMessage(sender, "- " + world.getName() + " = " + value);
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
