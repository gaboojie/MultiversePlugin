package org.gabooj.commands.world;

import org.bukkit.command.CommandSender;
import org.gabooj.commands.SubCommand;
import org.gabooj.scope.ScopeMeta;
import org.gabooj.utils.Messager;
import org.gabooj.worlds.WorldManager;
import org.gabooj.worlds.WorldMeta;

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
        return getStatusOfWorlds(sender);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Messager.sendInfoMessage(sender, getStatusOfWorlds(sender));
    }

    public String getStatusOfWorlds(CommandSender sender) {
        StringBuilder info = new StringBuilder("World Statuses:\n");
        for (ScopeMeta scopeMeta : worldManager.scopeManager.getScopes().values()) {
            if (scopeMeta.getWorlds().isEmpty()) continue;
            // Add info for each world in scope
            String scopeToAppend = "Group: '" + scopeMeta.getName() + "':\n - ";
            for (WorldMeta worldMeta : scopeMeta.getWorlds()) {
                scopeToAppend += worldMeta.getWorldID() + ", ";
            }
            scopeToAppend = scopeToAppend.substring(0, scopeToAppend.length()-2);
            info.append(scopeToAppend).append("\n");
        }

        // Remove trailing new line
        if (info.charAt(info.length()-1) == '\n') {
            info.deleteCharAt(info.length()-1);
        }
        return info.toString();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
