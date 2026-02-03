package org.gabooj.commands.warp;

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

public class WarpCommandHandler implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final WorldManager worldManager;

    public final Map<String, SubCommand> commands = new HashMap<>();
    public final List<String> subCommandNames = new ArrayList<>();

    public WarpCommandHandler(JavaPlugin plugin, WorldManager worldManager) {
        this.plugin = plugin;
        this.worldManager = worldManager;
        registerCommands();
    }

    public void registerCommands() {
        plugin.getCommand("warp").setExecutor(this);

        register(new CreateWarpCommand(worldManager, this));
        register(new DeleteWarpCommand(worldManager));
        register(new ListWarpComand(worldManager));
        register(new TeleportWarpCommand(worldManager));
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

        // Handle /warp
        if (args.length == 0) {
            sendWarpInfoCommandTo(sender);
            return true;
        }

        SubCommand sub = commands.get(args[0].toLowerCase());

        // If no command matches, inform the player
        if (sub == null) {
            Messager.sendWarningMessage(sender, args[0] + " was not a recognized subcommand. Use /warp to see a list of available commands.");
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

    public void sendWarpInfoCommandTo(CommandSender sender) {
        if (sender.isOp()) {
            String msg = """
                A command to manage warps in world groups.
                Use '/warp create <group name> <warp name>' to make a warp for a group at your current position.
                Use '/warp delete <group name> <warp name>' to delete a warp from a group.
                Use '/warp list' to list all warps in your current world group.
                Use '/warp teleport <warp name>' to teleport to a given warp.
                """;
            Messager.sendInfoMessage(sender, msg);
        } else {
            String msg = """
                A command to teleport to warp locations in your current world.
                Use '/warp list' to list all warps.
                Use '/warp teleport <warp name>' to teleport to a given warp.
                """;
            Messager.sendInfoMessage(sender, msg);
        }
    }
}