package org.BonneChaussure.blocks;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.BonneChaussure.gui.RenameBlockScreenHandler;

public class SensorBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory<RenameBlockScreenHandler.SyncData> {

    public static final BlockEntityType<SensorBlockEntity> TYPE = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of("teststone", "sensor"),
            BlockEntityType.Builder.create(SensorBlockEntity::new, ModBlocks.SENSOR).build()
    );

    private String customName = "";

    public SensorBlockEntity(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }

    public String getCustomName() { return customName; }
    public void setCustomName(String name) {
        this.customName = name;
        markDirty();
        // Notifie le client de la mise à jour
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        nbt.putString("customName", customName);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        customName = nbt.getString("customName");
    }

    @Override
    public Text getDisplayName() {
        return Text.literal("Sensor");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new RenameBlockScreenHandler(syncId, inv,
                new RenameBlockScreenHandler.SyncData(pos, customName, false));
    }

    @Override
    public RenameBlockScreenHandler.SyncData getScreenOpeningData(ServerPlayerEntity player) {
        return new RenameBlockScreenHandler.SyncData(pos, customName, false);
    }

    @Override
    public BlockEntityUpdateS2CPacket toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        return createNbt(registries);
    }
}
