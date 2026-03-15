package org.BonneChaussure.tests;

import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.BonneChaussure.blocks.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Moteur d'exécution des tests.
 * Instancié et tenu en vie par TestBenchBlockEntity.
 * Appelé à chaque tick serveur via be.tick().
 */
public class TestExecutor {

    public enum Phase { RESTORE, INJECT, OBSERVE}

    private final TestBenchBlockEntity be;
    private final ServerWorld world;
    private final List<TestCase> cases;
    private final int indexOffset;

    private int currentCase        = 0;
    private int currentObservation = 0;   // index dans tc.observations()
    private Phase phase            = Phase.RESTORE;
    private int tickCounter        = 0;
    private final Boolean[] results;

    /** Constructeur normal — lance tous les cas, index offset = 0. */
    public TestExecutor(TestBenchBlockEntity be, ServerWorld world, List<TestCase> cases) {
        this(be, world, cases, 0);
    }

    /** Constructeur avec offset — utilisé pour "run single test". */
    public TestExecutor(TestBenchBlockEntity be, ServerWorld world,
                        List<TestCase> cases, int indexOffset) {
        this.be          = be;
        this.world       = world;
        this.cases       = cases;
        this.results     = new Boolean[cases.size()];
        this.indexOffset = indexOffset;
    }

    public boolean tick() {
        if (currentCase >= cases.size()) {
            be.restoreStructure(world);
            resetAllInjectors();
            sendToAll(Text.literal("━━━━━━━━━━━━━━━━━━━━━━━━━━━━").formatted(Formatting.DARK_GRAY));
            sendSummary();
            be.onTestsDone(results);
            return false;
        }

        TestCase tc = cases.get(currentCase);

        switch (phase) {

            case RESTORE -> {
                be.restoreStructure(world);
                // Annonce le cas en cours
                sendToAll(Text.literal("━━━━━━━━━━━━━━━━━━━━━━━━━━━━").formatted(Formatting.DARK_GRAY));
                sendToAll(Text.literal("▶ Running : ")
                        .formatted(Formatting.GRAY)
                        .append(Text.literal(tc.name()).formatted(Formatting.WHITE, Formatting.BOLD)));
                phase = Phase.INJECT;
                tickCounter = 0;
            }

            case INJECT -> {
                applyInjectors(tc);
                applyExpected(tc, currentObservation);
                phase = Phase.OBSERVE;
                tickCounter = 0;
            }

            case OBSERVE -> {
                tickCounter++;
                Map<BlockPos, Boolean> obs = tc.observations().get(currentObservation);
                List<String> failures = getFailures(obs);

                if (tickCounter >= be.getMinObserveTicks() && failures.isEmpty()) {
                    // Observation courante validée
                    int totalObs = tc.observations().size();
                    if (totalObs > 1) {
                        sendToAll(Text.literal("  ✔ obs " + (currentObservation + 1) + "/" + totalObs)
                                .formatted(Formatting.GREEN));
                    }
                    currentObservation++;
                    if (currentObservation < totalObs) {
                        // Il reste des observations — on continue sans restaurer
                        applyExpected(tc, currentObservation);
                        tickCounter = 0;
                        // phase reste OBSERVE
                    } else {
                        // Toutes les observations passées → cas PASS
                        results[currentCase] = true;
                        sendToAll(Text.literal("✔ PASS").formatted(Formatting.GREEN, Formatting.BOLD));
                        nextCase();
                    }
                } else if (tickCounter >= be.getMaxTicks()) {
                    // Timeout sur cette observation → cas FAIL
                    results[currentCase] = false;
                    int totalObs = tc.observations().size();
                    if (totalObs > 1) {
                        sendToAll(Text.literal("✘ FAIL (obs " + (currentObservation + 1) + "/" + totalObs + ")")
                                .formatted(Formatting.RED, Formatting.BOLD));
                    } else {
                        sendToAll(Text.literal("✘ FAIL").formatted(Formatting.RED, Formatting.BOLD));
                    }
                    for (String f : failures) {
                        sendToAll(Text.literal("  • " + f).formatted(Formatting.RED));
                    }
                    nextCase();
                }
            }
        }

        return true;
    }

