package org.gabooj;

import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.commands.CommandHandler;

public class PluginManager extends JavaPlugin {

    public CommandHandler commandHandler;
    public WorldManager worldManager;

    @Override
    public void onEnable() {
        // Instantiate world manager
        worldManager = new WorldManager(this);
        worldManager.onEnable();

        // Register commands
        commandHandler = new CommandHandler(this, worldManager);
    }

    @Override
    public void onDisable() {
        worldManager.onDisable();
    }


}