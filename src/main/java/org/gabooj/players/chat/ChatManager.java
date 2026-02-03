package org.gabooj.players.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.utils.Messager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatManager {

    private static final Map<UUID, ChatSettings> playerChatSettings = new HashMap<>();

    private static JavaPlugin plugin;
    private static File file;
    private static FileConfiguration config;

    public ChatManager(JavaPlugin p) {
        plugin = p;
    }

    public void onEnable() {
        ConfigurationSerialization.registerClass(ChatSettings.class);
        loadChatSettings();

        // Register listener
        plugin.getServer().getPluginManager().registerEvents(new ChatListener(), plugin);
    }

    public void onDisable() {
        saveChatSettings();
    }

    public static ChatSettings getChatSettings(UUID uuid) {
        return playerChatSettings.get(uuid);
    }

    public static void removeChatSettings(UUID uuid) {
        playerChatSettings.remove(uuid);
    }

    public static Map<UUID, ChatSettings> getAllPlayerChatSettings() {
        return playerChatSettings;
    }

    public static boolean isPlayerName(String nickname, Player player) {
        return player.getName().equalsIgnoreCase(nickname);
    }

    public static boolean isNicknameAlreadyTaken(String nickname) {
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (offlinePlayer.getName() == null) continue;

            // Check player names
            String offlinePlayerName = offlinePlayer.getName();
            if (offlinePlayerName.equalsIgnoreCase(nickname)) return true;

            // Check player nicknames
            UUID offlinePlayerUUID = offlinePlayer.getUniqueId();
            ChatSettings settings = ChatManager.getChatSettings(offlinePlayerUUID);
            if (settings != null) {
                if (settings.nickname.equalsIgnoreCase(nickname)) return true;
            }
        }
        return false;
    }

    public static ChatSettings getOrCreateChatSettings(Player player) {
        UUID uuid = player.getUniqueId();
        if (playerChatSettings.containsKey(uuid)) {
            return playerChatSettings.get(uuid);
        } else {
            return createChatSettings(player);
        }
    }

    public static ChatSettings createChatSettings(Player player) {
        UUID uuid = player.getUniqueId();
        ChatSettings settings = new ChatSettings();
        settings.nickname = player.getName();
        playerChatSettings.put(uuid, settings);
        return settings;
    }

    public static Component getPlayerNicknameComponent(Player player) {
        ChatSettings settings = ChatManager.getOrCreateChatSettings(player);
        String nickname = settings.nickname.isEmpty() ? player.getName() : settings.nickname;
        Component nicknameComponent = Component.text(nickname).color(settings.nicknameHexColor);
        for (TextDecoration dec : settings.nicknameDecorations) {
            nicknameComponent = nicknameComponent.decorate(dec);
        }
        return nicknameComponent;
    }

    public void loadChatSettings() {
        // Load config file
        file = new File(plugin.getDataFolder(), "chatSettings.yml");
        if (!file.exists()) {
            // Create if DNE
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                Messager.broadcastWarningMessage(plugin.getServer(), "Could not create the chatSettings.yml file!");
            }
        }
        config = YamlConfiguration.loadConfiguration(file);

        // Load settings in config file
        ConfigurationSection section = config.getConfigurationSection("players");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                ChatSettings settings = (ChatSettings) section.get(key);
                playerChatSettings.put(uuid, settings);
            } catch (IllegalArgumentException ignored) {
                Messager.broadcastWarningMessage(plugin.getServer(), "Could not load an entry in chat settings: " + key);
            }
        }
    }

    public void saveChatSettings() {
        // Clear previously saved data
        config.set("players", null);

        // Set data
        for (var entry : playerChatSettings.entrySet()) {
            config.set("players." + entry.getKey(), entry.getValue());
        }

        // Save file
        try {
            config.save(file);
        } catch (IOException ignored) {
            Messager.broadcastWarningMessage(plugin.getServer(), "Could not save chatSettings file: chatSettings.yml!");
        }
    }

}
