package org.gabooj;

import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.commands.afk.AfkCommand;
import org.gabooj.commands.chat.ChatCommandHandler;
import org.gabooj.commands.group.GroupCommandHandler;
import org.gabooj.commands.home.HomeCommand;
import org.gabooj.commands.mail.MailCommand;
import org.gabooj.commands.protection.ClaimCommand;
import org.gabooj.commands.showHand.ShowHandCommand;
import org.gabooj.commands.tpa.TpaCommand;
import org.gabooj.commands.warp.WarpCommandHandler;
import org.gabooj.commands.world.WorldCommandHandler;
import org.gabooj.items.builderWand.BuilderWandModule;
import org.gabooj.items.treeFeller.TreeFellerModule;
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
    public ShowHandCommand showHandCommand;

    // Global managers
    public WorldManager worldManager;
    public ChatManager chatManager;
    public LandProtectionManager landProtectionManager;

    // Items
    public BuilderWandModule builderWandModule;
    public TreeFellerModule treeFellerModule;

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
        showHandCommand = new ShowHandCommand(this);

        // Register services
        PlayerMoveService.addMoveScheduler(this, worldManager);

        // Register items
        builderWandModule = new BuilderWandModule(this);
        builderWandModule.enable();
        treeFellerModule = new TreeFellerModule(this);
        treeFellerModule.enable();
    }

    @Override
    public void onDisable() {
        builderWandModule.disable();
        treeFellerModule.disable();
        landProtectionManager.onDisable();
        worldManager.onDisable();
        chatManager.onDisable();
        PlayerMoveService.onDisable();
    }


}
