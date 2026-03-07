package org.BonneChaussure.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.BonneChaussure.blocks.InjectorBlockEntity;
import org.BonneChaussure.blocks.SensorBlockEntity;

public record RenameBlockPacket(BlockPos pos, String name, boolean isInjector)
        implements CustomPayload {

    public static final Id<RenameBlockPacket> ID =
            new Id<>(Identifier.of("teststone", "rename_block"));

    public static final PacketCodec<PacketByteBuf, RenameBlockPacket> CODEC = PacketCodec.of(
            (p, buf) -> {
                buf.writeBlockPos(p.pos());
                buf.writeString(p.name());
                buf.writeBoolean(p.isInjector());
            },
            buf -> new RenameBlockPacket(buf.readBlockPos(), buf.readString(), buf.readBoolean())
    );

    @Override public Id<? extends CustomPayload> getId() { return ID; }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(ID, CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ID, (payload, ctx) ->
                ctx.server().execute(() -> {
                    ServerWorld world = ctx.player().getServerWorld();
                    if (payload.isInjector()) {
                        if (world.getBlockEntity(payload.pos()) instanceof InjectorBlockEntity be)
                            be.setCustomName(payload.name());
                    } else {
                        if (world.getBlockEntity(payload.pos()) instanceof SensorBlockEntity be)
                            be.setCustomName(payload.name());
                    }
                })
        );
    }
}
