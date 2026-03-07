package org.BonneChaussure.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.BonneChaussure.blocks.TestBenchBlockEntity;
import org.BonneChaussure.tests.TestCase;

import java.util.ArrayList;
import java.util.List;

public record SaveTestCasesPacket(
        BlockPos bench,
        List<TestCase> cases,
        List<BlockPos> injectorOrder,
        List<BlockPos> sensorOrder
) implements CustomPayload {

    public static final CustomPayload.Id<SaveTestCasesPacket> ID =
            new CustomPayload.Id<>(Identifier.of("teststone", "save_test_cases"));

    public static final PacketCodec<PacketByteBuf, SaveTestCasesPacket> CODEC = PacketCodec.of(
            (packet, buf) -> {
                buf.writeBlockPos(packet.bench());

                buf.writeInt(packet.cases().size());
                packet.cases().forEach(tc -> buf.writeNbt(tc.toNbt()));

                buf.writeInt(packet.injectorOrder().size());
                packet.injectorOrder().forEach(buf::writeBlockPos);

                buf.writeInt(packet.sensorOrder().size());
                packet.sensorOrder().forEach(buf::writeBlockPos);
            },
            buf -> {
                BlockPos bench = buf.readBlockPos();

                int caseCount = buf.readInt();
                List<TestCase> cases = new ArrayList<>();
                for (int i = 0; i < caseCount; i++)
                    cases.add(TestCase.fromNbt((NbtCompound) buf.readNbt()));

                int injCount = buf.readInt();
                List<BlockPos> injOrder = new ArrayList<>();
                for (int i = 0; i < injCount; i++) injOrder.add(buf.readBlockPos());

                int senCount = buf.readInt();
                List<BlockPos> senOrder = new ArrayList<>();
                for (int i = 0; i < senCount; i++) senOrder.add(buf.readBlockPos());

                return new SaveTestCasesPacket(bench, cases, injOrder, senOrder);
            }
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(ID, CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerWorld world = context.player().getServerWorld();
                if (world.getBlockEntity(payload.bench()) instanceof TestBenchBlockEntity be) {
                    be.setTestCases(payload.cases());
                    be.setScannedBlockOrder(payload.injectorOrder(), payload.sensorOrder());
                }
            });
        });
    }
}
