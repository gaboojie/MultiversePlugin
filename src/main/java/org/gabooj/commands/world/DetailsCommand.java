package org.gabooj.commands.world;

import org.bukkit.command.CommandSender;
import org.gabooj.commands.SubCommand;
import org.gabooj.utils.Messager;
import org.gabooj.worlds.WorldManager;
import org.gabooj.worlds.WorldMeta;

import java.util.List;

public class DetailsCommand implements SubCommand {

    private final WorldManager worldManager;

    public DetailsCommand(WorldManager worldManager) {
        this.worldManager = worldManager;
    }

    @Override
    public String name() {
        return "details";
    }

    @Override
    public List<String> aliases() {
        return List.of("info");
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
        return "A command to show the details of a given world. Use /world details <world> to get verbose details about a world.";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        WorldMeta meta = worldManager.getWorldMetaByID(args[0]);

        // No world found
        if (meta == null) {
            Messager.sendWarningMessage(sender, args[0] + " is not the name of a world.");
            return;
        }

        // Show details about world
        Messager.sendInfoMessage(sender, meta.toString(worldManager));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return worldManager.getWorldIDs();
    }
}
