package org.gabooj.commands.warp;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.gabooj.commands.SubCommand;
import org.gabooj.scope.ScopeMeta;
import org.gabooj.scope.Warp;
import org.gabooj.utils.Messager;
import org.gabooj.worlds.WorldManager;

import java.util.List;
import java.util.Set;

public class ListWarpComand implements SubCommand {

    private final WorldManager worldManager;

    public ListWarpComand(WorldManager worldManager) {
        this.worldManager = worldManager;
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
        return true;
    }

    @Override
    public String description(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return "You must be a player to use this command!";
        }
        ScopeMeta meta = worldManager.scopeManager.getCurrentPlayerScope(player);
        return getListMessage(meta);
    }

    public String getListMessage(ScopeMeta meta) {
        Set<Warp> warps = meta.getWarps();
        if (warps.isEmpty()) {
           return "No warps exist in " + meta.getName() + ".";
        } else {
            String warpStr = "Warps in " + meta.getName() + ": ";
            for (Warp warp : warps) {
                warpStr += warp.name + ", ";
            }
            warpStr = warpStr.substring(0, warpStr.length()-2);
            return warpStr;
        }
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        String groupName = args[0];

        // Ensure that group exists
        ScopeMeta meta = worldManager.scopeManager.getScopeByName(groupName);
        if (meta == null) {
            Messager.sendWarningMessage(sender, "No world with the name: '" + groupName + "' exists!");
            return;
        }

        // Send message to player
        Messager.sendInfoMessage(sender, getListMessage(meta));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return worldManager.scopeManager.getAllGroupNames();
    }
}
