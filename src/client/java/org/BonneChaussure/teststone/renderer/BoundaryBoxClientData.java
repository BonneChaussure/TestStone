package org.BonneChaussure.teststone.renderer;

import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BoundaryBoxClientData {

    public record BoxData(BlockPos corner1, BlockPos corner2, int color) {}

    public static final Map<BlockPos, BoxData> boxes = new ConcurrentHashMap<>();
}
