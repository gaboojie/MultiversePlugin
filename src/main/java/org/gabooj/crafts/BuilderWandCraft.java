package org.gabooj.crafts;

import java.util.List;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class BuilderWandCraft {

    public static NamespacedKey buildersWandKey;

    public static void addBuildersWandRecipe(JavaPlugin plugin) {
        // Builder's wand
        ItemStack result = new ItemStack(Material.STICK);
        result.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
        List<Component> lore = List.of(
                Component.text("Right click on a block to extend all connected blocks.", NamedTextColor.LIGHT_PURPLE)
        );
        setMeta(result, lore, Component.text("Builder's Wand", NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD));

        buildersWandKey = new NamespacedKey(plugin, "BuildersWand");
        ShapedRecipe recipe = new ShapedRecipe(buildersWandKey, result);

        // Add recipe
        recipe.shape(" s ", " s ", " s ");
        recipe.setIngredient('s', new RecipeChoice.MaterialChoice(Material.STICK));
        plugin.getServer().addRecipe(recipe);
    }

    public static void removeBuildersWandRecipe(JavaPlugin plugin) {
        Bukkit.removeRecipe(buildersWandKey);
    }

    public static void setMeta(ItemStack item, List<Component> lore, Component displayName) {
        ItemMeta meta = item.getItemMeta();
        meta.lore(lore);
        meta.displayName(displayName);
        item.setItemMeta(meta);
    }

}
