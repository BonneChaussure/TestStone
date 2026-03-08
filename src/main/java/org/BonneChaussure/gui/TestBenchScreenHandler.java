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

public class TestBenchScreenHandler extends ScreenHandler {

    // Record qui transite du serveur → client à l'ouverture
    public record SyncData(BlockPos pos, int sizeX, int sizeY, int sizeZ, int color,
                           List<BlockPos> injectors, Map<BlockPos, String> injectorNames,
                           List<BlockPos> sensors,   Map<BlockPos, String> sensorNames,
                           List<TestCase> testCases, int selectedCaseIndex) {

        public static final PacketCodec<RegistryByteBuf, SyncData> CODEC = PacketCodec.of(
                (data, buf) -> {
                    buf.writeBlockPos(data.pos());
                    buf.writeInt(data.sizeX()); buf.writeInt(data.sizeY()); buf.writeInt(data.sizeZ());
                    buf.writeInt(data.color());
                    buf.writeInt(data.injectors().size());
                    data.injectors().forEach(buf::writeBlockPos);
                    buf.writeInt(data.injectorNames().size());
                    data.injectorNames().forEach((p, n) -> { buf.writeBlockPos(p); buf.writeString(n); });
                    buf.writeInt(data.sensors().size());
                    data.sensors().forEach(buf::writeBlockPos);
                    buf.writeInt(data.sensorNames().size());
                    data.sensorNames().forEach((p, n) -> { buf.writeBlockPos(p); buf.writeString(n); });
                    buf.writeInt(data.testCases().size());
                    data.testCases().forEach(tc -> buf.writeNbt(tc.toNbt()));
                    buf.writeInt(data.selectedCaseIndex());
                },
                buf -> {
                    BlockPos pos = buf.readBlockPos();
                    int sx = buf.readInt(), sy = buf.readInt(), sz = buf.readInt();
                    int color = buf.readInt();
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
                    return new SyncData(pos, sx, sy, sz, color, inj, injNames, sen, senNames, cases, selectedCaseIndex);
                }
        );
    }

    public static final ScreenHandlerType<TestBenchScreenHandler> TYPE = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of("teststone", "test_bench"),
            new ExtendedScreenHandlerType<>(TestBenchScreenHandler::new, SyncData.CODEC)
    );

    public final BlockPos benchPos;
    public final int sizeX, sizeY, sizeZ, color;
    public final int selectedCaseIndex;
    public List<TestCase> testCases;
    public List<BlockPos> injectors;
    public Map<BlockPos, String> injectorNames;
    public List<BlockPos> sensors;
    public Map<BlockPos, String> sensorNames;

    // Constructeur serveur
    public TestBenchScreenHandler(int syncId, PlayerInventory inv, BlockPos pos,
                                  int sizeX, int sizeY, int sizeZ, int color) {
        super(TYPE, syncId);
        this.benchPos      = pos;
        this.sizeX = sizeX; this.sizeY = sizeY; this.sizeZ = sizeZ;
        this.color         = color;
        this.selectedCaseIndex = 0;
        this.injectorNames = new LinkedHashMap<>();
        this.sensorNames   = new LinkedHashMap<>();
    }

    // Constructeur client — reçoit les vraies données du BE via SyncData
    public TestBenchScreenHandler(int syncId, PlayerInventory inv, SyncData data) {
        super(TYPE, syncId);
        this.benchPos          = data.pos();
        this.sizeX             = data.sizeX();
        this.sizeY             = data.sizeY();
        this.sizeZ             = data.sizeZ();
        this.color             = data.color();
        this.injectors         = data.injectors();
        this.injectorNames     = data.injectorNames();
        this.sensors           = data.sensors();
        this.sensorNames       = data.sensorNames();
        this.testCases         = data.testCases();
        this.selectedCaseIndex = data.selectedCaseIndex();
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return null;
    }

    @Override
    public boolean canUse(PlayerEntity player) { return true; }
}
