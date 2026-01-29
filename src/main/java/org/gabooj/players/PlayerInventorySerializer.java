package org.gabooj.players;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.scope.ScopeMeta;

public class PlayerInventorySerializer {

    public static void clearInventoryStateOnDeath(Player player, ScopeMeta scopeMeta, JavaPlugin plugin) {
        PersistentDataContainer scopeContainer = PlayerSerializer.getOrCreateScopePDC(player, scopeMeta, plugin);

        scopeContainer.remove(PlayerSerializer.INV_CONTENTS);
        scopeContainer.remove(PlayerSerializer.ARMOR_CONTENTS);
        scopeContainer.remove(PlayerSerializer.OFFHAND);

        PlayerSerializer.savePrimaryPDC(player, scopeMeta, plugin, scopeContainer);
    }

    public static void saveInventory(Player player, ScopeMeta scopeMeta, JavaPlugin plugin) {
        PersistentDataContainer scopeContainer = PlayerSerializer.getOrCreateScopePDC(player, scopeMeta, plugin);

        scopeContainer.set(PlayerSerializer.INV_CONTENTS, PersistentDataType.BYTE_ARRAY, PlayerSerializer.serialize(player.getInventory().getContents()));
        scopeContainer.set(PlayerSerializer.ARMOR_CONTENTS, PersistentDataType.BYTE_ARRAY, PlayerSerializer.serialize(player.getInventory().getArmorContents()));
        scopeContainer.set(PlayerSerializer.OFFHAND, PersistentDataType.BYTE_ARRAY, PlayerSerializer.serialize(
            new ItemStack[] { player.getInventory().getItemInOffHand() }
        ));

        PlayerSerializer.savePrimaryPDC(player, scopeMeta, plugin, scopeContainer);
    }

    public static void loadInventory(Player player, ScopeMeta scopeMeta, JavaPlugin plugin) {
        PersistentDataContainer scopeContainer = PlayerSerializer.getScopePDC(player, scopeMeta, plugin);
        PlayerInventory inv = player.getInventory();

        // If container DNE, clear inventory
        if (scopeContainer == null) {
            inv.clear();
            inv.setArmorContents(new ItemStack[4]);
            inv.setItemInOffHand(null);
            return;
        }

        // --- Inventory contents ---
        byte[] invBytes = scopeContainer.get(PlayerSerializer.INV_CONTENTS, PersistentDataType.BYTE_ARRAY);
        if (invBytes != null) {
            ItemStack[] contents = PlayerSerializer.deserialize(invBytes);
            player.getInventory().setContents(contents);
        } else {
            inv.clear();
        }

        // --- Armor ---
        byte[] armorBytes = scopeContainer.get(PlayerSerializer.ARMOR_CONTENTS, PersistentDataType.BYTE_ARRAY);
        if (armorBytes != null) {
            ItemStack[] armor = PlayerSerializer.deserialize(armorBytes);
            player.getInventory().setArmorContents(armor);
        } else {
            inv.setArmorContents(new ItemStack[4]);
        }

        // --- Offhand ---
        byte[] offhandBytes = scopeContainer.get(PlayerSerializer.OFFHAND, PersistentDataType.BYTE_ARRAY);
        if (offhandBytes != null) {
            ItemStack[] offhand = PlayerSerializer.deserialize(offhandBytes);
            player.getInventory().setItemInOffHand(
                    offhand.length > 0 ? offhand[0] : null
            );
        } else {
            inv.setItemInOffHand(null);
        }

        // Force client sync (important after teleports)
        player.updateInventory();
    }

}