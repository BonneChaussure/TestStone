package org.BonneChaussure.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.BonneChaussure.blocks.TestBenchBlock;
import org.BonneChaussure.blocks.TestBenchBlockEntity;

public record UpdateBenchPacket(
        BlockPos bench,
        int sizeX, int sizeY, int sizeZ,
        int color,
        int rotation,
        boolean captureEntities,
        int maxTicks,
        int minObserveTicks
) implements CustomPayload {

    public static final CustomPayload.Id<UpdateBenchPacket> ID =
            new CustomPayload.Id<>(Identifier.of("teststone", "update_bench"));

    public static final PacketCodec<PacketByteBuf, UpdateBenchPacket> CODEC = PacketCodec.of(
            (packet, buf) -> {
                buf.writeBlockPos(packet.bench());
                buf.writeInt(packet.sizeX());
                buf.writeInt(packet.sizeY());
                buf.writeInt(packet.sizeZ());
                buf.writeInt(packet.color());
                buf.writeInt(packet.rotation());
                buf.writeBoolean(packet.captureEntities());
                buf.writeInt(packet.maxTicks());
                buf.writeInt(packet.minObserveTicks());
            },
            buf -> new UpdateBenchPacket(
                    buf.readBlockPos(),
                    buf.readInt(), buf.readInt(), buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readBoolean(),
                    buf.readInt(),
                    buf.readInt()
            )
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(ID, CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerWorld world = context.player().getServerWorld();
                if (world.getBlockEntity(payload.bench()) instanceof TestBenchBlockEntity be) {
                    be.setBoundaryBoxSize(payload.sizeX(), payload.sizeY(), payload.sizeZ());
                    be.setColor(payload.color());
                    be.setRotation(payload.rotation());
                    be.initBoundaryBox();
                    be.setCaptureEntities(payload.captureEntities());
                    be.setMaxTicks(payload.maxTicks());
                    be.setMinObserveTicks(payload.minObserveTicks());
                    TestBenchBlock.sendBoxToAll(world, be);
                }
            });
        });
    }
}