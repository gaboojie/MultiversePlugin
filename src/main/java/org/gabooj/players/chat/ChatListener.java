package org.gabooj.players.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.gabooj.players.PlayerTabManager;
import org.gabooj.utils.Messager;

import java.util.Map;
import java.util.UUID;

public class ChatListener implements Listener {

    //
    // PLAYER JOIN EVENT
    //

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPlayedBefore()) {
            // Resolve any nickname collision logic
            resetConflictingNickname(player);

            // Welcome player for first log in
            player.getServer().broadcast(getNewPlayerBroadcastComponent(player));
            Messager.messagePlayer(player, "Use /help for more information.", NamedTextColor.GRAY);
        } else {
            // Welcome back player for new log in
            player.getServer().broadcast(getRejoiningPlayerBroadcastComponent(player));
            Messager.sendSuccessMessage(player, "Welcome back to the server!");
        }

        // Update tab name for player
        PlayerTabManager.updatePlayerTab(player);

        // Set no join message
        event.joinMessage(null);
    }

    private Component getNewPlayerBroadcastComponent(Player player) {
        return Component.text("Welcome " + player.getName() + " to the server!", NamedTextColor.GREEN, TextDecoration.BOLD);
    }

    private Component getRejoiningPlayerBroadcastComponent(Player player) {
        Component comp = ChatManager.getPlayerNicknameComponent(player);
        return comp.append(Component.text(" rejoined the server.", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, false));
    }

    //
    // PLAYER CHAT EVENT
    //

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        ChatSettings settings = ChatManager.getOrCreateChatSettings(player);

        // Build the full message component
        Component component = buildChatMessage(player, event.message(), settings);

        // Set the component so it shows in chat
        event.renderer((source, message, viewer, filter) -> component);
    }

    private Component buildChatMessage(Player player, Component originalMessage, ChatSettings settings) {
        // Combine: [prefix] [nickname] [suffix]: message
        Component comp = Component.empty();

        // Add prefix if it exists
        if (!settings.prefix.isBlank()) {
            // Prefix
            Component prefix = Component.text(settings.prefix).color(settings.prefixHexColor);
            for (TextDecoration dec : settings.prefixDecorations) {
                prefix = prefix.decorate(dec);
            }

            comp = comp.append(prefix).append(Component.text(" "));
        }

        // Add nickname
        comp = comp.append(ChatManager.getPlayerNicknameComponent(player));

        // Add suffix
        if (!settings.suffix.isBlank()) {
            // Suffix
            Component suffix = Component.text(settings.suffix).color(settings.suffixHexColor);
            for (TextDecoration dec : settings.suffixDecorations) {
                suffix = suffix.decorate(dec);
            }

            comp = comp.append(Component.text(" ")).append(suffix);
        }

        // Add colon
        comp = comp.append(Component.text(": "));

        // Add message
        Component messageText = originalMessage.color(settings.messageHexColor);
        for (TextDecoration dec : settings.messageDecorations) {
            messageText = messageText.decorate(dec);
        }
        return comp.append(messageText);
    }

    public void resetConflictingNickname(Player player) {
        Map<UUID, ChatSettings> playerChatSettings = ChatManager.getAllPlayerChatSettings();
        for (Map.Entry<UUID, ChatSettings> entry : playerChatSettings.entrySet()) {
            UUID uuid = entry.getKey();
            ChatSettings settings = entry.getValue();

            // Ignore any matching uuid
            if (player.getUniqueId().equals(uuid)) continue;

            if (settings.nickname.equalsIgnoreCase(player.getName())) {
                // Nickname collision found, revert the settings name to their name via UUID look-up
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                String name = offlinePlayer.getName();

                // Ensure that name exists
                if (name == null) {
                    Messager.broadcastMessage(player.getServer(), "ERROR: Name collision found for 'offline player no name found' and '" + settings.nickname + "', but the UUID for '" + settings.nickname + "' could not be used to find the old name!", NamedTextColor.DARK_RED);
                    return;
                }

                // Update player's nickname
                settings.nickname = name;

                // If player is online, tell them
                Player onlinePlayer = Bukkit.getPlayer(uuid);
                if (onlinePlayer != null) {
                    Messager.sendWarningMessage(player, "Your nickname has been reverted to your original name, because a player with that name has logged in.");
                }
            }
        }
    }

    //
    // REPLACING NAME FOR NICKNAME EVENTS
    //

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        Component original = event.deathMessage();
        if (original == null) return;

        // Set new message
        event.deathMessage(Component.text(applyNickToMessage(original, player), NamedTextColor.RED));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Component msg = event.quitMessage();
        if (msg == null) return;

        // Set new message
        event.quitMessage(Component.text(applyNickToMessage(msg, player), NamedTextColor.GRAY));
    }

    public String applyNickToMessage(Component message, Player player) {
        String nick = ChatManager.getChatSettings(player.getUniqueId()).nickname;
        String plain = PlainTextComponentSerializer.plainText().serialize(message);
        return plain.replace(player.getName(), nick);
    }

}