    private void nextCase() {
        // On transmet l'index réel dans la liste complète (currentCase + indexOffset)
        be.onCaseResult(currentCase + indexOffset, results[currentCase]);
        currentCase++;
        currentObservation = 0;
        phase = Phase.RESTORE;
        tickCounter = 0;
    }

    // ── Résumé final ──────────────────────────────────────────────────────────

    private void sendSummary() {
        int pass = 0, fail = 0;
        for (Boolean r : results) {
            if (r != null && r) pass++;
            else fail++;
        }

        sendToAll(Text.literal("Results : ")
                .formatted(Formatting.GRAY)
                .append(Text.literal(pass + " PASS ").formatted(Formatting.GREEN))
                .append(Text.literal("/ " + fail + " FAIL").formatted(fail > 0 ? Formatting.RED : Formatting.GREEN)));

        for (int i = 0; i < cases.size(); i++) {
            boolean p = results[i] != null && results[i];
            sendToAll(Text.literal("  " + (p ? "✔" : "✘") + " " + cases.get(i).name())
                    .formatted(p ? Formatting.GREEN : Formatting.RED));
        }
    }

    // ── Vérification des sensors avec détail ─────────────────────────────────

    /** Retourne la liste des erreurs — vide = tout est bon */
    private List<String> getFailures(Map<BlockPos, Boolean> obs) {
        List<String> failures = new ArrayList<>();
        for (Map.Entry<BlockPos, Boolean> e : obs.entrySet()) {
            BlockState state = world.getBlockState(e.getKey());
            if (!state.isOf(ModBlocks.SENSOR)) {
                failures.add("Can't find sensor " + e.getKey().toShortString());
                continue;
            }
            boolean actual   = state.get(SensorBlock.POWERED);
            boolean expected = e.getValue();
            if (actual != expected) {
                String sensorName = (world.getBlockEntity(e.getKey()) instanceof SensorBlockEntity sbe
                        && !sbe.getCustomName().isEmpty())
                        ? sbe.getCustomName()
                        : e.getKey().toShortString();
                failures.add(sensorName + " : expected "
                        + (expected ? "ON" : "OFF") + ", received "
                        + (actual   ? "ON" : "OFF"));
            }
        }
        return failures;
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private void sendToAll(Text message) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            player.sendMessage(message, false); // false = chat, pas action bar
        }
    }

    private void resetAllInjectors() {
        for (BlockPos p : be.getScannedInjectors()) {
            BlockState state = world.getBlockState(p);
            if (state.isOf(ModBlocks.INJECTOR))
                world.setBlockState(p, state.with(InjectorBlock.POWERED, false));
        }
        for (BlockPos p : be.getScannedSensors()) {
            BlockState state = world.getBlockState(p);
            if (state.isOf(ModBlocks.SENSOR))
                world.setBlockState(p, state.with(SensorBlock.EXPECTED, false));
        }
    }

    private void applyInjectors(TestCase tc) {
        for (Map.Entry<BlockPos, Boolean> e : tc.injectorValues().entrySet()) {
            BlockState state = world.getBlockState(e.getKey());
            if (state.isOf(ModBlocks.INJECTOR))
                world.setBlockState(e.getKey(), state.with(InjectorBlock.POWERED, e.getValue()));
        }
    }

    private void applyExpected(TestCase tc, int obsIndex) {
        Map<BlockPos, Boolean> obs = tc.observations().get(obsIndex);
        for (Map.Entry<BlockPos, Boolean> e : obs.entrySet()) {
            BlockState state = world.getBlockState(e.getKey());
            if (state.isOf(ModBlocks.SENSOR))
                world.setBlockState(e.getKey(), state.with(SensorBlock.EXPECTED, e.getValue()));
        }
    }

    public int getCurrentCase()   { return currentCase; }
    public Phase getPhase()       { return phase; }
    public Boolean[] getResults() { return results; }
}