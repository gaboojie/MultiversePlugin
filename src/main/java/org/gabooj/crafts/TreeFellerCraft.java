package org.gabooj.crafts;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class TreeFellerCraft {

    public static NamespacedKey treeFellerDiamondKey, treeFellerNetheriteKey;

    public static void addTreeFellerRecipes(JavaPlugin plugin) {
        // Diamond axe
        ItemStack diamondResult = new ItemStack(Material.DIAMOND_AXE);
        List<Component> diamondLore = List.of(
                Component.text("Sneak and break a log to break all connected logs!", NamedTextColor.LIGHT_PURPLE)
        );
        setMeta(diamondResult, diamondLore, Component.text("Tree Feller", NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD));

        treeFellerDiamondKey = new NamespacedKey(plugin, "TreeFellerDiamond");
        ShapedRecipe diamondRecipe = new ShapedRecipe(treeFellerDiamondKey, diamondResult);
        diamondRecipe.shape("aaa", "aba", "aaa");
        diamondRecipe.setIngredient('a', new RecipeChoice.MaterialChoice(Material.DIAMOND));
        diamondRecipe.setIngredient('b', new RecipeChoice.MaterialChoice(Material.DIAMOND_AXE));
        plugin.getServer().addRecipe(diamondRecipe);

        // Netherite axe
        ItemStack netheriteResult = new ItemStack(Material.NETHERITE_AXE);
        List<Component> netheriteLore = List.of(
                Component.text("Sneak and break a log to break all connected logs!", NamedTextColor.LIGHT_PURPLE)
        );
        setMeta(netheriteResult, netheriteLore, Component.text("Tree Feller", NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD));

        treeFellerNetheriteKey = new NamespacedKey(plugin, "TreeFellerNetherite");
        ShapedRecipe netheriteRecipe = new ShapedRecipe(treeFellerNetheriteKey, netheriteResult);
        netheriteRecipe.shape("aaa", "aba", "aaa");
        netheriteRecipe.setIngredient('a', new RecipeChoice.MaterialChoice(Material.DIAMOND));
        netheriteRecipe.setIngredient('b', new RecipeChoice.MaterialChoice(Material.NETHERITE_AXE));
        plugin.getServer().addRecipe(netheriteRecipe);
    }

    public static void removeTreeFellerRecipes(JavaPlugin plugin) {
        Bukkit.removeRecipe(treeFellerDiamondKey);
        Bukkit.removeRecipe(treeFellerNetheriteKey);
    }

    public static void setMeta(ItemStack item, List<Component> lore, Component displayName) {
        ItemMeta meta = item.getItemMeta();
        meta.lore(lore);
        meta.displayName(displayName);
        item.setItemMeta(meta);
    }
}
