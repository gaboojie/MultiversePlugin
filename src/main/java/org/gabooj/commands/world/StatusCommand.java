package org.gabooj.commands.world;

import org.bukkit.command.CommandSender;
import org.gabooj.commands.SubCommand;
import org.gabooj.scope.ScopeMeta;
import org.gabooj.worlds.WorldManager;
import org.gabooj.worlds.WorldMeta;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

public class StatusCommand implements SubCommand {

    private final WorldManager worldManager;

    public StatusCommand(WorldManager worldManager) {
        this.worldManager = worldManager;
    }

    @Override
    public String name() {
        return "status";
    }

    @Override
    public List<String> aliases() {
        return List.of();
    }

    @Override
    public boolean needsOp() {
        return false;
    }

    @Override
    public boolean needsToBePlayer() {
        return false;
    }

    @Override
    public String description(CommandSender sender) {
        sender.sendMessage(getStatusOfWorldsComponent(sender));
        return null;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage(getStatusOfWorldsComponent(sender));
    }

    public Component getStatusOfWorldsComponent(CommandSender sender) {
        Component allText = Component.text("World Statuses:\n", NamedTextColor.GOLD);
        for (ScopeMeta scopeMeta : worldManager.scopeManager.getScopes().values()) {
            if (scopeMeta.getWorlds().isEmpty()) continue;

            // Add group component
            String groupText = "Group: '" + scopeMeta.getName() + "':\n -";
            Component groupComponent = Component.text(groupText, NamedTextColor.GOLD);

            // Add each world
            for (WorldMeta worldMeta : scopeMeta.getWorlds()) {
                groupComponent.append(getWorldStatus(worldMeta));
            }
            groupComponent.append(Component.text("\n"));
            allText.append(groupComponent);
        }

        // Add information text;
        allText.append(Component.text("Online", NamedTextColor.GREEN));
        allText.append(Component.text("| Offline", NamedTextColor.RED));
        
        return allText;
    }

    public Component getWorldStatus(WorldMeta meta) {
        NamedTextColor worldColor = (meta.isLoaded() && !meta.isUnloading) ? NamedTextColor.GREEN : NamedTextColor.RED;
        return Component.text(" " + meta.getWorldID(), worldColor);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
