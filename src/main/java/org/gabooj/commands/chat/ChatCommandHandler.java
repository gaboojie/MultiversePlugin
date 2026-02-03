package org.gabooj.commands.chat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.commands.SubCommand;
import org.gabooj.utils.Messager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ChatCommandHandler implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;

    public final Map<String, SubCommand> commands = new HashMap<>();
    public final List<String> subCommandNames = new ArrayList<>();

    public ChatCommandHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        registerCommands();
    }

    public void registerCommands() {
        plugin.getCommand("chat").setExecutor(this);

        register(new ColorCommand());
        register(new FormatCommand());
        register(new NickCommand());
        register(new PrefixCommand());
        register(new SuffixCommand());
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

        // Handle /chat
        if (args.length == 0) {
            sendChatInfoCommandTo(sender);
            return true;
        }

        SubCommand sub = commands.get(args[0].toLowerCase());

        // If no command matches, inform the player
        if (sub == null) {
            Messager.sendWarningMessage(sender, args[0] + " was not a recognized subcommand. Use /chat to see a list of available commands.");
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

    public void sendChatInfoCommandTo(CommandSender sender) {
        String msg = """
                A command to manage your chat settings.
                ===
                The format for your chat looks like this:
                [prefix] [nickname] [suffix]: <message>
                ===
                Setting your nick, prefix, or suffix:
                - Use '/chat nick <name>' to set your nick name.
                - Use '/chat prefix <name>' to set your nick name's prefix.
                - Use '/chat suffix <name>' to set your nick name's suffix.
                - Ex: '/chat nick NPC'
                ===
                Updating color:
                - Use '/chat color <type> <hex>' to set <type> to have a color by the <hex> value.
                - Possible types: nick, prefix, suffix, & message.
                - Hex value must be specified with 6 hex digits: 'FFFFFF' or '0xFFFFFF'.
                - Ex: '/chat color prefix FFFFFF'
                ===
                Updating format:
                - Use '/chat format <type> <format> <value>' to set <type> to have a <format> by a true/false <value>.
                - Possible types: nick, prefix, suffix, & message.
                - Possible formats: bold, italic, underlined, & strikethrough.
                - Possible values: true or false.
                - Ex: '/chat format nick bold true'
                ===
                Misc info:
                - Your nick will be reverted if any online or offline player has that name/nickname.
                - Your nick can only be alphanumeric with underscores.
                - Your nick must be between 3 to 16 characters long.
                - Your suffix and prefix cannot be more than 16 characters long.
                """;
        Messager.sendInfoMessage(sender, msg);
    }
}
