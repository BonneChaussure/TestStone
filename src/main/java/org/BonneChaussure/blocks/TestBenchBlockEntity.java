package org.BonneChaussure.blocks;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class TestBenchBlockEntity extends BlockEntity {

    public static final BlockEntityType<TestBenchBlockEntity> TYPE = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of("teststone", "test_bench"),
            BlockEntityType.Builder.create(TestBenchBlockEntity::new, ModBlocks.TEST_BENCH).build()
    );

    // Rayon de la BoundaryBox autour du TestBench
    public static final int RADIUS = 5;

    private int color = 0xFF0000; // rouge par défaut

    // Coins stockés en NBT — null tant que le bloc n'est pas posé
    private BlockPos corner1 = null;
    private BlockPos corner2 = null;

    public TestBenchBlockEntity(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }

    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; markDirty(); }

    // Calcule et stocke les coins au moment de la pose
    public void initBoundaryBox() {
        corner1 = pos.add(1,1,1);
        corner2 = pos.add( RADIUS,  RADIUS,  RADIUS);
        markDirty(); // signale à Minecraft que le NBT doit être sauvegardé
    }

    public BlockPos getCorner1() { return corner1; }
    public BlockPos getCorner2() { return corner2; }
    public boolean hasBoundaryBox() { return corner1 != null && corner2 != null; }

    // ── Sérialisation NBT ──────────────────────────────────────────────────────

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        if (corner1 != null) {
            nbt.putLong("corner1", corner1.asLong());
            nbt.putLong("corner2", corner2.asLong());
        }

        nbt.putInt("color", color);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        if (nbt.contains("corner1")) {
            corner1 = BlockPos.fromLong(nbt.getLong("corner1"));
            corner2 = BlockPos.fromLong(nbt.getLong("corner2"));
        }

        if (nbt.contains("color")) color = nbt.getInt("color");
    }
}
