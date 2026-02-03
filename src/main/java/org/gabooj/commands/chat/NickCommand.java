package org.gabooj.commands.chat;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.gabooj.commands.SubCommand;
import org.gabooj.players.PlayerTabManager;
import org.gabooj.players.chat.ChatManager;
import org.gabooj.players.chat.ChatSettings;
import org.gabooj.utils.Messager;

import java.util.List;

public class NickCommand implements SubCommand {

    @Override
    public String name() {
        return "nick";
    }

    @Override
    public List<String> aliases() {
        return List.of("nickname");
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
                A command to set your nickname using '/chat nick <nickname>'.
                - Your nick will be reverted if any online or offline player has that name/nickname.
                - Your nick can only be alphanumeric with underscores.
                - Your nick must be between 3 to 16 characters long.
                - Type something in chat to see your current nickname!
                """;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        String nick = args[0];

        // Check if alphanumeric
        if (!isAlphanumericOrUnderscore(nick) ) {
            Messager.sendWarningMessage(sender, "Your nickname must be alphanumeric (but can also include an underscore)!");
            return;
        }

        // Check if incorrect size
        if (isIncorrectSize(nick)) {
            Messager.sendWarningMessage(sender, "Your nickname must be between 3 to 16 characters long!");
            return;
        }

        // Check if nick is already the player's nickname
        ChatSettings playerSettings = ChatManager.getOrCreateChatSettings(player);
        if (playerSettings.nickname.equalsIgnoreCase(nick)) {
            Messager.sendWarningMessage(sender, "Your nickname is already: " + playerSettings.nickname + "!");
            return;
        }

        // Check if player is trying to reset their nickname to their default name
        if (ChatManager.isPlayerName(nick, player)) {
            Messager.sendSuccessMessage(sender, "Reset your nickname to your default name.");
            playerSettings.nickname = player.getName();
            return;
        }

        // Check if it coincides with any online or offline player's name or nickname
        if (ChatManager.isNicknameAlreadyTaken(nick)) {
            Messager.sendWarningMessage(sender, "The nickname: '" + nick + "' is already taken as a name or nickname by another player!");
            return;
        }

        Messager.sendSuccessMessage(sender, "Set your nickname to: " + nick + ".");
        playerSettings.nickname = nick;
        PlayerTabManager.updatePlayerTab(player);
    }

    public boolean isAlphanumericOrUnderscore(String s) {
        if (s == null || s.isEmpty()) return false;

        for (char c : s.toCharArray()) {
            if (!(Character.isLetterOrDigit(c) || c == '_')) {
                return false;
            }
        }
        return true;
    }

    public boolean isIncorrectSize(String s) {
        return s.length() < 3 || s.length() > 16;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of("<nickname>");
    }
}
