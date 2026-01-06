package org.gabooj.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.WorldManager;
import org.gabooj.WorldMeta;

import java.util.Collection;
import java.util.List;

public class StatusCommand implements SubCommand {

    private final JavaPlugin plugin;
    private final WorldManager worldManager;
    private final CommandHandler commandHandler;

    public StatusCommand(JavaPlugin plugin, WorldManager worldManager, CommandHandler commandHandler) {
        this.plugin = plugin;
        this.worldManager = worldManager;
        this.commandHandler = commandHandler;
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
        sender.sendMessage(ChatColor.GOLD + getStatusOfWorlds(sender));
    }

    public String getStatusOfWorlds(CommandSender sender) {
        Collection<WorldMeta> worlds = worldManager.worlds.values();
        if (worlds.isEmpty()) {
            return "There are no worlds available right now.";
        } else {
            StringBuilder info = new StringBuilder("World Statuses:\n");
            for (WorldMeta meta : worlds) {
                String toAppend = "";
                if (!meta.visible) {
                    toAppend = " [INVISIBLE]";
                    if (!sender.isOp()) {
                        continue;
                    }
                }

                ChatColor color = ChatColor.GOLD;
                if (meta.status == WorldMeta.Status.UNLOADED) {
                    color = ChatColor.RED;
                } else if (meta.status == WorldMeta.Status.REGISTERED) {
                    color = ChatColor.DARK_RED;
                }
                info.append(ChatColor.GOLD + "- World '").append(meta.worldName).append("': ").append(color + "" + meta.status).append(toAppend).append("\n");
            }
            // Remove trailing new line
            if (info.charAt(info.length()-1) == '\n') {
                info.deleteCharAt(info.length()-1);
            }
            return info.toString();
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
