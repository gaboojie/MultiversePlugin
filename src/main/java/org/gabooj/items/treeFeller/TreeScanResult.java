package org.gabooj.items.treeFeller;

import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class TreeScanResult {

    private final List<Block> logs;
    private final List<Block> leaves;

    public TreeScanResult(Set<Block> logs, Set<Block> leaves) {
        this.logs = logs.stream()
                .sorted(Comparator.comparingInt(b -> -b.getY()))
                .toList();

        this.leaves = leaves.stream()
                .sorted(Comparator.comparingInt(b -> -b.getY()))
                .toList();
    }

    public boolean isValidTree() {
        return !logs.isEmpty() && !leaves.isEmpty();
    }

    public List<Block> allBlocks() {
        List<Block> out = new ArrayList<>(logs);
        out.addAll(leaves);
        return out;
    }
}
