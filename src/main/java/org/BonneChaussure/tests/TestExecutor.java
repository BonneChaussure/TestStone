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

    public enum Phase { RESET, WAITING_RESET, INJECT, OBSERVE, DONE }

    public static final int MAX_TICKS    = 40;
    public static final int RESET_TICKS  = 20;
    public static final int MIN_OBSERVE_TICKS = 10;

    private final TestBenchBlockEntity be;
    private final ServerWorld world;
    private final List<TestCase> cases;

    private int currentCase  = 0;
    private Phase phase      = Phase.RESET;
    private int tickCounter  = 0;
    private final Boolean[] results;

    public TestExecutor(TestBenchBlockEntity be, ServerWorld world, List<TestCase> cases) {
        this.be      = be;
        this.world   = world;
        this.cases   = cases;
        this.results = new Boolean[cases.size()];
    }

    public boolean tick() {
        if (currentCase >= cases.size()) {
            resetAllInjectors();
            sendToAll(Text.literal("━━━━━━━━━━━━━━━━━━━━━━━━━━━━").formatted(Formatting.DARK_GRAY));
            sendSummary();
            be.onTestsDone(results);
            return false;
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
                    // Annonce le cas en cours
                    sendToAll(Text.literal("━━━━━━━━━━━━━━━━━━━━━━━━━━━━").formatted(Formatting.DARK_GRAY));
                    sendToAll(Text.literal("▶ Running : ")
                            .formatted(Formatting.GRAY)
                            .append(Text.literal(tc.name()).formatted(Formatting.WHITE, Formatting.BOLD)));
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
                List<String> failures = getFailures(tc);
                if (tickCounter >= MIN_OBSERVE_TICKS && failures.isEmpty()) {
                    results[currentCase] = true;
                    sendToAll(Text.literal("✔ PASS").formatted(Formatting.GREEN, Formatting.BOLD));
                    nextCase();
                } else if (tickCounter >= MAX_TICKS) {
                    results[currentCase] = false;
                    sendToAll(Text.literal("✘ FAIL").formatted(Formatting.RED, Formatting.BOLD));
                    // Détail des erreurs
                    for (String f : failures) {
                        sendToAll(Text.literal("  • " + f).formatted(Formatting.RED));
                    }
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
    private List<String> getFailures(TestCase tc) {
        List<String> failures = new ArrayList<>();
        for (Map.Entry<BlockPos, Boolean> e : tc.sensorExpected().entrySet()) {
            BlockState state = world.getBlockState(e.getKey());
            if (!state.isOf(ModBlocks.SENSOR)) {
                failures.add("Can't find sensor " + e.getKey().toShortString());
                continue;
            }
            boolean actual   = state.get(SensorBlock.POWERED);
            boolean expected = e.getValue();
            if (actual != expected) {
                // Récupère le nom custom si disponible
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

    private void applyExpected(TestCase tc) {
        for (Map.Entry<BlockPos, Boolean> e : tc.sensorExpected().entrySet()) {
            BlockState state = world.getBlockState(e.getKey());
            if (state.isOf(ModBlocks.SENSOR))
                world.setBlockState(e.getKey(), state.with(SensorBlock.EXPECTED, e.getValue()));
        }
    }

    public int getCurrentCase()   { return currentCase; }
    public Phase getPhase()       { return phase; }
    public Boolean[] getResults() { return results; }
}
