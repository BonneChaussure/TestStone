package org.BonneChaussure.blocks;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.BonneChaussure.gui.TestBenchScreenHandler;

public class TestBenchBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory<TestBenchScreenHandler.SyncData> {

    private int color = 0xFF0000; // rouge par défaut

    public static final int DEFAULT_SIZE = 5;

    private int sizeX = DEFAULT_SIZE;
    private int sizeY = DEFAULT_SIZE;
    private int sizeZ = DEFAULT_SIZE;

    // Coins stockés en NBT — null tant que le bloc n'est pas posé
    private BlockPos corner1 = null;
    private BlockPos corner2 = null;

    public static final BlockEntityType<TestBenchBlockEntity> TYPE = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of("teststone", "test_bench"),
            BlockEntityType.Builder.create(TestBenchBlockEntity::new, ModBlocks.TEST_BENCH).build()
    );

    public TestBenchBlockEntity(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }

    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; markDirty(); }

    // Calcule et stocke les coins au moment de la pose
    public void initBoundaryBox() {
        corner1 = pos.add(1, 1, 1);
        corner2 = pos.add( sizeX,  sizeY,  sizeZ);
        markDirty();
    }

    public void setBoundaryBoxSize(int x, int y, int z) {
        this.sizeX = x;
        this.sizeY = y;
        this.sizeZ = z;
        initBoundaryBox();
    }

    public BlockPos getCorner1() { return corner1; }
    public BlockPos getCorner2() { return corner2; }
    public boolean hasBoundaryBox() { return corner1 != null && corner2 != null; }

    public int getSizeX() { return sizeX; }
    public int getSizeY() { return sizeY; }
    public int getSizeZ() { return sizeZ; }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.teststone.test_bench");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new TestBenchScreenHandler(syncId, inv, pos, sizeX, sizeY, sizeZ, color);
    }

    @Override
    public TestBenchScreenHandler.SyncData getScreenOpeningData(ServerPlayerEntity player) {
        return new TestBenchScreenHandler.SyncData(pos, sizeX, sizeY, sizeZ, color);
    }

    // ── Sérialisation NBT ──────────────────────────────────────────────────────

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);

        nbt.putInt("sizeX", sizeX);
        nbt.putInt("sizeY", sizeY);
        nbt.putInt("sizeZ", sizeZ);

        if (corner1 != null) {
            nbt.putLong("corner1", corner1.asLong());
            nbt.putLong("corner2", corner2.asLong());
        }

        nbt.putInt("color", color);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);

        if (nbt.contains("sizeX")) sizeX = nbt.getInt("sizeX");
        if (nbt.contains("sizeY")) sizeY = nbt.getInt("sizeY");
        if (nbt.contains("sizeZ")) sizeZ = nbt.getInt("sizeZ");

        if (nbt.contains("corner1")) {
            corner1 = BlockPos.fromLong(nbt.getLong("corner1"));
            corner2 = BlockPos.fromLong(nbt.getLong("corner2"));
        }

        if (nbt.contains("color")) color = nbt.getInt("color");
    }
}
