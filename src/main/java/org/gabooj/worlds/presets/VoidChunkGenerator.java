package org.gabooj.worlds.presets;

import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class VoidChunkGenerator extends ChunkGenerator {

    private final boolean genStructures;

    public VoidChunkGenerator(boolean genStructures) {
        this.genStructures = genStructures;
    }

    @Override
    public void generateNoise(
            @NotNull WorldInfo worldInfo,
            @NotNull Random random,
            int chunkX,
            int chunkZ,
            @NotNull ChunkData chunkData
    ) {
        // Intentionally empty
    }

    @Override
    public boolean shouldGenerateNoise() {
        return genStructures;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return genStructures;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return genStructures;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return genStructures;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return true;
    }

    @Override
    public boolean shouldGenerateStructures() {
        return genStructures;
    }

}
