package org.gabooj;

import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.commands.group.GroupCommandHandler;
import org.gabooj.commands.warp.WarpCommandHandler;
import org.gabooj.commands.world.WorldCommandHandler;
import org.gabooj.worlds.WorldManager;

public class PluginManager extends JavaPlugin {

    public WorldCommandHandler worldCommandHandler;
    public GroupCommandHandler groupCommandHandler;
    public WarpCommandHandler warpCommandHandler;
    public WorldManager worldManager;

    @Override
    public void onEnable() {
        // Instantiate world manager
        worldManager = new WorldManager(this);
        worldManager.onEnable();

        // Register commands
        worldCommandHandler = new WorldCommandHandler(this, worldManager);
        groupCommandHandler = new GroupCommandHandler(this, worldManager);
        warpCommandHandler = new WarpCommandHandler(this, worldManager);
    }

    @Override
    public void onDisable() {
        worldManager.onDisable();
    }


}