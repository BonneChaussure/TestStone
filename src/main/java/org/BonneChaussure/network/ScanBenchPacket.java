package org.BonneChaussure.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.BonneChaussure.blocks.ModBlocks;
import org.BonneChaussure.blocks.TestBenchBlockEntity;

import java.util.ArrayList;
import java.util.List;

public record ScanBenchPacket(BlockPos bench) implements CustomPayload {

    public static final CustomPayload.Id<ScanBenchPacket> ID =
            new CustomPayload.Id<>(Identifier.of("teststone", "scan_bench"));

    public static final PacketCodec<PacketByteBuf, ScanBenchPacket> CODEC = PacketCodec.of(
            (packet, buf) -> buf.writeBlockPos(packet.bench()),
            buf -> new ScanBenchPacket(buf.readBlockPos())
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(ID, CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerWorld world = context.player().getServerWorld();
                if (!(world.getBlockEntity(payload.bench()) instanceof TestBenchBlockEntity be)) return;
                if (!be.hasBoundaryBox()) return;

                List<BlockPos> injectors = new ArrayList<>();
                List<BlockPos> sensors   = new ArrayList<>();

                // Parcourt tous les blocs dans la BoundaryBox
                BlockPos c1 = be.getCorner1();
                BlockPos c2 = be.getCorner2();
                int minX = Math.min(c1.getX(), c2.getX()), maxX = Math.max(c1.getX(), c2.getX());
                int minY = Math.min(c1.getY(), c2.getY()), maxY = Math.max(c1.getY(), c2.getY());
                int minZ = Math.min(c1.getZ(), c2.getZ()), maxZ = Math.max(c1.getZ(), c2.getZ());

                for (int x = minX; x <= maxX; x++)
                    for (int y = minY; y <= maxY; y++)
                        for (int z = minZ; z <= maxZ; z++) {
                            BlockPos p = new BlockPos(x, y, z);
                            if (world.getBlockState(p).isOf(ModBlocks.INJECTOR)) injectors.add(p);
                            else if (world.getBlockState(p).isOf(ModBlocks.SENSOR))   sensors.add(p);
                        }

                be.setScannedBlocks(injectors, sensors);

                // Renvoie les résultats au client
                ServerPlayNetworking.send(context.player(),
                        new SyncScannedBlocksPacket(payload.bench(), injectors, sensors));
            });
        });
    }
}
