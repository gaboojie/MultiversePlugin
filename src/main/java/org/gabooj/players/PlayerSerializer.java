package org.gabooj.players;

import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.gabooj.scope.ScopeMeta;

import java.io.*;

@SuppressWarnings("deprecation")
public class PlayerSerializer {

    // Last scope
    public static NamespacedKey LAST_SCOPE_KEY;

    // Inventory
    public static NamespacedKey INV_CONTENTS;
    public static NamespacedKey ARMOR_CONTENTS;
    public static NamespacedKey OFFHAND;

    // Location
    public static NamespacedKey LOC_KEY;
    public static NamespacedKey LOC_X_KEY;
    public static NamespacedKey LOC_Y_KEY;
    public static NamespacedKey LOC_Z_KEY;
    public static NamespacedKey LOC_YAW_KEY;
    public static NamespacedKey LOC_WORLD_KEY;
    public static NamespacedKey LOC_PITCH_KEY;

    // Bed location
    public static NamespacedKey BED_X_KEY;
    public static NamespacedKey BED_Y_KEY;
    public static NamespacedKey BED_Z_KEY;
    public static NamespacedKey BED_WORLD_KEY;

    // Misc
    public static NamespacedKey ENDERCHEST_KEY;
    public static NamespacedKey FIRE_TICKS_KEY;
    public static NamespacedKey POTION_EFFECTS_KEY;
    public static NamespacedKey XP_KEY;
    public static NamespacedKey LEVEL_KEY;
    public static NamespacedKey HEALTH_KEY;
    public static NamespacedKey HUNGER_KEY;
    public static NamespacedKey SATURATION_KEY;


    public static void init(JavaPlugin plugin) {
        // Last scope
        LAST_SCOPE_KEY = new NamespacedKey(plugin, "last_scope_world");

        // Inventory
        INV_CONTENTS = new NamespacedKey(plugin, "inventory");
        ARMOR_CONTENTS = new NamespacedKey(plugin, "armor");
        OFFHAND = new NamespacedKey(plugin, "offhand");

        // Location
        LOC_KEY = new NamespacedKey(plugin, "last_location");
        LOC_X_KEY = new NamespacedKey(plugin, "last_location_x");
        LOC_Y_KEY = new NamespacedKey(plugin, "last_location_y");
        LOC_Z_KEY = new NamespacedKey(plugin, "last_location_z");
        LOC_YAW_KEY = new NamespacedKey(plugin, "last_location_yaw");
        LOC_PITCH_KEY = new NamespacedKey(plugin, "last_location_pitch");
        LOC_WORLD_KEY = new NamespacedKey(plugin, "last_location_world");

        // Bed location
        BED_X_KEY = new NamespacedKey(plugin, "bed_x");
        BED_Y_KEY = new NamespacedKey(plugin, "bed_y");
        BED_Z_KEY = new NamespacedKey(plugin, "bed_z");
        BED_WORLD_KEY = new NamespacedKey(plugin, "bed_world");

        // Misc
        ENDERCHEST_KEY = new NamespacedKey(plugin, "ender_chest");
        FIRE_TICKS_KEY = new NamespacedKey(plugin, "fire_ticks");
        POTION_EFFECTS_KEY = new NamespacedKey(plugin, "potion_effects");
        XP_KEY = new NamespacedKey(plugin, "xp");
        LEVEL_KEY = new NamespacedKey(plugin, "level");
        HEALTH_KEY = new NamespacedKey(plugin, "health");
        HUNGER_KEY = new NamespacedKey(plugin, "hunger");
        SATURATION_KEY = new NamespacedKey(plugin, "saturation");
    }

    public static NamespacedKey scopeKey(JavaPlugin plugin, String scopeID) {
        return new NamespacedKey(plugin, "scope_" + scopeID);
    }

    public static byte[] serializePotionEffects(PotionEffect[] effects) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {

            oos.writeInt(effects.length);
            for (PotionEffect effect : effects) {
                oos.writeUTF(effect.getType().getName());  // effect type
                oos.writeInt(effect.getAmplifier());       // level
                oos.writeInt(effect.getDuration());        // duration in ticks
                oos.writeBoolean(effect.isAmbient());
                oos.writeBoolean(effect.hasParticles());
                oos.writeBoolean(effect.hasIcon());
            }

            oos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    public static PotionEffect[] deserializePotionEffects(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return new PotionEffect[0];

        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bais)) {

            int length = ois.readInt();
            PotionEffect[] effects = new PotionEffect[length];

            for (int i = 0; i < length; i++) {
                PotionEffectType type = PotionEffectType.getByName(ois.readUTF());
                int amplifier = ois.readInt();
                int duration = ois.readInt();
                boolean ambient = ois.readBoolean();
                boolean particles = ois.readBoolean();
                boolean icon = ois.readBoolean();

                if (type != null) {
                    effects[i] = new PotionEffect(type, duration, amplifier, ambient, particles, icon);
                } else {
                    effects[i] = null; // skip invalid types
                }
            }

            return effects;
        } catch (IOException e) {
            e.printStackTrace();
            return new PotionEffect[0];
        }
    }

    public static PersistentDataContainer getOrCreateScopePDC(Player player, ScopeMeta scopeMeta, JavaPlugin plugin) {
        NamespacedKey scopeKey = scopeKey(plugin, scopeMeta.getScopeId());
        PersistentDataContainer root = player.getPersistentDataContainer();

        PersistentDataContainer scopeContainer = root.get(scopeKey, PersistentDataType.TAG_CONTAINER);

        if (scopeContainer == null) {
            scopeContainer = root.getAdapterContext().newPersistentDataContainer();
            root.set(scopeKey, PersistentDataType.TAG_CONTAINER, scopeContainer);
        }

        return scopeContainer;
    }

    public static void savePrimaryPDC(Player player, ScopeMeta scopeMeta, JavaPlugin plugin, PersistentDataContainer pdc) {
        PersistentDataContainer root = player.getPersistentDataContainer();
        NamespacedKey scopeKey = scopeKey(plugin, scopeMeta.getScopeId());
        root.set(scopeKey, PersistentDataType.TAG_CONTAINER, pdc);
    }

    public static PersistentDataContainer getScopePDC(Player player, ScopeMeta scopeMeta, JavaPlugin plugin) {
        NamespacedKey scopeKey = scopeKey(plugin, scopeMeta.getScopeId());
        return player.getPersistentDataContainer().get(scopeKey, PersistentDataType.TAG_CONTAINER);
    }

    public static PersistentDataContainer getScopePDC(OfflinePlayer player, ScopeMeta scopeMeta, JavaPlugin plugin) {
        NamespacedKey scopeKey = scopeKey(plugin, scopeMeta.getScopeId());
        return player.getPersistentDataContainer().get(scopeKey, PersistentDataType.TAG_CONTAINER);
    }

    public static byte[] serialize(ItemStack[] items) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BukkitObjectOutputStream oos = new BukkitObjectOutputStream(baos)) {

            oos.writeInt(items.length);
            for (ItemStack item : items) {
                oos.writeObject(item);
            }
            return baos.toByteArray();

        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize inventory", e);
        }
    }

    public static ItemStack[] deserialize(byte[] data) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             BukkitObjectInputStream ois = new BukkitObjectInputStream(bais)) {

            int size = ois.readInt();
            ItemStack[] items = new ItemStack[size];

            for (int i = 0; i < size; i++) {
                items[i] = (ItemStack) ois.readObject();
            }
            return items;

        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Failed to deserialize inventory", e);
        }
    }

}
