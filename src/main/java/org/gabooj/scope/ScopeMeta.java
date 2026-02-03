package org.gabooj.scope;

import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.gabooj.worlds.WorldMeta;

import java.util.*;

public class ScopeMeta {

    private final String scopeId;

    private SpawnLocation spawnLocation;
    private GameMode gameMode;
    private boolean doHardcore;
    private Difficulty difficulty;
    private String name;
    private boolean isVisible;

    private final Set<WorldMeta> worlds = new HashSet<>();
    private final Map<World.Environment, WorldMeta> envMap = new EnumMap<>(World.Environment.class);
    public final Set<Warp> warps = new HashSet<>();

    public ScopeMeta(String scopeId) {
        this.scopeId = scopeId;
        this.name = scopeId;
        this.isVisible = true;
    }

    public void updateEnvironmentMap() {
        envMap.clear();
        for (WorldMeta meta : worlds) {
            envMap.put(meta.getEnvironment(), meta);
        }
    }

    public boolean hasEnvironment(World.Environment env) {
        return envMap.containsKey(env);
    }

    public WorldMeta getWorldMetaByEnvironment(World.Environment env) {
        return envMap.get(env) != null ? envMap.get(env) : null;
    }

    public void addWorld(WorldMeta meta) {
        worlds.add(meta);
        updateEnvironmentMap();
    }

    public Set<WorldMeta> getWorlds() {
        return this.worlds;
    }

    public void removeWorld(WorldMeta meta) {
        worlds.remove(meta);
        updateEnvironmentMap();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean visible) {
        isVisible = visible;
    }

    public boolean doHardcore() {
        return doHardcore;
    }

    public void setDoHardcore(boolean doHardcore) {
        this.doHardcore = doHardcore;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public String getScopeId() { return scopeId; }

    public SpawnLocation getSpawnLocation() {
        return spawnLocation;
    }

    public void setSpawnLocation(SpawnLocation spawnLocation) {
        this.spawnLocation = spawnLocation;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public boolean doesWarpExist(String warpName) {
        return getWarpByName(warpName) != null;
    }

    public Set<Warp> getWarps() {
        return this.warps;
    }

    public Warp getWarpByName(String warpName) {
        for (Warp warp : this.warps) {
            if (warp.name.equalsIgnoreCase(warpName)) {
                return warp;
            }
        }
        return null;
    }

}
