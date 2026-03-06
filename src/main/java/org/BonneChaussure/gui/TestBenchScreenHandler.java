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

public class TestBenchScreenHandler extends ScreenHandler {

    // Record qui transite du serveur → client à l'ouverture
    public record SyncData(BlockPos pos, int sizeX, int sizeY, int sizeZ, int color) {
        public static final PacketCodec<RegistryByteBuf, SyncData> CODEC = PacketCodec.of(
                (data, buf) -> {
                    buf.writeBlockPos(data.pos());
                    buf.writeInt(data.sizeX());
                    buf.writeInt(data.sizeY());
                    buf.writeInt(data.sizeZ());
                    buf.writeInt(data.color());
                },
                buf -> new SyncData(buf.readBlockPos(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt())
        );
    }

    public static final ScreenHandlerType<TestBenchScreenHandler> TYPE = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of("teststone", "test_bench"),
            new ExtendedScreenHandlerType<>(TestBenchScreenHandler::new, SyncData.CODEC)
    );

    public final BlockPos benchPos;
    public final int sizeX, sizeY, sizeZ, color;

    // Constructeur serveur
    public TestBenchScreenHandler(int syncId, PlayerInventory inv, BlockPos pos,
                                  int sizeX, int sizeY, int sizeZ, int color) {
        super(TYPE, syncId);
        this.benchPos = pos;
        this.sizeX = sizeX; this.sizeY = sizeY; this.sizeZ = sizeZ;
        this.color = color;
    }

    // Constructeur client — reçoit les vraies données du BE via SyncData
    public TestBenchScreenHandler(int syncId, PlayerInventory inv, SyncData data) {
        super(TYPE, syncId);
        this.benchPos = data.pos();
        this.sizeX = data.sizeX(); this.sizeY = data.sizeY(); this.sizeZ = data.sizeZ();
        this.color = data.color();
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return null;
    }

    @Override
    public boolean canUse(PlayerEntity player) { return true; }
}
