package org.BonneChaussure.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record RemoveBoundaryBoxPacket(BlockPos bench) implements CustomPayload {

    public static final CustomPayload.Id<RemoveBoundaryBoxPacket> ID =
            new CustomPayload.Id<>(Identifier.of("teststone", "remove_boundary_box"));

    public static final PacketCodec<PacketByteBuf, RemoveBoundaryBoxPacket> CODEC = PacketCodec.of(
            (packet, buf) -> buf.writeBlockPos(packet.bench()),
            buf -> new RemoveBoundaryBoxPacket(buf.readBlockPos())
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
    }
}
