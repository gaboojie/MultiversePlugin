package org.gabooj.items.treeFeller;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class TreeFeller {

    private static final int MAX_BLOCKS = 200;
    private static final String TREE_FELLER_PLAIN_NAME = "Tree Feller";

    public static void handleBlockBreakEvent(BlockBreakEvent event, JavaPlugin plugin) {
        if (event.getPlayer().isSneaking() && isLog(event.getBlock())) {
            ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
            if (isTreeFeller(item)) {
                runTreeFeller(event, plugin);
            }
        }
    }

    public static void runTreeFeller(BlockBreakEvent event, JavaPlugin plugin) {
        // Get blocks and trees
        Material logType = event.getBlock().getType();
        Material leafType = LEAF_BY_LOG.get(logType);

        // Scan for logs and leaves of these types
        TreeScanResult result = scan(event.getBlock(), logType, leafType);

        if (!result.isValidTree()) return;
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();

        new TreeBreakTask(result.allBlocks().iterator(), event.getPlayer(), tool).runTaskTimer(plugin, 0L, 3L);
    }

    // Helper functions

    public static boolean isLog(Block block) {
        return switch (block.getType()) {
            case OAK_LOG, SPRUCE_LOG, CHERRY_LOG, BIRCH_LOG, JUNGLE_LOG, ACACIA_LOG, DARK_OAK_LOG, CRIMSON_STEM, WARPED_STEM, MANGROVE_LOG, MANGROVE_ROOTS, MUDDY_MANGROVE_ROOTS, PALE_OAK_LOG -> true;
            default -> false;
        };
    }

    public static boolean isTreeFeller(ItemStack item) {
        if (item == null) return false;
        if (item.getType() != Material.DIAMOND_AXE && item.getType() != Material.NETHERITE_AXE) return false;
        return doesItemHaveName(item, TREE_FELLER_PLAIN_NAME);
    }

    public static boolean doesItemHaveName(ItemStack item, String name) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        Component displayName = meta.displayName();
        if (displayName == null) return false;

        String plainName = PlainTextComponentSerializer.plainText().serialize(displayName);
        if (!plainName.equalsIgnoreCase(name)) return false;

        if (displayName.color() != NamedTextColor.LIGHT_PURPLE) return false;
        return displayName.decoration(TextDecoration.BOLD) == TextDecoration.State.TRUE;
    }

    private static final EnumMap<Material, Material> LEAF_BY_LOG = new EnumMap<>(Material.class);
    static {
        LEAF_BY_LOG.put(Material.OAK_LOG, Material.OAK_LEAVES);
        LEAF_BY_LOG.put(Material.BIRCH_LOG, Material.BIRCH_LEAVES);
        LEAF_BY_LOG.put(Material.SPRUCE_LOG, Material.SPRUCE_LEAVES);
        LEAF_BY_LOG.put(Material.JUNGLE_LOG, Material.JUNGLE_LEAVES);
        LEAF_BY_LOG.put(Material.ACACIA_LOG, Material.ACACIA_LEAVES);
        LEAF_BY_LOG.put(Material.DARK_OAK_LOG, Material.DARK_OAK_LEAVES);
        LEAF_BY_LOG.put(Material.MANGROVE_LOG, Material.MANGROVE_LEAVES);
        LEAF_BY_LOG.put(Material.CHERRY_LOG, Material.CHERRY_LEAVES);
        LEAF_BY_LOG.put(Material.PALE_OAK_LOG, Material.PALE_OAK_LEAVES);
    }

    public static TreeScanResult scan(Block start, Material logType, Material leafType) {
        Set<Block> logs = new HashSet<>();
        Set<Block> leaves = new HashSet<>();
        Queue<Block> queue = new ArrayDeque<>();

        // Use BFS to get all leaves and blocks that match log type and leaf type
        queue.add(start);
        logs.add(start);
        while (!queue.isEmpty() && (logs.size() + leaves.size()) < MAX_BLOCKS) {
            Block b = queue.poll();

            for (BlockFace face : BlockFace.values()) {
                if (!face.isCartesian()) continue;

                Block n = b.getRelative(face);
                if (n.getType() == logType && logs.add(n)) {
                    queue.add(n);
                } else if (n.getType() == leafType && leaves.add(n)) {
                    queue.add(n);
                }
            }
        }
        return new TreeScanResult(logs, leaves);
    }



}
