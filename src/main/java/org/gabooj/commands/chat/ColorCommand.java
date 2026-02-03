package org.gabooj.commands.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.gabooj.commands.SubCommand;
import org.gabooj.players.PlayerTabManager;
import org.gabooj.players.chat.ChatManager;
import org.gabooj.players.chat.ChatSettings;
import org.gabooj.utils.Messager;

import java.util.List;
import java.util.regex.Pattern;

public class ColorCommand implements SubCommand {

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^(#|0x)?[0-9A-Fa-f]{6}$");

    @Override
    public String name() {
        return "color";
    }

    @Override
    public List<String> aliases() {
        return List.of("c");
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
                A command to set the color of your nick, prefix, suffix, or message.
                - Use '/chat color <type> <hex>' to set <type> to have a color by the <hex> value.
                - Possible types: nick, prefix, suffix, & message.
                - Hex value must be specified with 6 hex digits: 'FFFFFF' or '0xFFFFFF'.
                - Ex: '/chat color prefix FFFFFF'
                """;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // Send description
        if (args.length < 2) {
            Messager.sendInfoMessage(sender, description(sender));
            return;
        }
        Player player = (Player) sender;

        // Ensure text color value is valid
        String hexString = args[1];
        TextColor color = parseHexColor(hexString);
        if (color == null) {
            Messager.sendWarningMessage(sender, "Could not parse hex value: " + hexString + "!");
            return;
        }

        // Apply hex color according to type
        String type = args[0].toLowerCase();
        ChatSettings settings = ChatManager.getOrCreateChatSettings(player);
        switch (type) {
            case "nick":
                settings.nicknameHexColor = color;
                PlayerTabManager.updatePlayerTab(player);
                break;
            case "prefix":
                settings.prefixHexColor = color;
                break;
            case "suffix":
                settings.suffixHexColor = color;
                break;
            case "message":
                settings.messageHexColor = color;
                break;
            default:
                Messager.sendWarningMessage(sender, "Could not identify chat type: " + type + "!");
                return;
        }

        // Message player with color
        player.sendMessage(Component.text("Set '" + type + "' to this text's color.", color));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return switch (args.length) {
            case 1 -> List.of("nick", "prefix", "suffix", "message");
            case 2 -> List.of("<hex value>");
            default -> List.of();
        };
    }

    public static TextColor parseHexColor(String input) {
        if (input == null || !HEX_COLOR_PATTERN.matcher(input).matches()) {
            return null;
        }

        // Normalize input to #RRGGBB
        String normalized;
        if (input.startsWith("#")) {
            normalized = input;
        } else if (input.startsWith("0x") || input.startsWith("0X")) {
            normalized = "#" + input.substring(2);
        } else {
            normalized = "#" + input;
        }

        return TextColor.fromHexString(normalized);
    }
}
