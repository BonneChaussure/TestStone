package org.BonneChaussure.teststone.renderer;

import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BoundaryBoxClientData {
    public static final Map<BlockPos, BlockPos[]> boxes = new ConcurrentHashMap<>();
}
