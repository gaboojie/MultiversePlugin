package org.gabooj.players;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.gabooj.players.afk.AfkManager;
import org.gabooj.players.chat.ChatManager;
import org.gabooj.scope.ScopeMeta;
import org.gabooj.worlds.WorldManager;
import org.gabooj.worlds.WorldMeta;

public class PlayerTabManager {

    public static WorldManager worldManager;

    public static void updatePlayerTab(Player player) {
        ScopeMeta scopeMeta = worldManager.scopeManager.getCurrentPlayerScope(player);
        World world = player.getWorld();
        WorldMeta worldMeta = worldManager.getWorldMetaByID(world.getName());
        if (worldMeta == null) return;
        updatePlayerTab(player, scopeMeta, worldMeta);
    }

    public static void updatePlayerTab(Player player, ScopeMeta scopeMeta, WorldMeta targetWorldMeta) {
        // Create component for world
        World.Environment environment = targetWorldMeta.getEnvironment();
        NamedTextColor color = getColorByWorld(environment);
        Component worldComponent = Component.text("[" + scopeMeta.getName().toUpperCase() + "] ", color);

        // Create component
        Component comp = Component.empty().append(worldComponent).append(ChatManager.getPlayerNicknameComponent(player));

        // Add AFK component if AFK
        boolean isAfk = AfkManager.isAfk(player);
        if (isAfk) {
            Component afkComponent = Component.text(" [AFK]", NamedTextColor.DARK_GRAY);
            comp = comp.append(afkComponent);
        }

        player.playerListName(comp);
    }

    private static NamedTextColor getColorByWorld(World.Environment environment) {
        switch (environment) {
            case NORMAL, CUSTOM -> {
                return NamedTextColor.GRAY;
            }
            case NETHER -> {
                return NamedTextColor.RED;
            }
            case THE_END -> {
                return NamedTextColor.BLACK;
            }
        }
        return NamedTextColor.GRAY;
    }
}
