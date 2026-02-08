package org.gabooj.items.builderWand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.gabooj.crafts.BuilderWandCraft;
import org.gabooj.utils.Messager;
import org.gabooj.utils.PlayerService;

public class BuilderWandModule implements Listener {

    private final JavaPlugin plugin;

    private static final HashMap<BlockFace, List<Vector>> perpendicularDirectionsMap = new HashMap<>();
    private static final HashMap<Player, Long> cooldowns = new HashMap<>();
    private static final String BUILDERS_WAND_PLAIN_NAME = "Builder's Wand";

    public BuilderWandModule(JavaPlugin plugin) {
        this.plugin = plugin;
        populatePerpendicularDirections();
    }

    public void enable() {
        // Register event listener
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Add recipe
        BuilderWandCraft.addBuildersWandRecipe(plugin);
    }

    public void disable() {
        // Remove recipe
        BuilderWandCraft.removeBuildersWandRecipe(plugin);

        // Unregister event listener
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.discoverRecipe(BuilderWandCraft.buildersWandKey);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && item != null && item.getType() == Material.STICK && doesItemHaveName(item, BUILDERS_WAND_PLAIN_NAME) && event.getClickedBlock() != null) {
            extendBlocks(event.getClickedBlock(), event.getPlayer(), event.getBlockFace());
        }
    }

    public boolean doesItemHaveName(ItemStack item, String name) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        Component displayName = meta.displayName();
        if (displayName == null) return false;

        String plainName = PlainTextComponentSerializer.plainText().serialize(displayName);
        if (!plainName.equalsIgnoreCase(name)) return false;

        if (displayName.color() != NamedTextColor.LIGHT_PURPLE) return false;
        return displayName.decoration(TextDecoration.BOLD) == TextDecoration.State.TRUE;
    }

    public void extendBlocks(Block block, Player player, BlockFace face) {
        long now = System.currentTimeMillis();
        if (cooldowns.containsKey(player) && (now - cooldowns.get(player)) < 500) {
            return;
        }
        cooldowns.put(player, now);

        List<Block> blocksToExtend = new ArrayList<>();
        blocksToExtend.add(block);
        int blocksAdded = 1;
        Stack<Block> stack = new Stack<>();
        stack.add(block);

        List<Vector> directions = perpendicularDirectionsMap.getOrDefault(face, new ArrayList<>());
        if (directions.isEmpty()) return;

        while (!stack.isEmpty() && blocksAdded < 64) {
            Block blockOn = stack.pop();

            // Search for more blocks to extend in each direction
            for (Vector direction : directions) {
                Block adjacentBlock = blockOn.getRelative(direction.getBlockX(), direction.getBlockY(), direction.getBlockZ());
                Block blockToPlace = adjacentBlock.getRelative(face);

                if (adjacentBlock.getType() == block.getType() && (blockToPlace.getType() == Material.AIR || blockToPlace.getType() == Material.WATER) && !blocksToExtend.contains(adjacentBlock)) {
                    blocksAdded += 1;
                    stack.add(adjacentBlock);
                    blocksToExtend.add(adjacentBlock);
                }
            }
        }

        // Ensure player has enough items
        if (!player.getInventory().contains(block.getType(), blocksAdded)) {
            Messager.sendWarningMessage(player, "You do not have enough blocks! (You need " + blocksAdded + " " + block.getType().toString() + ")");
            return;
        }

        // Place blocks
        for (Block blockOn : blocksToExtend) {
            Block blockToPlace = blockOn.getRelative(face);
            blockToPlace.setType(block.getType());
        }

        // Remove from player inventory
        PlayerService.removeFromInventory(player.getInventory(), block.getType(), blocksAdded);
    }

    private static void populatePerpendicularDirections() {
        perpendicularDirectionsMap.put(BlockFace.UP, createPerpendicularVectors(new Vector(0, 1, 0)));
        perpendicularDirectionsMap.put(BlockFace.DOWN, createPerpendicularVectors(new Vector(0, -1, 0)));
        perpendicularDirectionsMap.put(BlockFace.NORTH, createPerpendicularVectors(new Vector(0, 0, 1)));
        perpendicularDirectionsMap.put(BlockFace.SOUTH, createPerpendicularVectors(new Vector(0, 0, -1)));
        perpendicularDirectionsMap.put(BlockFace.EAST, createPerpendicularVectors(new Vector(1, 0, 0)));
        perpendicularDirectionsMap.put(BlockFace.WEST, createPerpendicularVectors(new Vector(-1, 0, 0)));
    }

    // Helper method to create the perpendicular vectors for a given block face vector
    private static List<Vector> createPerpendicularVectors(Vector direction) {
        List<Vector> perpendicularVectors = new ArrayList<>();

        if (direction.getX() == 0 && direction.getY() == 1 && direction.getZ() == 0) { // UP
            perpendicularVectors.add(new Vector(1, 0, 0));  // Positive X
            perpendicularVectors.add(new Vector(-1, 0, 0)); // Negative X
            perpendicularVectors.add(new Vector(0, 0, 1));  // Positive Z
            perpendicularVectors.add(new Vector(0, 0, -1)); // Negative Z
        } else if (direction.getX() == 0 && direction.getY() == -1 && direction.getZ() == 0) { // DOWN
            perpendicularVectors.add(new Vector(1, 0, 0));  // Positive X
            perpendicularVectors.add(new Vector(-1, 0, 0)); // Negative X
            perpendicularVectors.add(new Vector(0, 0, 1));  // Positive Z
            perpendicularVectors.add(new Vector(0, 0, -1)); // Negative Z
        } else if (direction.getX() == 0 && direction.getY() == 0 && direction.getZ() == 1) { // NORTH
            perpendicularVectors.add(new Vector(1, 0, 0));  // Positive X
            perpendicularVectors.add(new Vector(-1, 0, 0)); // Negative X
            perpendicularVectors.add(new Vector(0, 1, 0));  // Positive Y
            perpendicularVectors.add(new Vector(0, -1, 0)); // Negative Y
        } else if (direction.getX() == 0 && direction.getY() == 0 && direction.getZ() == -1) { // SOUTH
            perpendicularVectors.add(new Vector(1, 0, 0));  // Positive X
            perpendicularVectors.add(new Vector(-1, 0, 0)); // Negative X
            perpendicularVectors.add(new Vector(0, 1, 0));  // Positive Y
            perpendicularVectors.add(new Vector(0, -1, 0)); // Negative Y
        } else if (direction.getX() == 1 && direction.getY() == 0 && direction.getZ() == 0) { // EAST
            perpendicularVectors.add(new Vector(0, 1, 0));  // Positive Y
            perpendicularVectors.add(new Vector(0, -1, 0)); // Negative Y
            perpendicularVectors.add(new Vector(0, 0, 1));  // Positive Z
            perpendicularVectors.add(new Vector(0, 0, -1)); // Negative Z
        } else if (direction.getX() == -1 && direction.getY() == 0 && direction.getZ() == 0) { // WEST
            perpendicularVectors.add(new Vector(0, 1, 0));  // Positive Y
            perpendicularVectors.add(new Vector(0, -1, 0)); // Negative Y
            perpendicularVectors.add(new Vector(0, 0, 1));  // Positive Z
            perpendicularVectors.add(new Vector(0, 0, -1)); // Negative Z
        }

        return perpendicularVectors;
    }
}
