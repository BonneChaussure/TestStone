package org.BonneChaussure.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record SetBoundaryBoxPacket(BlockPos bench, BlockPos corner1, BlockPos corner2, int color) implements CustomPayload {

    public static final CustomPayload.Id<SetBoundaryBoxPacket> ID =
            new CustomPayload.Id<>(Identifier.of("teststone", "set_boundary_box"));

    public static final PacketCodec<PacketByteBuf, SetBoundaryBoxPacket> CODEC = PacketCodec.of(
            (packet, buf) -> {
                buf.writeBlockPos(packet.bench());
                buf.writeBlockPos(packet.corner1());
                buf.writeBlockPos(packet.corner2());
                buf.writeInt(packet.color());
            },
            buf -> new SetBoundaryBoxPacket(buf.readBlockPos(), buf.readBlockPos(), buf.readBlockPos(), buf.readInt())
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
    }
}
