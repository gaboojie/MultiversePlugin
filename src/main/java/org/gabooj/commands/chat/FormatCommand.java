package org.gabooj.commands.chat;

import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.gabooj.commands.SubCommand;
import org.gabooj.players.PlayerTabManager;
import org.gabooj.players.chat.ChatManager;
import org.gabooj.players.chat.ChatSettings;
import org.gabooj.utils.Messager;

import java.util.List;
import java.util.Set;

public class FormatCommand implements SubCommand {

    @Override
    public String name() {
        return "format";
    }

    @Override
    public List<String> aliases() {
        return List.of("f");
    }

    @Override
    public boolean needsOp() {
        return false;
    }

    @Override
    public boolean needsToBePlayer() {
        return true;
    }

    @Override
    public String description(CommandSender sender) {
        return """
                A command to set the format of your nick, prefix, suffix, or message.
                - Use '/chat format <type> <format> <value>' to set <type> to have a <format> by a true/false <value>.
                - Possible types: nick, prefix, suffix, & message.
                - Possible formats: bold, italic, underlined, & strikethrough.
                - Possible values: true or false.
                - Ex: '/chat format nick bold true'
                """;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // Send description
        if (args.length < 3) {
            Messager.sendInfoMessage(sender, description(sender));
            return;
        }
        Player player = (Player) sender;

        // Ensure text color value is valid
        String formatString = args[1].toLowerCase();
        TextDecoration format = TextDecoration.NAMES.value(formatString);
        if (format == null) {
            Messager.sendWarningMessage(sender, "Could not identify the format: " + formatString + "!");
            return;
        }

        // Parse boolean value
        boolean value = Boolean.parseBoolean(args[2]);

        // Apply hex color according to type
        String type = args[0].toLowerCase();
        ChatSettings settings = ChatManager.getOrCreateChatSettings(player);
        Set<TextDecoration> decorations = null;
        switch (type) {
            case "nick":
                decorations = settings.nicknameDecorations;
                break;
            case "prefix":
                decorations = settings.prefixDecorations;
                break;
            case "suffix":
                decorations = settings.suffixDecorations;
                break;
            case "message":
                decorations = settings.messageDecorations;
                break;
            default:
                Messager.sendWarningMessage(sender, "Could not identify chat type: " + type + "!");
                return;
        }

        // Update decoration in the set
        if (value) {
            decorations.add(format);
            Messager.sendSuccessMessage(sender, "Enabled '" + formatString + "' to your " + type + ".");
        } else {
            decorations.remove(format);
            Messager.sendSuccessMessage(sender, "Disabled '" + formatString + "' from your " + type + ".");
        }

        // Update text decoration in tab
        if (type.equals("nick")) PlayerTabManager.updatePlayerTab(player);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> List.of("nick", "prefix", "suffix", "message");
            case 2 -> List.of("bold", "italic", "underlined", "strikethrough");
            case 3 -> List.of("true", "false");
            default -> List.of();
        };
    }
}
