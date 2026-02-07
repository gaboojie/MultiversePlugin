package org.gabooj.commands.tpa;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.gabooj.services.PlayerMoveService;
import org.gabooj.utils.Messager;
import org.gabooj.worlds.WorldManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TpaCommand implements CommandExecutor, TabCompleter {

    private static final long REQUEST_TIMEOUT_MS = 120_000L;

    private final Map<UUID, LinkedHashMap<UUID, TeleportRequest>> pendingRequestsByTarget = new ConcurrentHashMap<>();
    private final WorldManager worldManager;

    public TpaCommand(JavaPlugin plugin, WorldManager worldManager) {
        this.worldManager = worldManager;

        PluginCommand command = plugin.getCommand("tpa");
        if (command == null) {
            plugin.getLogger().warning("Command 'tpa' is not defined in plugin.yml");
            return;
        }
        command.setExecutor(this);
        command.setTabCompleter(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            Messager.sendWarningMessage(sender, "You must be a player to execute this command.");
            return true;
        }

        pruneExpiredRequestsFor(player.getUniqueId());

        if (args.length == 0) {
            sendInfo(player);
            return true;
        }

        String first = args[0].toLowerCase();
        switch (first) {
            case "accept" -> {
                if (args.length > 2) {
                    Messager.sendInfoMessage(sender, "Usage: /tpa accept [player]");
                    return true;
                }
                String requesterName = args.length == 2 ? args[1] : null;
                acceptRequest(player, requesterName);
                return true;
            }
            case "deny" -> {
                if (args.length > 2) {
                    Messager.sendInfoMessage(sender, "Usage: /tpa deny [player]");
                    return true;
                }
                String requesterName = args.length == 2 ? args[1] : null;
                denyRequest(player, requesterName);
                return true;
            }
            default -> {
                createRequest(player, args[0]);
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
        if (!(sender instanceof Player player)) {
            return List.of();
        }

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> matches = new ArrayList<>();
            if ("accept".startsWith(partial)) {
                matches.add("accept");
            }
            if ("deny".startsWith(partial)) {
                matches.add("deny");
            }
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getUniqueId().equals(player.getUniqueId())) {
                    continue;
                }
                if (online.getName().toLowerCase().startsWith(partial)) {
                    matches.add(online.getName());
                }
            }
            return matches;
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("accept") || args[0].equalsIgnoreCase("deny"))) {
            pruneExpiredRequestsFor(player.getUniqueId());
            LinkedHashMap<UUID, TeleportRequest> requests = pendingRequestsByTarget.get(player.getUniqueId());
            if (requests == null || requests.isEmpty()) {
                return List.of();
            }
            String partial = args[1].toLowerCase();
            return requests.values().stream()
                    .map(TeleportRequest::requesterName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .distinct()
                    .toList();
        }

        return List.of();
    }

    private void sendInfo(Player player) {
        String msg = """
                Teleport request commands:
                /tpa <player> - Request to teleport to a player.
                /tpa accept - Accept the oldest pending request.
                /tpa accept <player> - Accept a request from that player.
                /tpa deny - Deny the oldest pending request.
                /tpa deny <player> - Deny a request from that player.
                """;
        Messager.sendInfoMessage(player, msg);
    }

    private void createRequest(Player requester, String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !target.isOnline()) {
            Messager.sendWarningMessage(requester, "That player is not online.");
            return;
        }

        if (target.getUniqueId().equals(requester.getUniqueId())) {
            Messager.sendWarningMessage(requester, "You cannot send a teleport request to yourself.");
            return;
        }

        if (worldManager.scopeManager.doPlayerScopesMatch(requester, target)) {
            Messager.sendWarningMessage(requester, "You cannot teleport to a player in a different world!");
            return;
        }

        UUID targetId = target.getUniqueId();
        pruneExpiredRequestsFor(targetId);

        LinkedHashMap<UUID, TeleportRequest> pendingForTarget =
                pendingRequestsByTarget.computeIfAbsent(targetId, key -> new LinkedHashMap<>());
        if (pendingForTarget.containsKey(requester.getUniqueId())) {
            Messager.sendWarningMessage(requester, "You already have a pending teleport request to " + target.getName() + ".");
            return;
        }

        TeleportRequest request = new TeleportRequest(
                requester.getUniqueId(),
                requester.getName(),
                targetId,
                System.currentTimeMillis()
        );
        pendingForTarget.put(requester.getUniqueId(), request);

        Messager.sendInfoMessage(requester, "Teleport request sent to " + target.getName() + ".");
        Messager.sendInfoMessage(target, requester.getName() + " wants to teleport to you.");
        Messager.sendInfoMessage(target, "Use /tpa accept " + requester.getName() + " or /tpa deny " + requester.getName() + ".");
    }

    private void acceptRequest(Player target, @Nullable String requesterName) {
        UUID targetId = target.getUniqueId();
        pruneExpiredRequestsFor(targetId);

        TeleportRequest request = requesterName == null
                ? getFirstRequestFor(targetId)
                : getRequestFor(targetId, requesterName);
        if (request == null) {
            if (requesterName == null) {
                Messager.sendWarningMessage(target, "You have no pending teleport requests.");
            } else {
                Messager.sendWarningMessage(target, "No pending request from " + requesterName + ".");
            }
            return;
        }

        removeRequest(request);
        Player requester = Bukkit.getPlayer(request.requesterId());
        if (requester == null || !requester.isOnline()) {
            Messager.sendWarningMessage(target, request.requesterName() + " is no longer online.");
            return;
        }

        if (worldManager.scopeManager.doPlayerScopesMatch(requester, target)) {
            Messager.sendWarningMessage(target, "You cannot accept a teleport request from a player in a different world!");
            Messager.sendWarningMessage(requester, "Your teleport request to " + target.getName() + " was cancelled, because they are in a different world!");
            return;
        }

        PlayerMoveService.beginTeleportWarmup(
                requester,
                target.getLocation(),
                PlayerMoveService.DEFAULT_TPA_WARMUP_SECONDS,
                () -> {
                    Messager.sendInfoMessage(requester, "Teleported to " + target.getName() + ".");
                    Messager.sendInfoMessage(target, request.requesterName() + " teleported to you.");
                },
                () -> Messager.sendWarningMessage(requester, "Teleport cancelled.")
        );

        Messager.sendInfoMessage(target, "Accepted request from " + request.requesterName() + ".");
        Messager.sendInfoMessage(requester, target.getName() + " accepted your teleport request.");
    }

    private void denyRequest(Player target, @Nullable String requesterName) {
        UUID targetId = target.getUniqueId();
        pruneExpiredRequestsFor(targetId);

        TeleportRequest request = requesterName == null
                ? getFirstRequestFor(targetId)
                : getRequestFor(targetId, requesterName);
        if (request == null) {
            if (requesterName == null) {
                Messager.sendWarningMessage(target, "You have no pending teleport requests.");
            } else {
                Messager.sendWarningMessage(target, "No pending request from " + requesterName + ".");
            }
            return;
        }

        removeRequest(request);
        Messager.sendInfoMessage(target, "Denied request from " + request.requesterName() + ".");

        Player requester = Bukkit.getPlayer(request.requesterId());
        if (requester != null && requester.isOnline()) {
            Messager.sendWarningMessage(requester, target.getName() + " denied your teleport request.");
        }
    }

    private @Nullable TeleportRequest getFirstRequestFor(UUID targetId) {
        LinkedHashMap<UUID, TeleportRequest> requests = pendingRequestsByTarget.get(targetId);
        if (requests == null || requests.isEmpty()) {
            return null;
        }
        return requests.values().iterator().next();
    }

    private @Nullable TeleportRequest getRequestFor(UUID targetId, String requesterName) {
        LinkedHashMap<UUID, TeleportRequest> requests = pendingRequestsByTarget.get(targetId);
        if (requests == null || requests.isEmpty()) {
            return null;
        }

        for (TeleportRequest request : requests.values()) {
            if (request.requesterName().equalsIgnoreCase(requesterName)) {
                return request;
            }
        }
        return null;
    }

    private void removeRequest(TeleportRequest request) {
        LinkedHashMap<UUID, TeleportRequest> requests = pendingRequestsByTarget.get(request.targetId());
        if (requests == null) {
            return;
        }

        requests.remove(request.requesterId());
        if (requests.isEmpty()) {
            pendingRequestsByTarget.remove(request.targetId());
        }
    }

    private void pruneExpiredRequestsFor(UUID targetId) {
        LinkedHashMap<UUID, TeleportRequest> requests = pendingRequestsByTarget.get(targetId);
        if (requests == null || requests.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, TeleportRequest>> iterator = requests.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TeleportRequest> entry = iterator.next();
            TeleportRequest request = entry.getValue();
            if ((now - request.createdAtMillis()) > REQUEST_TIMEOUT_MS) {
                iterator.remove();

                Player requester = Bukkit.getPlayer(request.requesterId());
                if (requester != null && requester.isOnline()) {
                    Messager.sendWarningMessage(requester, "Your teleport request to " + Bukkit.getOfflinePlayer(targetId).getName() + " expired.");
                }
            }
        }

        if (requests.isEmpty()) {
            pendingRequestsByTarget.remove(targetId);
        }
    }

    private record TeleportRequest(UUID requesterId, String requesterName, UUID targetId, long createdAtMillis) {
    }

}
