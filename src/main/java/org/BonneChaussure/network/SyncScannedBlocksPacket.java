package org.BonneChaussure.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record SyncScannedBlocksPacket(
        BlockPos bench,
        List<BlockPos> injectors,
        Map<BlockPos, String> injectorNames,
        List<BlockPos> sensors,
        Map<BlockPos, String> sensorNames
) implements CustomPayload {

    public static final CustomPayload.Id<SyncScannedBlocksPacket> ID =
            new CustomPayload.Id<>(Identifier.of("teststone", "sync_scanned_blocks"));

    public static final PacketCodec<RegistryByteBuf, SyncScannedBlocksPacket> CODEC = PacketCodec.of(
            (packet, buf) -> {
                buf.writeBlockPos(packet.bench());

                // Injectors (positions)
                buf.writeInt(packet.injectors().size());
                packet.injectors().forEach(buf::writeBlockPos);

                // Injector names
                buf.writeInt(packet.injectorNames().size());
                packet.injectorNames().forEach((pos, name) -> {
                    buf.writeBlockPos(pos);
                    buf.writeString(name);
                });

                // Sensors (positions)
                buf.writeInt(packet.sensors().size());
                packet.sensors().forEach(buf::writeBlockPos);

                // Sensor names
                buf.writeInt(packet.sensorNames().size());
                packet.sensorNames().forEach((pos, name) -> {
                    buf.writeBlockPos(pos);
                    buf.writeString(name);
                });
            },
            buf -> {
                BlockPos bench = buf.readBlockPos();

                int injCount = buf.readInt();
                List<BlockPos> inj = new ArrayList<>();
                for (int i = 0; i < injCount; i++) inj.add(buf.readBlockPos());

                int injNameCount = buf.readInt();
                Map<BlockPos, String> injNames = new LinkedHashMap<>();
                for (int i = 0; i < injNameCount; i++)
                    injNames.put(buf.readBlockPos(), buf.readString());

                int senCount = buf.readInt();
                List<BlockPos> sen = new ArrayList<>();
                for (int i = 0; i < senCount; i++) sen.add(buf.readBlockPos());

                int senNameCount = buf.readInt();
                Map<BlockPos, String> senNames = new LinkedHashMap<>();
                for (int i = 0; i < senNameCount; i++)
                    senNames.put(buf.readBlockPos(), buf.readString());

                return new SyncScannedBlocksPacket(bench, inj, injNames, sen, senNames);
            }
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
    }
}
