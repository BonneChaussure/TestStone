package org.BonneChaussure.gui;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class RenameBlockScreenHandler extends ScreenHandler {

    public record SyncData(BlockPos pos, String currentName, boolean isInjector) {
        public static final PacketCodec<RegistryByteBuf, SyncData> CODEC = PacketCodec.of(
                (data, buf) -> {
                    buf.writeBlockPos(data.pos());
                    buf.writeString(data.currentName());
                    buf.writeBoolean(data.isInjector());
                },
                buf -> new SyncData(buf.readBlockPos(), buf.readString(), buf.readBoolean())
        );
    }

    public static final ScreenHandlerType<RenameBlockScreenHandler> TYPE = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of("teststone", "rename_block"),
            new ExtendedScreenHandlerType<>(RenameBlockScreenHandler::new, SyncData.CODEC)
    );

    public final BlockPos blockPos;
    public final String currentName;
    public final boolean isInjector;

    public RenameBlockScreenHandler(int syncId, PlayerInventory inv, SyncData data) {
        super(TYPE, syncId);
        this.blockPos    = data.pos();
        this.currentName = data.currentName();
        this.isInjector  = data.isInjector();
    }

    @Override public ItemStack quickMove(PlayerEntity player, int slot) { return null; }
    @Override public boolean canUse(PlayerEntity player) { return true; }
}
