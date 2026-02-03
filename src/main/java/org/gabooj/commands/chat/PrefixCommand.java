package org.gabooj.commands.chat;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.gabooj.commands.SubCommand;
import org.gabooj.players.chat.ChatManager;
import org.gabooj.players.chat.ChatSettings;
import org.gabooj.utils.Messager;

import java.util.List;

public class PrefixCommand implements SubCommand {

    @Override
    public String name() {
        return "prefix";
    }

    @Override
    public List<String> aliases() {
        return List.of("pre", "p");
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
                A command to set your nickname's prefix using '/chat prefix <prefix>'.
                - Use CLEAR as <prefix> to clear your prefix.
                - Your prefix cannot be longer than 16 characters.
                """;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        String prefix = args[0];
        ChatSettings settings = ChatManager.getOrCreateChatSettings(player);

        // Handle prefix being too long
        if (prefix.length() > 16) {
            Messager.sendWarningMessage(sender, "Your prefix cannot be longer than 16 characters!");
            return;
        }

        if (prefix.equalsIgnoreCase("CLEAR")) {
            // Handle clearing prefix
            Messager.sendSuccessMessage(sender, "Cleared your nickname's prefix.");
            settings.prefix = "";
        } else {
            // Handle setting prefix
            Messager.sendSuccessMessage(sender, "Set your prefix to: " + prefix + ".");
            settings.prefix = prefix;
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of("<prefix>", "CLEAR");
    }
}
