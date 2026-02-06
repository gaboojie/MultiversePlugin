package org.gabooj.protection;

import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.utils.Messager;

public class LandProtectionListener implements Listener {

    private final LandProtectionManager manager;
    private final JavaPlugin plugin;

    public LandProtectionListener(JavaPlugin plugin, LandProtectionManager manager) {
        this.manager = manager;
        this.plugin = plugin;
    }

    public void onEnable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private boolean isOpPlayer(Entity entity) {
        return (entity instanceof Player p) && p.isOp();
    }

    private void warnClaimed(Player player) {
        Messager.sendWarningMessage(player, "You can't do that in a claimed chunk.");
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (isOpPlayer(event.getEntity())) return;
        if (manager.isClaimed(event.getBlock())) {
            event.setCancelled(true);
            if (event.getEntity() instanceof Player player) {
                warnClaimed(player);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getPlayer().isOp()) return;
        if (manager.isClaimed(event.getBlock())) {
            event.setCancelled(true);
            warnClaimed(event.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getPlayer().isOp()) return;
        if (manager.isClaimed(event.getBlockPlaced())) {
            event.setCancelled(true);
            warnClaimed(event.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (manager.isClaimed(event.getLocation())) {
            event.setCancelled(true);
            return;
        }
        removeClaimedBlocks(event.blockList());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (manager.isClaimed(event.getBlock())) {
            event.setCancelled(true);
            return;
        }
        removeClaimedBlocks(event.blockList());
    }

    private void removeClaimedBlocks(List<Block> blocks) {
        blocks.removeIf((b) -> manager.isClaimed(b));
    }

    // OP damaging

    private Player getPlayerAttacker(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p) return p;
        return null;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!manager.isClaimed(event.getEntity().getLocation())) return;

        Player attacker = getPlayerAttacker(event.getDamager());
        if (attacker != null && attacker.isOp()) {
            return;
        }

        event.setCancelled(true);
        if (attacker != null) {
            warnClaimed(attacker);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent) return;
        if (!manager.isClaimed(event.getEntity().getLocation())) return;

        event.setCancelled(true);
    }
}
