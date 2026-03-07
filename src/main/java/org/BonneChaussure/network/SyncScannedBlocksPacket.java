package org.BonneChaussure.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public record SyncScannedBlocksPacket(BlockPos bench, List<BlockPos> injectors, List<BlockPos> sensors)
        implements CustomPayload {

    public static final CustomPayload.Id<SyncScannedBlocksPacket> ID =
            new CustomPayload.Id<>(Identifier.of("teststone", "sync_scanned_blocks"));

    public static final PacketCodec<RegistryByteBuf, SyncScannedBlocksPacket> CODEC = PacketCodec.of(
            (packet, buf) -> {
                buf.writeBlockPos(packet.bench());
                buf.writeInt(packet.injectors().size());
                packet.injectors().forEach(buf::writeBlockPos);
                buf.writeInt(packet.sensors().size());
                packet.sensors().forEach(buf::writeBlockPos);
            },
            buf -> {
                BlockPos bench = buf.readBlockPos();
                int injCount = buf.readInt();
                List<BlockPos> inj = new ArrayList<>();
                for (int i = 0; i < injCount; i++) inj.add(buf.readBlockPos());
                int senCount = buf.readInt();
                List<BlockPos> sen = new ArrayList<>();
                for (int i = 0; i < senCount; i++) sen.add(buf.readBlockPos());
                return new SyncScannedBlocksPacket(bench, inj, sen);
            }
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
    }
}
