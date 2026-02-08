package org.gabooj.items.treeFeller;

import org.gabooj.crafts.TreeFellerCraft;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class TreeFellerModule implements Listener {

    private final JavaPlugin plugin;

    public TreeFellerModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        // Register event listener
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Add craft
        TreeFellerCraft.addTreeFellerRecipes(plugin);
    }

    public void disable() {
        // Remove craft
        TreeFellerCraft.removeTreeFellerRecipes(plugin);

        // Unregister event listener
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // Call tree feller plugin
        TreeFeller.handleBlockBreakEvent(event, plugin);
    }

    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.discoverRecipe(TreeFellerCraft.treeFellerDiamondKey);
        player.discoverRecipe(TreeFellerCraft.treeFellerNetheriteKey);
    }
}
