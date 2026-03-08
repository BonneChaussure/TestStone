package org.BonneChaussure.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.BonneChaussure.blocks.TestBenchBlockEntity;

public record RunSingleTestPacket(BlockPos bench, int caseIndex) implements CustomPayload {

    public static final Id<RunSingleTestPacket> ID =
            new Id<>(Identifier.of("teststone", "run_single_test"));

    public static final PacketCodec<PacketByteBuf, RunSingleTestPacket> CODEC = PacketCodec.of(
            (p, buf) -> {
                buf.writeBlockPos(p.bench());
                buf.writeInt(p.caseIndex());
            },
            buf -> new RunSingleTestPacket(buf.readBlockPos(), buf.readInt())
    );

    @Override public Id<? extends CustomPayload> getId() { return ID; }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(ID, CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ID, (payload, ctx) ->
                ctx.server().execute(() -> {
                    ServerWorld world = ctx.player().getServerWorld();
                    if (world.getBlockEntity(payload.bench()) instanceof TestBenchBlockEntity be) {
                        be.startSingleTest(world, payload.caseIndex());
                    }
                })
        );
    }
}
