package org.gabooj.utils;

import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class Messager {

    // Player sender messages

    public static void messagePlayer(Player player, String message, NamedTextColor color) {
        player.sendMessage(Component.text(message, color));
    }

    public static void sendWarningMessage(Player player, String message) {
        messagePlayer(player, message, NamedTextColor.RED);
    }

    public static void sendSevereWarningMessage(Player player, String message) {
        messagePlayer(player, message, NamedTextColor.DARK_RED);
    }

    public static void sendSuccessMessage(Player player, String message) {
        messagePlayer(player, message, NamedTextColor.GREEN);
    }

    public static void sendInfoMessage(Player player, String message) {
        messagePlayer(player, message, NamedTextColor.GOLD);
    }

    // Command sender messages

    public static void messageSender(CommandSender sender, String message, NamedTextColor color) {
        sender.sendMessage(Component.text(message, color));
    }

    public static void sendWarningMessage(CommandSender sender, String message) {
        messageSender(sender, message, NamedTextColor.RED);
    }

    public static void sendSevereWarningMessage(CommandSender sender, String message) {
        messageSender(sender, message, NamedTextColor.DARK_RED);
    }

    public static void sendSuccessMessage(CommandSender sender, String message) {
        messageSender(sender, message, NamedTextColor.GREEN);
    }

    public static void sendInfoMessage(CommandSender sender, String message) {
        messageSender(sender, message, NamedTextColor.GOLD);
    }

    // Broadcast message

    public static void broadcastMessage(Server server, String message, NamedTextColor color) {
        server.broadcast(Component.text(message, color));
    }

    public static void broadcastWarningMessage(Server server, String message) {
        broadcastMessage(server, message, NamedTextColor.RED);
    }

    public static void broadcastSuccessMessage(Server server, String message) {
        broadcastMessage(server, message, NamedTextColor.GREEN);
    }

    public static void broadcastInfoMessage(Server server, String message) {
        broadcastMessage(server, message, NamedTextColor.GOLD);
    }


}
