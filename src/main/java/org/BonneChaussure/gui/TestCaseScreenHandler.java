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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TestCaseScreenHandler extends ScreenHandler {

    public record SyncData(BlockPos bench,
                           List<BlockPos> injectors, Map<BlockPos, String> injectorNames,
                           List<BlockPos> sensors,   Map<BlockPos, String> sensorNames,
                           List<TestCase> cases, int selectedCaseIndex) {

        public static final PacketCodec<RegistryByteBuf, SyncData> CODEC = PacketCodec.of(
                (data, buf) -> {
                    buf.writeBlockPos(data.bench());
                    buf.writeInt(data.injectors().size());
                    data.injectors().forEach(buf::writeBlockPos);
                    buf.writeInt(data.injectorNames().size());
                    data.injectorNames().forEach((p, n) -> { buf.writeBlockPos(p); buf.writeString(n); });
                    buf.writeInt(data.sensors().size());
                    data.sensors().forEach(buf::writeBlockPos);
                    buf.writeInt(data.sensorNames().size());
                    data.sensorNames().forEach((p, n) -> { buf.writeBlockPos(p); buf.writeString(n); });
                    buf.writeInt(data.cases().size());
                    data.cases().forEach(tc -> buf.writeNbt(tc.toNbt()));
                    buf.writeInt(data.selectedCaseIndex());
                },
                buf -> {
                    BlockPos bench = buf.readBlockPos();
                    int injCount = buf.readInt();
                    List<BlockPos> inj = new ArrayList<>();
                    for (int i = 0; i < injCount; i++) inj.add(buf.readBlockPos());
                    int injNameCount = buf.readInt();
                    Map<BlockPos, String> injNames = new LinkedHashMap<>();
                    for (int i = 0; i < injNameCount; i++) injNames.put(buf.readBlockPos(), buf.readString());
                    int senCount = buf.readInt();
                    List<BlockPos> sen = new ArrayList<>();
                    for (int i = 0; i < senCount; i++) sen.add(buf.readBlockPos());
                    int senNameCount = buf.readInt();
                    Map<BlockPos, String> senNames = new LinkedHashMap<>();
                    for (int i = 0; i < senNameCount; i++) senNames.put(buf.readBlockPos(), buf.readString());
                    int caseCount = buf.readInt();
                    List<TestCase> cases = new ArrayList<>();
                    for (int i = 0; i < caseCount; i++) cases.add(TestCase.fromNbt((NbtCompound) buf.readNbt()));
                    int selectedCaseIndex = buf.readInt();
                    return new SyncData(bench, inj, injNames, sen, senNames, cases, selectedCaseIndex);
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
    public final Map<BlockPos, String> injectorNames;
    public final List<BlockPos> sensors;
    public final Map<BlockPos, String> sensorNames;
    public final List<TestCase> cases;
    public final int selectedCaseIndex;

    public TestCaseScreenHandler(int syncId, PlayerInventory inv, SyncData data) {
        super(TYPE, syncId);
        this.bench             = data.bench();
        this.injectors         = data.injectors();
        this.injectorNames     = data.injectorNames();
        this.sensors           = data.sensors();
        this.sensorNames       = data.sensorNames();
        this.cases             = new ArrayList<>(data.cases());
        this.selectedCaseIndex = data.selectedCaseIndex();
    }

    @Override public ItemStack quickMove(PlayerEntity player, int slot) { return null; }
    @Override public boolean canUse(PlayerEntity player) { return true; }
}