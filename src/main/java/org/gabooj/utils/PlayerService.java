package org.gabooj.utils;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class PlayerService {

    public static void removeFromInventory(Inventory inventory, Material material, int amount) {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() == material) {
                // Update quantity of item
                int itemAmount = item.getAmount();
                int amountToRemove = Math.min(amount, itemAmount);
                item.setAmount(itemAmount - amountToRemove);
                amount -= amountToRemove;

                // If item is now empty, remove item
                if (itemAmount <= 0) {
                    inventory.setItem(i, null);
                }

                // If all items removed, stop removing
                if (amount <= 0) {
                    break;
                }
            }
        }
    }

}
