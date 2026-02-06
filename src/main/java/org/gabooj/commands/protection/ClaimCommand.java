package org.gabooj.commands.protection;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.protection.LandProtectionManager;
import org.gabooj.utils.Messager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ClaimCommand implements CommandExecutor, TabCompleter {

    private final LandProtectionManager protectionManager;

    public ClaimCommand(JavaPlugin plugin, LandProtectionManager protectionManager) {
        this.protectionManager = protectionManager;

        PluginCommand command = plugin.getCommand("claim");
        if (command == null) {
            plugin.getLogger().warning("Command 'claim' is not defined in plugin.yml");
            return;
        }
        command.setExecutor(this);
        command.setTabCompleter(this);
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String @NotNull [] args
    ) {
        if (!(sender instanceof Player player)) {
            Messager.sendWarningMessage(sender, "You must be a player to execute this command.");
            return true;
        }

        if (!player.isOp()) {
            Messager.sendWarningMessage(player, "You must be OP to use this command.");
            return true;
        }

        if (args.length != 1) {
            Messager.sendInfoMessage(player, "Usage: /claim <on|off|info>");
            return true;
        }

        int chunkX = player.getLocation().getBlockX() >> 4;
        int chunkZ = player.getLocation().getBlockZ() >> 4;
        int sectionY = player.getLocation().getBlockY() >> 4;
        UUID worldId = player.getWorld().getUID();
        long chunkKey = LandProtectionManager.chunkKey(chunkX, chunkZ);

        switch (args[0].toLowerCase()) {
            case "on" -> {
                Map<Long, Set<Integer>> worldClaims = LandProtectionManager.protectionsByWorld
                        .computeIfAbsent(worldId, ignored -> new HashMap<>());
                Set<Integer> sections = worldClaims.computeIfAbsent(chunkKey, ignored -> new HashSet<>());
                if (sections.add(sectionY)) {
                    Messager.sendSuccessMessage(player, "Claimed chunk (" + chunkX + ", " + sectionY + ", " + chunkZ + ").");
                } else {
                    Messager.sendInfoMessage(player, "Chunk (" + chunkX + ", " + sectionY + ", " + chunkZ + ") is already claimed.");
                }
                return true;
            }
            case "off" -> {
                Map<Long, Set<Integer>> worldClaims = LandProtectionManager.protectionsByWorld.get(worldId);
                if (worldClaims == null) {
                    Messager.sendInfoMessage(player, "Chunk (" + chunkX + ", " + sectionY + ", " + chunkZ + ") is not claimed.");
                    return true;
                }
                Set<Integer> sections = worldClaims.get(chunkKey);
                if (sections == null || !sections.remove(sectionY)) {
                    Messager.sendInfoMessage(player, "Chunk (" + chunkX + ", " + sectionY + ", " + chunkZ + ") is not claimed.");
                    return true;
                }
                if (sections.isEmpty()) {
                    worldClaims.remove(chunkKey);
                }
                if (worldClaims.isEmpty()) {
                    LandProtectionManager.protectionsByWorld.remove(worldId);
                }
                Messager.sendSuccessMessage(player, "Unclaimed chunk (" + chunkX + ", " + sectionY + ", " + chunkZ + ").");
                return true;
            }
            case "info" -> {
                boolean claimed = protectionManager.isClaimed(worldId, chunkX, chunkZ, sectionY);
                String worldName = player.getWorld().getName();
                String status = claimed ? "claimed" : "not claimed";
                Messager.sendInfoMessage(player, "Chunk (" + chunkX + ", " + sectionY + ", " + chunkZ + ") in " + worldName + " is " + status + ".");
                return true;
            }
            default -> {
                Messager.sendInfoMessage(player, "Usage: /claim <on|off|info>");
                return true;
            }
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String @NotNull [] args
    ) {
        if (args.length != 1) {
            return List.of();
        }
        String partial = args[0].toLowerCase();
        return List.of("on", "off", "info").stream()
                .filter(option -> option.startsWith(partial))
                .toList();
    }
}
