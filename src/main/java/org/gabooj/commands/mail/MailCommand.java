package org.gabooj.commands.mail;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.players.chat.ChatManager;
import org.gabooj.utils.Messager;
import org.gabooj.utils.ServerService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MailCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;

    public MailCommand(JavaPlugin plugin) {
        this.plugin = plugin;

        PluginCommand command = plugin.getCommand("mail");
        if (command == null) {
            plugin.getLogger().warning("Command 'mail' is not defined in plugin.yml");
            return;
        }
        command.setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            Messager.sendWarningMessage(sender, "You must be a player to execute this command.");
            return true;
        }

        // Handle /mail
        if (args.length == 0) {
            Messager.sendInfoMessage(player, "Mail usage:\n1. '/mail read' - To read your mail\n2. '/mail send <name> <msg>' - To send a message to <name>\n3. '/mail clear' - To clear your mail");
            return true;
        }

        // Handle /mail <action>
        String action = args[0].toLowerCase();
        switch (action) {
            case "read" -> handleMailRead(player, command, label, args);
            case "send" -> handleMailSend(player, command, label, args);
            case "clear" -> handleMailClear(player, command, label, args);
            default -> Messager.sendWarningMessage(player, "The mail command must be used like '/mail read', '/mail send <name> <msg>', or '/mail clear'.");
        }
        return true;
    }

    public void handleMailRead(Player player, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        List<String> mail = ChatManager.playerMail.getOrDefault(player.getUniqueId(), new ArrayList<>());
        if (mail.isEmpty()) {
            Messager.sendInfoMessage(player, "You do not have any mail.");
        } else {
            StringBuilder to_send = new StringBuilder("Mail:\n");
            for (int i = 0; i < mail.size(); i++) {
                to_send.append((i + 1)).append(". ").append(mail.get(i)).append("\n");
            }
            Messager.sendInfoMessage(player, to_send.toString());
        }
    }

    public void handleMailSend(Player player, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length < 3) {
            Messager.sendInfoMessage(player, "To send mail, use '/mail send <name> <msg>'.");
        } else {
            // Parse message
            String message = player.getName() + ":";
            for (int i = 2; i < args.length; i++) {
                message += " " + args[i];
            }

            // Get recipient
            String recipientName = args[1];
            OfflinePlayer recipient = ServerService.getOfflinePlayerByName(recipientName, plugin.getServer());

            // No recipient exists
            if (recipient == null) {
                Messager.sendWarningMessage(player, "No online or offline player could be found with the name: " + recipientName + "!");
                return;
            }

            // Send mail
            List<String> mail = ChatManager.playerMail.getOrDefault(recipient.getUniqueId(), new ArrayList<>());
            mail.add(message);
            ChatManager.playerMail.put(recipient.getUniqueId(), mail);
            
            // Message recipient when online
            if (recipient.getName().equalsIgnoreCase(player.getName())) {
                Messager.sendSuccessMessage(player, "Sent message to yourself.");
            } else if (recipient.isOnline()) {
                Player onlineRecipinet = (Player) recipient;
                Messager.sendSuccessMessage(player, "Sent message to " + recipient.getName() + ".");
                Messager.sendInfoMessage(onlineRecipinet, player.getName() + " has sent you mail! Use '/mail read' to read your mail.");
            }
        }
    }

    public void handleMailClear(Player player, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        ChatManager.playerMail.remove(player.getUniqueId());
        Messager.sendInfoMessage(player, "You have cleared all your mail.");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 1) {
            return List.of("read", "clear", "send");
        } else if (args[0].equalsIgnoreCase("send")) {
            if (args.length == 2) {
                // /mail send <name> 
                return ServerService.getValidOfflinePlayers(plugin.getServer()).stream().map((p) -> p.getName()).toList();
            } else {
                // /mail send <name> <message>
                return List.of("<message");
            }
        }
        return List.of();
    }

}

