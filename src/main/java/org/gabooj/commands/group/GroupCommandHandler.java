package org.gabooj.commands.group;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.commands.SubCommand;
import org.gabooj.utils.Messager;
import org.gabooj.worlds.WorldManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class GroupCommandHandler implements CommandExecutor, TabCompleter  {

    private final JavaPlugin plugin;
    private final WorldManager worldManager;

    public final Map<String, SubCommand> commands = new HashMap<>();
    public final List<String> subCommandNames = new ArrayList<>();

    public GroupCommandHandler(JavaPlugin plugin, WorldManager worldManager) {
        this.plugin = plugin;
        this.worldManager = worldManager;
        registerCommands();
    }

    public void registerCommands() {
        plugin.getCommand("group").setExecutor(this);

        register(new CreateGroupCommand(worldManager));
        register(new AddGroupCommand(worldManager));
        register(new RemoveGroupCommand(worldManager));
        register(new DeleteGroupCommand(worldManager));
        register(new ListGroupCommand(worldManager));
        register(new ConfigGroupCommand(worldManager));
        register(new GameRuleConfigCommand(worldManager));
    }

    public void register(SubCommand command) {
        commands.put(command.name().toLowerCase(), command);
        subCommandNames.add(command.name().toLowerCase());
        for (String alias : command.aliases()) {
            commands.put(alias.toLowerCase(), command);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!sender.isOp()) {
            Messager.sendWarningMessage(sender, "You must be an admin to execute this command.");
            return true;
        }

        // Handle /group
        if (args.length == 0) {
            sendGroupInfoCommandTo(sender);
            return true;
        }

        SubCommand sub = commands.get(args[0].toLowerCase());

        // If no command matches, inform the player
        if (sub == null) {
            Messager.sendWarningMessage(sender, args[0] + " was not a recognized subcommand. Use /group to see a list of available commands.");
            return true;
        }

        // Check if command sender needs to be a player
        if (sub.needsToBePlayer() && !(sender instanceof Player)) {
            Messager.sendWarningMessage(sender, "You must be a player to execute this command!");
            return true;
        }

        // Send description if not enough information given
        if (args.length == 1) {
            Messager.sendInfoMessage(sender, sub.description(sender));
            return true;
        }

        // Execute command
        String[] remaining = Arrays.copyOfRange(args, 1, args.length);
        sub.execute(sender, remaining);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, @NotNull String @NotNull [] args) {
        if (!sender.isOp()) return List.of();

        if (args.length == 1) {
            return subCommandNames.stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }

        SubCommand sub = commands.get(args[0].toLowerCase());
        if (sub == null) return List.of();
        return sub.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    public void sendGroupInfoCommandTo(CommandSender sender) {
        String msg = """
                A command to manage world groups.
                - Use '/group create <group name>' to make a new group.
                - Use '/group add <group name> <world name>' to add a world to a group.
                - Use '/group remove <group name> <world name>' to remove a world from a group.
                - Use '/group delete <group name>' to delete a group.
                - Use '/group list' to list all groups and their worlds.
                """;
        Messager.sendInfoMessage(sender, msg);
    }
}
