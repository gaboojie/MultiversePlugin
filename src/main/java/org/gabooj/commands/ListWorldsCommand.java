package org.gabooj.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.WorldManager;
import org.gabooj.WorldMeta;

import java.util.Collection;
import java.util.List;

public class ListWorldsCommand implements SubCommand {

    private final JavaPlugin plugin;
    private final WorldManager worldManager;
    private final CommandHandler commandHandler;

    public ListWorldsCommand(JavaPlugin plugin, WorldManager worldManager, CommandHandler commandHandler) {
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
        return false;
    }

    @Override
    public boolean needsToBePlayer() {
        return false;
    }

    @Override
    public String description(CommandSender sender) {
        return getFormattedWorldList(sender);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GOLD + getFormattedWorldList(sender));
    }

    public String getFormattedWorldList(CommandSender sender) {
        Collection<WorldMeta> worlds = worldManager.worlds.values();
        if (worlds.isEmpty()) {
            return "There are no worlds available right now.";
        } else {
            StringBuilder info = new StringBuilder("Worlds:");
            for (WorldMeta meta : worlds) {
                String toAppend = "";
                if (!meta.visible) {
                    toAppend = " [INVISIBLE]";
                    if (!sender.isOp()) {
                        continue;
                    }
                }

                info.append(" ").append(meta.worldName).append(toAppend).append(",");
            }
            // Remove trailing comma
            if (info.charAt(info.length()-1) == ',') {
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
