package org.gabooj.commands.chat;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.gabooj.commands.SubCommand;
import org.gabooj.players.chat.ChatManager;
import org.gabooj.players.chat.ChatSettings;
import org.gabooj.utils.Messager;

import java.util.List;

public class SuffixCommand implements SubCommand {

    @Override
    public String name() {
        return "suffix";
    }

    @Override
    public List<String> aliases() {
        return List.of("suf", "s");
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
                A command to set your nickname's suffix using '/chat suffix <suffix>'.
                - Use CLEAR as <suffix> to clear your suffix.
                - Your suffix cannot be longer than 16 characters.
                """;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        String suffix = args[0];
        ChatSettings settings = ChatManager.getOrCreateChatSettings(player);

        // Handle prefix being too long
        if (suffix.length() > 16) {
            Messager.sendWarningMessage(sender, "Your suffix cannot be longer than 16 characters!");
            return;
        }

        if (suffix.equalsIgnoreCase("CLEAR")) {
            // Handle clearing suffix
            Messager.sendSuccessMessage(sender, "Cleared your nickname's suffix.");
            settings.suffix = "";
        } else {
            // Handle setting suffix
            Messager.sendSuccessMessage(sender,  "Set your suffix to: " + suffix + ".");
            settings.suffix = suffix;
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of("<suffix>", "CLEAR");
    }
}
