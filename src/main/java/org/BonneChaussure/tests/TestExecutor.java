package org.BonneChaussure.tests;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.BonneChaussure.blocks.InjectorBlock;
import org.BonneChaussure.blocks.ModBlocks;
import org.BonneChaussure.blocks.SensorBlock;
import org.BonneChaussure.blocks.TestBenchBlockEntity;

import java.util.List;
import java.util.Map;

/**
 * Moteur d'exécution des tests.
 * Instancié et tenu en vie par TestBenchBlockEntity.
 * Appelé à chaque tick serveur via be.tick().
 */
public class TestExecutor {

    public enum Phase { RESET, WAITING_RESET, INJECT, OBSERVE, DONE }

    public static final int MAX_TICKS = 40; // 2 secondes
    public static final int RESET_TICKS = 20;
    public static final int MIN_OBSERVE_TICKS = 10;

    private final TestBenchBlockEntity be;
    private final ServerWorld world;
    private final List<TestCase> cases;

    private int currentCase = 0;
    private Phase phase = Phase.RESET;
    private int tickCounter = 0;

    // Résultats : true = PASS, false = FAIL, null = pas encore joué
    private final Boolean[] results;

    public TestExecutor(TestBenchBlockEntity be, ServerWorld world, List<TestCase> cases) {
        this.be = be;
        this.world = world;
        this.cases = cases;
        this.results = new Boolean[cases.size()];
    }

    /**
     * Appelé à chaque tick serveur. Retourne true tant que l'exécution continue.
     */
    public boolean tick() {
        if (currentCase >= cases.size()) {
            resetAllInjectors();
            be.onTestsDone(results);
            return false; // fin
        }

        TestCase tc = cases.get(currentCase);

        switch (phase) {

            case RESET -> {
                resetAllInjectors();
                phase = Phase.WAITING_RESET;
                tickCounter = 0;
            }

            case WAITING_RESET -> {
                tickCounter++;
                if (tickCounter >= RESET_TICKS) {
                    phase = Phase.INJECT;
                    tickCounter = 0;
                }
            }

            case INJECT -> {
                applyInjectors(tc);
                applyExpected(tc);
                phase = Phase.OBSERVE;
                tickCounter = 0;
            }

            case OBSERVE -> {
                tickCounter++;
                if (tickCounter >= MIN_OBSERVE_TICKS && checkSensors(tc)) {
                    results[currentCase] = true;
                    nextCase();
                } else if (tickCounter >= MAX_TICKS) {
                    results[currentCase] = false;
                    nextCase();
                }
            }

            case DONE -> { return false; }
        }

        return true;
    }

    private void nextCase() {
        be.onCaseResult(currentCase, results[currentCase]);
        currentCase++;
        phase = Phase.RESET;
        tickCounter = 0;
    }

    // ── Manipulation des blocs ──────────────────────────────────────────

    private void resetAllInjectors() {
        for (BlockPos p : be.getScannedInjectors()) {
            BlockState state = world.getBlockState(p);
            if (state.isOf(ModBlocks.INJECTOR)) {
                world.setBlockState(p, state.with(InjectorBlock.POWERED, false));
            }
        }

        // Reset EXPECTED sur tous les sensors
        for (BlockPos p : be.getScannedSensors()) {
            BlockState state = world.getBlockState(p);
            if (state.isOf(ModBlocks.SENSOR)) {
                world.setBlockState(p, state.with(SensorBlock.EXPECTED, false));
            }
        }
    }

    private void applyInjectors(TestCase tc) {
        for (Map.Entry<BlockPos, Boolean> e : tc.injectorValues().entrySet()) {
            BlockState state = world.getBlockState(e.getKey());
            if (state.isOf(ModBlocks.INJECTOR)) {
                world.setBlockState(e.getKey(), state.with(InjectorBlock.POWERED, e.getValue()));
            }
        }
    }

    private boolean checkSensors(TestCase tc) {
        for (Map.Entry<BlockPos, Boolean> e : tc.sensorExpected().entrySet()) {
            BlockState state = world.getBlockState(e.getKey());
            if (!state.isOf(ModBlocks.SENSOR)) return false;
            boolean actual = state.get(SensorBlock.POWERED);
            if (actual != e.getValue()) return false;
        }
        return true;
    }

    private void applyExpected(TestCase tc) {
        for (Map.Entry<BlockPos, Boolean> e : tc.sensorExpected().entrySet()) {
            BlockState state = world.getBlockState(e.getKey());
            if (state.isOf(ModBlocks.SENSOR)) {
                world.setBlockState(e.getKey(), state.with(SensorBlock.EXPECTED, e.getValue()));
            }
        }
    }

    public int getCurrentCase() { return currentCase; }
    public Phase getPhase()     { return phase; }
    public Boolean[] getResults() { return results; }
}
