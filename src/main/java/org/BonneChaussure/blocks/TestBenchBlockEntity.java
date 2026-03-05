package org.BonneChaussure.blocks;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class TestBenchBlockEntity extends BlockEntity {

    public static final BlockEntityType<TestBenchBlockEntity> TYPE = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of("teststone", "test_bench"),
            BlockEntityType.Builder.create(TestBenchBlockEntity::new, ModBlocks.TEST_BENCH).build()
    );

    public TestBenchBlockEntity(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }
}
