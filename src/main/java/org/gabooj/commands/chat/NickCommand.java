package org.gabooj.commands.chat;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.commands.SubCommand;
import org.gabooj.commands.group.GroupCommandHandler;
import org.gabooj.scope.ScopeMeta;
import org.gabooj.worlds.WorldManager;
import org.gabooj.worlds.WorldMeta;

import java.util.List;
import java.util.Set;

public class SetChatCommand implements SubCommand {

    private final JavaPlugin plugin;
    private final WorldManager worldManager;
    private final ChatCommandHandler commandHandler;

    public SetChatCommand(JavaPlugin plugin, WorldManager worldManager, ChatCommandHandler commandHandler) {
        this.plugin = plugin;
        this.worldManager = worldManager;
        this.commandHandler = commandHandler;
    }

    @Override
    public String name() {
        return "list";
    }

    @Override
    public List<String> aliases() {
        return List.of();
    }

    @Override
    public boolean needsOp() {
        return true;
    }

    @Override
    public boolean needsToBePlayer() {
        return false;
    }

    @Override
    public String description(CommandSender sender) {
        List<String> groups = worldManager.scopeManager.getAllGroupNames();

        if (groups.isEmpty()) {
            return "No groups have been created yet.";
        }

        String msg = "Groups:\n";

        for (ScopeMeta scopeMeta : worldManager.scopeManager.getScopes().values()) {
            Set<WorldMeta> worldMetas = scopeMeta.getWorlds();
            if (worldMetas.isEmpty()) {
                msg += "- Group '" + scopeMeta.getScopeId() + "' has no worlds.\n";
            } else {
                String groupMsg = "- Group '" + scopeMeta.getName() + "' has worlds: ";
                for (WorldMeta worldMeta : worldMetas) {
                    groupMsg += worldMeta.getWorldID() + ", ";
                }
                groupMsg = groupMsg.substring(0, groupMsg.length()-2);
                msg += groupMsg + "\n";
            }
        }
        return msg;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GOLD + description(sender));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
