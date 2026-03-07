package org.BonneChaussure.gui;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.BonneChaussure.tests.TestCase;

import java.util.ArrayList;
import java.util.List;

public class TestCaseScreenHandler extends ScreenHandler {

    public record SyncData(BlockPos bench, List<BlockPos> injectors, List<BlockPos> sensors, List<TestCase> cases) {
        public static final PacketCodec<RegistryByteBuf, SyncData> CODEC = PacketCodec.of(
                (data, buf) -> {
                    buf.writeBlockPos(data.bench());
                    buf.writeInt(data.injectors().size());
                    data.injectors().forEach(buf::writeBlockPos);
                    buf.writeInt(data.sensors().size());
                    data.sensors().forEach(buf::writeBlockPos);
                    buf.writeInt(data.cases().size());
                    data.cases().forEach(tc -> buf.writeNbt(tc.toNbt()));
                },
                buf -> {
                    BlockPos bench = buf.readBlockPos();
                    int injCount = buf.readInt();
                    List<BlockPos> inj = new ArrayList<>();
                    for (int i = 0; i < injCount; i++) inj.add(buf.readBlockPos());
                    int senCount = buf.readInt();
                    List<BlockPos> sen = new ArrayList<>();
                    for (int i = 0; i < senCount; i++) sen.add(buf.readBlockPos());
                    int caseCount = buf.readInt();
                    List<TestCase> cases = new ArrayList<>();
                    for (int i = 0; i < caseCount; i++) cases.add(TestCase.fromNbt((NbtCompound) buf.readNbt()));
                    return new SyncData(bench, inj, sen, cases);
                }
        );
    }

    public static final ScreenHandlerType<TestCaseScreenHandler> TYPE = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of("teststone", "test_case"),
            new ExtendedScreenHandlerType<>(TestCaseScreenHandler::new, SyncData.CODEC)
    );

    public final BlockPos bench;
    public final List<BlockPos> injectors;
    public final List<BlockPos> sensors;
    public final List<TestCase> cases;

    public TestCaseScreenHandler(int syncId, PlayerInventory inv, SyncData data) {
        super(TYPE, syncId);
        this.bench     = data.bench();
        this.injectors = data.injectors();
        this.sensors   = data.sensors();
        this.cases     = new ArrayList<>(data.cases());
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return null;
    }

    @Override
    public boolean canUse(PlayerEntity player) { return true; }
}
