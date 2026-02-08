package org.gabooj.commands.showHand;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.utils.Messager;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ShowHandCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public ShowHandCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("hand");
        if (command == null) {
            plugin.getLogger().warning("Command 'hand' is not defined in plugin.yml");
            return;
        }
        command.setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            Messager.sendWarningMessage(sender, "You must be a player to execute this command.");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            Messager.sendWarningMessage(player, "You are not holding an item.");
            return true;
        }

        Component hoverText = buildItemHoverText(item);
        Component msg = Component.text(player.getName() + " is showing their hand.", NamedTextColor.GOLD)
                .hoverEvent(HoverEvent.showText(hoverText));
        plugin.getServer().broadcast(msg);

        return true;
    }

    private static Component buildItemHoverText(ItemStack item) {
        List<Component> lines = new ArrayList<>();

        ItemMeta meta = item.getItemMeta();

        Component name = (meta != null && meta.hasDisplayName() && meta.displayName() != null)
                ? meta.displayName()
                : Component.text(humanizeIdentifier(item.getType().name()), NamedTextColor.WHITE);
        lines.add(name);

        lines.add(Component.text("Material: ", NamedTextColor.GRAY)
                .append(Component.text(item.getType().name(), NamedTextColor.WHITE))
                .append(Component.text("  x" + item.getAmount(), NamedTextColor.GRAY)));

        if (meta instanceof Damageable damageable && item.getType().getMaxDurability() > 0) {
            int max = item.getType().getMaxDurability();
            int remaining = Math.max(0, max - damageable.getDamage());
            lines.add(Component.text("Durability: ", NamedTextColor.GRAY)
                    .append(Component.text(remaining + "/" + max, NamedTextColor.WHITE)));
        }

        if (meta != null && meta.isUnbreakable()) {
            lines.add(Component.text("Unbreakable: ", NamedTextColor.GRAY)
                    .append(Component.text("true", NamedTextColor.WHITE)));
        }

        if (meta instanceof Repairable repairable && repairable.hasRepairCost()) {
            lines.add(Component.text("Repair Cost: ", NamedTextColor.GRAY)
                    .append(Component.text(String.valueOf(repairable.getRepairCost()), NamedTextColor.WHITE)));
        }

        // Regular enchants
        Map<Enchantment, Integer> enchants = item.getEnchantments();
        if (!enchants.isEmpty()) {
            lines.add(Component.text("Enchantments:", NamedTextColor.GOLD));
            enchants.entrySet().stream()
                    .sorted(Comparator.comparing(e -> e.getKey().getKey().toString()))
                    .forEach(entry -> lines.add(formatEnchantmentLine(entry.getKey(), entry.getValue())));
        }

        // Stored enchants (enchanted books)
        if (meta instanceof EnchantmentStorageMeta storageMeta && storageMeta.hasStoredEnchants()) {
            lines.add(Component.text("Stored Enchantments:", NamedTextColor.GOLD));
            storageMeta.getStoredEnchants().entrySet().stream()
                    .sorted(Comparator.comparing(e -> e.getKey().getKey().toString()))
                    .forEach(entry -> lines.add(formatEnchantmentLine(entry.getKey(), entry.getValue())));
        }

        // Lore
        if (meta != null && meta.lore() != null && !meta.lore().isEmpty()) {
            lines.add(Component.text("Lore:", NamedTextColor.GOLD));
            for (Component loreLine : meta.lore()) {
                lines.add(loreLine);
            }
        }

        // Item-type specific extras
        if (meta instanceof PotionMeta potionMeta) {
            if (potionMeta.getBasePotionType() != null) {
                lines.add(Component.text("Potion: ", NamedTextColor.GRAY)
                        .append(Component.text(potionMeta.getBasePotionType().name(), NamedTextColor.WHITE)));
            }
            if (potionMeta.hasCustomEffects()) {
                lines.add(Component.text("Effects:", NamedTextColor.GOLD));
                potionMeta.getCustomEffects().forEach(effect -> {
                    String duration = formatDuration(effect.getDuration());
                    String amplifier = String.valueOf(effect.getAmplifier() + 1);
                    lines.add(Component.text("• ", NamedTextColor.GRAY)
                            .append(Component.text(effect.getType().getKey().getKey().toUpperCase(Locale.ROOT), NamedTextColor.WHITE))
                            .append(Component.text(" " + amplifier + " (" + duration + ")", NamedTextColor.GRAY)));
                });
            }
        } else if (meta instanceof BookMeta bookMeta) {
            if (bookMeta.hasTitle()) {
                lines.add(Component.text("Title: ", NamedTextColor.GRAY)
                        .append(Component.text(bookMeta.getTitle(), NamedTextColor.WHITE)));
            }
            if (bookMeta.hasAuthor()) {
                lines.add(Component.text("Author: ", NamedTextColor.GRAY)
                        .append(Component.text(bookMeta.getAuthor(), NamedTextColor.WHITE)));
            }
            lines.add(Component.text("Pages: ", NamedTextColor.GRAY)
                    .append(Component.text(String.valueOf(bookMeta.getPageCount()), NamedTextColor.WHITE)));
        } else if (meta instanceof SkullMeta skullMeta) {
            if (skullMeta.getOwningPlayer() != null && skullMeta.getOwningPlayer().getName() != null) {
                lines.add(Component.text("Skull Owner: ", NamedTextColor.GRAY)
                        .append(Component.text(skullMeta.getOwningPlayer().getName(), NamedTextColor.WHITE)));
            }
        } else if (meta instanceof MapMeta mapMeta) {
            if (mapMeta.hasMapView() && mapMeta.getMapView() != null) {
                lines.add(Component.text("Map ID: ", NamedTextColor.GRAY)
                        .append(Component.text(String.valueOf(mapMeta.getMapView().getId()), NamedTextColor.WHITE)));
            }
        }

        Component tooltip = Component.empty();
        boolean first = true;
        for (Component line : lines) {
            if (!first) tooltip = tooltip.append(Component.newline());
            tooltip = tooltip.append(line);
            first = false;
        }
        return tooltip;
    }

    private static Component formatEnchantmentLine(Enchantment enchantment, int level) {
        String name = humanizeIdentifier(enchantment.getKey().getKey());
        String lvl = toRoman(level);
        return Component.text("• ", NamedTextColor.GRAY)
                .append(Component.text(name, NamedTextColor.WHITE))
                .append(Component.text(" " + lvl, NamedTextColor.GRAY));
    }

    private static String formatDuration(int ticks) {
        Duration d = Duration.ofSeconds(ticks / 20L);
        long minutes = d.toMinutes();
        long seconds = d.minusMinutes(minutes).toSeconds();
        return String.format(Locale.ROOT, "%d:%02d", minutes, seconds);
    }

    private static String humanizeIdentifier(String raw) {
        String[] parts = raw.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) sb.append(part.substring(1));
        }
        return sb.toString();
    }

    private static String toRoman(int number) {
        if (number <= 0) return String.valueOf(number);
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(number);
        };
    }

}
