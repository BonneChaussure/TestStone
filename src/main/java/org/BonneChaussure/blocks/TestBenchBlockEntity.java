package org.BonneChaussure.blocks;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.text.Text;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import org.BonneChaussure.gui.TestBenchScreenHandler;
import org.BonneChaussure.tests.TestCase;
import org.BonneChaussure.tests.TestExecutor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TestBenchBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory<TestBenchScreenHandler.SyncData> {

    private int color = 0xFF0000; // rouge par défaut

    public static final int DEFAULT_SIZE = 5;

    private int sizeX = DEFAULT_SIZE;
    private int sizeY = DEFAULT_SIZE;
    private int sizeZ = DEFAULT_SIZE;

    // Coins stockés en NBT — null tant que le bloc n'est pas posé
    private BlockPos corner1 = null;
    private BlockPos corner2 = null;

    private static final boolean CAPTURE_ENTITIES = true;
    private StructureTemplate savedStructure = null;
    private BlockPos structureOrigin = null;

    private List<BlockPos> scannedInjectors = new ArrayList<>();
    private List<BlockPos> scannedSensors   = new ArrayList<>();
    private Map<BlockPos, String> injectorNames = new LinkedHashMap<>();
    private Map<BlockPos, String> sensorNames   = new LinkedHashMap<>();
    private List<TestCase> testCases        = new ArrayList<>();
    private int selectedCaseIndex           = 0;

    public enum TestState { IDLE, RUNNING, DONE }
    private TestState testState = TestState.IDLE;
    private Boolean[] lastResults = null;
    private TestExecutor executor = null;

    public static final BlockEntityType<TestBenchBlockEntity> TYPE = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of("teststone", "test_bench"),
            BlockEntityType.Builder.create(TestBenchBlockEntity::new, ModBlocks.TEST_BENCH).build()
    );

    public TestBenchBlockEntity(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }

    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; markDirty(); }

    // Calcule et stocke les coins au moment de la pose
    public void initBoundaryBox() {
        corner1 = pos.add(1, 1, 1);
        corner2 = pos.add( sizeX,  sizeY,  sizeZ);
        markDirty();
    }

    public void setBoundaryBoxSize(int x, int y, int z) {
        this.sizeX = x;
        this.sizeY = y;
        this.sizeZ = z;
        initBoundaryBox();
    }

    public BlockPos getCorner1() { return corner1; }
    public BlockPos getCorner2() { return corner2; }
    public boolean hasBoundaryBox() { return corner1 != null && corner2 != null; }

    public int getSizeX() { return sizeX; }
    public int getSizeY() { return sizeY; }
    public int getSizeZ() { return sizeZ; }

    public List<BlockPos> getScannedInjectors() { return scannedInjectors; }
    public List<BlockPos> getScannedSensors()   { return scannedSensors; }
    public Map<BlockPos, String> getInjectorNames() { return injectorNames; }
    public Map<BlockPos, String> getSensorNames()   { return sensorNames; }
    public List<TestCase> getTestCases()        { return testCases; }

    public void setScannedBlocks(List<BlockPos> injectors, Map<BlockPos, String> injNames,
                                 List<BlockPos> sensors,   Map<BlockPos, String> senNames) {
        this.scannedInjectors = injectors;
        this.scannedSensors   = sensors;
        this.injectorNames    = new LinkedHashMap<>(injNames);
        this.sensorNames      = new LinkedHashMap<>(senNames);
        markDirty();
    }

    public void setTestCases(List<TestCase> cases) {
        this.testCases = cases;
        markDirty();
    }

    public int getSelectedCaseIndex() { return selectedCaseIndex; }
    public void setSelectedCaseIndex(int idx) {
        this.selectedCaseIndex = Math.max(0, idx);
        markDirty();
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.teststone.test_bench");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new TestBenchScreenHandler(syncId, inv, pos, sizeX, sizeY, sizeZ, color);
    }

    @Override
    public TestBenchScreenHandler.SyncData getScreenOpeningData(ServerPlayerEntity player) {
        return new TestBenchScreenHandler.SyncData(
                pos, sizeX, sizeY, sizeZ, color,
                scannedInjectors, injectorNames,
                scannedSensors, sensorNames,
                testCases, selectedCaseIndex
        );
    }

    public void setScannedBlockOrder(List<BlockPos> injectorOrder, List<BlockPos> sensorOrder) {
        // Réordonne en respectant l'ordre reçu, ignore les pos inconnues
        List<BlockPos> newInj = new ArrayList<>();
        for (BlockPos p : injectorOrder)
            if (scannedInjectors.contains(p)) newInj.add(p);
        // Ajoute les éventuels oubliés (sécurité)
        for (BlockPos p : scannedInjectors)
            if (!newInj.contains(p)) newInj.add(p);
        scannedInjectors = newInj;

        List<BlockPos> newSen = new ArrayList<>();
        for (BlockPos p : sensorOrder)
            if (scannedSensors.contains(p)) newSen.add(p);
        for (BlockPos p : scannedSensors)
            if (!newSen.contains(p)) newSen.add(p);
        scannedSensors = newSen;

        markDirty();
    }

    // ── Méthode tick — appelée par le Block ticker ────────────────────────────
    public void tick(ServerWorld serverWorld) {
        if (executor == null) return;
        boolean ongoing = executor.tick();
        if (!ongoing) {
            executor = null;
        }
    }

    // ── Lance l'exécution ─────────────────────────────────────────────────────
    public void startTests(ServerWorld serverWorld) {
        if (testCases.isEmpty()) return;
        captureStructure(serverWorld);
        testState = TestState.RUNNING;
        executor = new TestExecutor(this, serverWorld, testCases);
        markDirty();
    }

    // ── Lance un seul cas ─────────────────────────────────────────────────────
    public void startSingleTest(ServerWorld serverWorld, int caseIndex) {
        if (testCases.isEmpty() || caseIndex < 0 || caseIndex >= testCases.size()) return;
        captureStructure(serverWorld);
        testState = TestState.RUNNING;
        executor = new TestExecutor(this, serverWorld,
                List.of(testCases.get(caseIndex)), caseIndex);
        markDirty();
    }

    // ── Callback appelé par TestExecutor après chaque cas ─────────────────────
    public void onCaseResult(int caseIdx, boolean pass) {
        TestBenchBlock.sendCaseResult((ServerWorld) world, this, caseIdx, pass);
    }

    // ── Callback appelé par TestExecutor en fin de session ────────────────────
    public void onTestsDone(Boolean[] results) {
        this.lastResults = results;
        this.testState = TestState.DONE;
        this.executor = null;
        this.savedStructure = null;
        this.structureOrigin = null;
        markDirty();
        TestBenchBlock.sendAllResults((ServerWorld) world, this, results);
    }

    public TestState getTestState()   { return testState; }
    public Boolean[] getLastResults() { return lastResults; }

    public void captureStructure(ServerWorld serverWorld) {
        if (!hasBoundaryBox()) return;

        int minX = Math.min(corner1.getX(), corner2.getX());
        int minY = Math.min(corner1.getY(), corner2.getY());
        int minZ = Math.min(corner1.getZ(), corner2.getZ());
        int maxX = Math.max(corner1.getX(), corner2.getX());
        int maxY = Math.max(corner1.getY(), corner2.getY());
        int maxZ = Math.max(corner1.getZ(), corner2.getZ());

        structureOrigin = new BlockPos(minX, minY, minZ);
        Vec3i size = new Vec3i(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1);

        savedStructure = new StructureTemplate();
        // null = aucun bloc à ignorer ; CAPTURE_ENTITIES contrôle la capture des entités
        savedStructure.saveFromWorld(serverWorld, structureOrigin, size, CAPTURE_ENTITIES, null);
    }

    public boolean restoreStructure(ServerWorld serverWorld) {
        if (savedStructure == null || structureOrigin == null) return false;

        int minX = Math.min(corner1.getX(), corner2.getX());
        int minY = Math.min(corner1.getY(), corner2.getY());
        int minZ = Math.min(corner1.getZ(), corner2.getZ());
        int maxX = Math.max(corner1.getX(), corner2.getX());
        int maxY = Math.max(corner1.getY(), corner2.getY());
        int maxZ = Math.max(corner1.getZ(), corner2.getZ());

        net.minecraft.util.math.BlockBox region = new net.minecraft.util.math.BlockBox(
                minX, minY, minZ, maxX, maxY, maxZ);

        // ── 1. Discard des entités ────────────────────────────────────────────
        if (CAPTURE_ENTITIES) {
            net.minecraft.util.math.Box box = new net.minecraft.util.math.Box(
                    minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
            serverWorld.getEntitiesByClass(
                    net.minecraft.entity.Entity.class, box,
                    e -> !(e instanceof net.minecraft.entity.player.PlayerEntity)
            ).forEach(net.minecraft.entity.Entity::discard);
            serverWorld.getEntitiesByClass(
                    net.minecraft.entity.ItemEntity.class, box, e -> true
            ).forEach(net.minecraft.entity.Entity::discard);
        }

        // ── 2. Restaure les blocs ─────────────────────────────────────────────
        StructurePlacementData placement = new StructurePlacementData()
                .setMirror(BlockMirror.NONE)
                .setRotation(BlockRotation.NONE)
                .setIgnoreEntities(!CAPTURE_ENTITIES);

        savedStructure.place(serverWorld, structureOrigin, structureOrigin, placement,
                serverWorld.getRandom(), 2);

        // ── 3. Vide les ticks schedulés ─────────────────────
        serverWorld.getBlockTickScheduler().clearNextTicks(region);
        serverWorld.getFluidTickScheduler().clearNextTicks(region);

        return true;
    }

    // ── Sérialisation NBT ──────────────────────────────────────────────────────

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);

        nbt.putInt("sizeX", sizeX);
        nbt.putInt("sizeY", sizeY);
        nbt.putInt("sizeZ", sizeZ);

        if (corner1 != null) {
            nbt.putLong("corner1", corner1.asLong());
            nbt.putLong("corner2", corner2.asLong());
        }

        nbt.putInt("color", color);

        NbtList injList = new NbtList();
        scannedInjectors.forEach(p -> { NbtCompound e = new NbtCompound(); e.putLong("p", p.asLong()); injList.add(e); });
        nbt.put("scannedInjectors", injList);

        NbtList injNameList = new NbtList();
        injectorNames.forEach((p, name) -> { NbtCompound e = new NbtCompound(); e.putLong("p", p.asLong()); e.putString("n", name); injNameList.add(e); });
        nbt.put("injectorNames", injNameList);

        NbtList senList = new NbtList();
        scannedSensors.forEach(p -> { NbtCompound e = new NbtCompound(); e.putLong("p", p.asLong()); senList.add(e); });
        nbt.put("scannedSensors", senList);

        NbtList senNameList = new NbtList();
        sensorNames.forEach((p, name) -> { NbtCompound e = new NbtCompound(); e.putLong("p", p.asLong()); e.putString("n", name); senNameList.add(e); });
        nbt.put("sensorNames", senNameList);

        NbtList caseList = new NbtList();
        testCases.forEach(tc -> caseList.add(tc.toNbt()));
        nbt.put("testCases", caseList);

        nbt.putInt("selectedCaseIndex", selectedCaseIndex);

    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);

        if (nbt.contains("sizeX")) sizeX = nbt.getInt("sizeX");
        if (nbt.contains("sizeY")) sizeY = nbt.getInt("sizeY");
        if (nbt.contains("sizeZ")) sizeZ = nbt.getInt("sizeZ");

        if (nbt.contains("corner1")) {
            corner1 = BlockPos.fromLong(nbt.getLong("corner1"));
            corner2 = BlockPos.fromLong(nbt.getLong("corner2"));
        }

        if (nbt.contains("color")) color = nbt.getInt("color");

        NbtList injNbt = nbt.getList("scannedInjectors", 10);
        scannedInjectors = new ArrayList<>();
        for (int i = 0; i < injNbt.size(); i++) scannedInjectors.add(BlockPos.fromLong(injNbt.getCompound(i).getLong("p")));

        NbtList injNameNbt = nbt.getList("injectorNames", 10);
        injectorNames = new LinkedHashMap<>();
        for (int i = 0; i < injNameNbt.size(); i++) {
            NbtCompound e = injNameNbt.getCompound(i);
            injectorNames.put(BlockPos.fromLong(e.getLong("p")), e.getString("n"));
        }

        NbtList senNbt = nbt.getList("scannedSensors", 10);
        scannedSensors = new ArrayList<>();
        for (int i = 0; i < senNbt.size(); i++) scannedSensors.add(BlockPos.fromLong(senNbt.getCompound(i).getLong("p")));

        NbtList senNameNbt = nbt.getList("sensorNames", 10);
        sensorNames = new LinkedHashMap<>();
        for (int i = 0; i < senNameNbt.size(); i++) {
            NbtCompound e = senNameNbt.getCompound(i);
            sensorNames.put(BlockPos.fromLong(e.getLong("p")), e.getString("n"));
        }

        NbtList caseNbt = nbt.getList("testCases", 10);
        testCases = new ArrayList<>();
        for (int i = 0; i < caseNbt.size(); i++) testCases.add(TestCase.fromNbt(caseNbt.getCompound(i)));

        if (nbt.contains("selectedCaseIndex")) selectedCaseIndex = nbt.getInt("selectedCaseIndex");
    }
}
