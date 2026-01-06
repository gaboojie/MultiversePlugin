package org.gabooj.commands;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.WorldManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CommandHandler implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final WorldManager worldManager;

    public final List<String> subCommandNames = new ArrayList<>();
    public final List<String> plebSubCommandNames = new ArrayList<>();
    public final Map<String, SubCommand> commands = new HashMap<>();

    public CommandHandler(JavaPlugin plugin, WorldManager worldManager) {
        this.plugin = plugin;
        this.worldManager = worldManager;
        registerCommands();
    }

    public void registerCommands() {
        plugin.getCommand("world").setExecutor(this);

        // Register commands
        register(new CreateWorldCommand(plugin, worldManager, this));
        register(new ListWorldsCommand(plugin, worldManager, this));
        register(new StatusCommand(plugin, worldManager, this));
        register(new DetailsCommand(plugin, worldManager, this));
        register(new PolicyCommand(plugin, worldManager, this));
        register(new DeleteWorldCommand(plugin, worldManager, this));
        register(new LoadWorldCommand(plugin, worldManager, this));
        register(new UnloadWorldCommand(plugin, worldManager, this));
        register(new TeleportWorldCommand(plugin, worldManager, this));
        register(new ImportWorldCommand(plugin, worldManager, this));
    }

    public void register(SubCommand command) {
        commands.put(command.name().toLowerCase(), command);
        subCommandNames.add(command.name().toLowerCase());
        if (!command.needsOp()) {
            plebSubCommandNames.add(command.name().toLowerCase());
        }
        for (String alias : command.aliases()) {
            commands.put(alias.toLowerCase(), command);
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, @NotNull String @NotNull [] args) {
        if (args.length == 1) {
            List<String> subComNames;
            if (sender.isOp()) {
                subComNames = subCommandNames;
            } else {
                subComNames = plebSubCommandNames;
            }
            return subComNames.stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }

        SubCommand sub = commands.get(args[0].toLowerCase());
        if (sub == null) return List.of();
        if (sub.needsOp() && !sender.isOp()) return List.of();

        return sub.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        // Handle /world
        if (args.length == 0) {
            sendWorldInfoCommandTo(sender);
            return true;
        }

        SubCommand sub = commands.get(args[0].toLowerCase());

        // If no command matches, inform the player
        if (sub == null) {
            sender.sendMessage(ChatColor.RED + args[0] + " was not a recognized subcommand. Use /world to see a list of available commands.");
            return true;
        }

        // Check if command requires admin permissions
        if (sub.needsOp() && !sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "You must be an admin to execute this command.");
            return true;
        }

        // Check if command sender needs to be a player
        if (sub.needsToBePlayer() && !(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You must be a player to execute this command!");
            return true;
        }

        // Send description if not enough information given
        if (args.length == 1) {
            sender.sendMessage(ChatColor.GOLD + sub.description(sender));
            return true;
        }

        // Execute command
        String[] remaining = Arrays.copyOfRange(args, 1, args.length);
        sub.execute(sender, remaining);
        return true;
    }

    public void sendWorldInfoCommandTo(CommandSender sender) {
        if (sender.isOp()) {
            StringBuilder opInfo = new StringBuilder("A command to control worlds across a server with /world <subcommand>. Use /world <world name> to teleport to a given world. Possible subcommands are: ");

            // Dynamically create list of subcommands
            for (String commandName : subCommandNames) {
                opInfo.append(commandName).append(", ");
            }
            opInfo = new StringBuilder(opInfo.substring(0, opInfo.length() - 2));
            opInfo.append(".");

            sender.sendMessage(ChatColor.GOLD + opInfo.toString());
        } else {
            String info = """
                    A command to handle teleportation across worlds.
                    Use /world teleport <world name> to teleport to a world.
                    Use '/world list' to view a list of worlds.
                    Use '/world status' to view the status of each world.
                    """;
            sender.sendMessage(ChatColor.GOLD + info);
        }
    }
}