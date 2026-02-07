package org.gabooj;

import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.commands.afk.AfkCommand;
import org.gabooj.commands.chat.ChatCommandHandler;
import org.gabooj.commands.group.GroupCommandHandler;
import org.gabooj.commands.home.HomeCommand;
import org.gabooj.commands.mail.MailCommand;
import org.gabooj.commands.protection.ClaimCommand;
import org.gabooj.commands.tpa.TpaCommand;
import org.gabooj.commands.warp.WarpCommandHandler;
import org.gabooj.commands.world.WorldCommandHandler;
import org.gabooj.players.chat.ChatManager;
import org.gabooj.protection.LandProtectionManager;
import org.gabooj.services.PlayerMoveService;
import org.gabooj.worlds.WorldManager;

public class PluginManager extends JavaPlugin {

    // Commands
    public WorldCommandHandler worldCommandHandler;
    public GroupCommandHandler groupCommandHandler;
    public WarpCommandHandler warpCommandHandler;
    public ChatCommandHandler chatCommandHandler;
    public AfkCommand afkCommand;
    public TpaCommand tpaCommand;
    public ClaimCommand claimCommand;
    public HomeCommand homeCommand;
    public MailCommand mailCommand;

    // Global managers
    public WorldManager worldManager;
    public ChatManager chatManager;
    public LandProtectionManager landProtectionManager;

    @Override
    public void onEnable() {
        // Instantiate chat manager
        chatManager = new ChatManager(this);
        chatManager.onEnable();

        // Instantiate world manager
        worldManager = new WorldManager(this);
        worldManager.onEnable();

        // Instantiate protection manager
        landProtectionManager = new LandProtectionManager(this, worldManager);
        landProtectionManager.onEnable();

        // Register command handlers
        worldCommandHandler = new WorldCommandHandler(this, worldManager);
        groupCommandHandler = new GroupCommandHandler(this, worldManager);
        warpCommandHandler = new WarpCommandHandler(this, worldManager);
        chatCommandHandler = new ChatCommandHandler(this);

        // Register individual commands
        afkCommand = new AfkCommand(this);
        tpaCommand = new TpaCommand(this, worldManager);
        claimCommand = new ClaimCommand(this, landProtectionManager);
        homeCommand = new HomeCommand(this, worldManager);
        mailCommand = new MailCommand(this);

        // Register services
        PlayerMoveService.addMoveScheduler(this, worldManager);
    }

    @Override
    public void onDisable() {
        landProtectionManager.onDisable();
        worldManager.onDisable();
        chatManager.onDisable();
        PlayerMoveService.onDisable();
    }


}
