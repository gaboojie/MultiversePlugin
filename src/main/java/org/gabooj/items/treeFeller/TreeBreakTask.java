package org.gabooj.items.treeFeller;

import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Iterator;

public class TreeBreakTask extends BukkitRunnable {

    private static final int BLOCKS_PER_TICK = 8;

    private final Iterator<Block> iterator;
    private final Player player;
    private final ItemStack tool;

    public TreeBreakTask(Iterator<Block> iterator, Player player, ItemStack tool) {
        this.iterator = iterator;
        this.player = player;
        this.tool = tool;
    }

    @Override
    public void run() {
        if (!player.isOnline()) {
            cancel();
            return;
        }

        int count = 0;

        while (iterator.hasNext() && count++ < BLOCKS_PER_TICK) {
            Block b = iterator.next();
            if (b.getType().isAir()) continue;

            b.breakNaturally(tool, true);

            if (damageTool(tool)) {
                cancel();
                return;
            }
        }

        if (!iterator.hasNext()) {
            cancel();
        }
    }

    private boolean damageTool(ItemStack tool) {
        if (!(tool.getItemMeta() instanceof Damageable meta)) return false;

        if (tool.getEnchantmentLevel(Enchantment.UNBREAKING) > 0 &&
                Math.random() < 1.0 / (tool.getEnchantmentLevel(Enchantment.UNBREAKING) + 1)) {
            return false;
        }

        meta.setDamage(meta.getDamage() + 1);
        tool.setItemMeta(meta);

        return meta.getDamage() >= tool.getType().getMaxDurability();
    }
}
