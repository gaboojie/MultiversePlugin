package org.gabooj.players;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.gabooj.scope.ScopeMeta;
import org.gabooj.worlds.WorldManager;
import org.gabooj.worlds.WorldMeta;

public class PlayerTabManager {

    private static Scoreboard scoreboard;

    public static void initScoreboard() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        scoreboard = manager.getNewScoreboard();
    }

    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    private static NamedTextColor getColorByWorld(World.Environment environment) {
        switch (environment) {
            case NORMAL, CUSTOM -> {
                return NamedTextColor.GRAY;
            }
            case NETHER -> {
                return NamedTextColor.RED;
            }
            case THE_END -> {
                return NamedTextColor.BLACK;
            }
        }
        return NamedTextColor.GRAY;
    }

    public static void updatePlayer(Player player, WorldManager worldManager) {
        ScopeMeta scopeMeta = worldManager.scopeManager.getCurrentPlayerScope(player);
        World world = player.getWorld();
        WorldMeta worldMeta = worldManager.getWorldMetaByID(world.getName());
        if (worldMeta == null) return;
        updatePlayer(player, scopeMeta, worldMeta, worldManager);
    }

    public static void updatePlayer(Player player, ScopeMeta scopeMeta, WorldMeta targetWorldMeta, WorldManager worldManager) {
        World.Environment environment = targetWorldMeta.getEnvironment();
        Team team = ensureTeam(scopeMeta, environment);

        removePlayerFromAllTeams(player);
        team.addEntry(player.getName());

        // Assign scoreboard (safe to call repeatedly)
        player.setScoreboard(scoreboard);
    }

    public static void removePlayer(Player player) {
        removePlayerFromAllTeams(player);
    }

    public static void restoreMainScoreboard() {
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(main);
        }
    }

    private static Team ensureTeam(ScopeMeta scopeMeta, World.Environment environment) {
        String teamId = teamId(scopeMeta.getScopeId(), environment);

        Team team = scoreboard.getTeam(teamId);
        if (team != null) return team;
        team = scoreboard.registerNewTeam(teamId);

        NamedTextColor color = getColorByWorld(environment);

        team.prefix(Component.text(
                "[" + scopeMeta.getScopeId().toUpperCase() + "] ",
                color
        ));
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.ALWAYS);

        return team;
    }

    private static void removePlayerFromAllTeams(Player player) {
        String entry = player.getName();
        for (Team team : scoreboard.getTeams()) {
            if (team.hasEntry(entry)) {
                team.removeEntry(entry);
            }
        }
    }

    private static String teamId(String scopeId, World.Environment environment) {
        String envShort = switch (environment) {
            case NORMAL -> "ow";
            case NETHER -> "n";
            case THE_END -> "e";
            case CUSTOM -> "c";
        };

        // Example: sc_smp_ow
        return ("sc_" + scopeId + "_" + envShort)
                .toLowerCase()
                .substring(0, Math.min(16, ("sc_" + scopeId + "_" + envShort).length()));
    }


}
