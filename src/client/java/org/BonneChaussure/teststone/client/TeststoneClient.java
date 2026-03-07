package org.BonneChaussure.teststone.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.BonneChaussure.gui.TestBenchScreenHandler;
import org.BonneChaussure.gui.TestCaseScreenHandler;
import org.BonneChaussure.network.RemoveBoundaryBoxPacket;
import org.BonneChaussure.network.SetBoundaryBoxPacket;
import org.BonneChaussure.network.SyncScannedBlocksPacket;
import org.BonneChaussure.network.SyncTestResultPacket;
import org.BonneChaussure.teststone.gui.TestBenchScreen;
import org.BonneChaussure.teststone.gui.TestCaseScreen;
import org.BonneChaussure.teststone.renderer.BoundaryBoxClientData;
import org.BonneChaussure.teststone.renderer.BoundaryBoxRenderer;

import java.util.ArrayList;
import java.util.List;

public class TeststoneClient implements ClientModInitializer {
    // Stockage temporaire des résultats du scan
    public static List<BlockPos> lastScannedInjectors = new ArrayList<>();
    public static List<BlockPos> lastScannedSensors   = new ArrayList<>();
    public static BlockPos lastScannedBench     = null;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(SetBoundaryBoxPacket.ID, (payload, context) -> {
            BoundaryBoxClientData.boxes.put(payload.bench(), new BoundaryBoxClientData.BoxData(payload.corner1(), payload.corner2(), payload.color()));
        });

        ClientPlayNetworking.registerGlobalReceiver(RemoveBoundaryBoxPacket.ID, (payload, context) -> {
            BoundaryBoxClientData.boxes.remove(payload.bench());
        });

        WorldRenderEvents.AFTER_TRANSLUCENT.register(BoundaryBoxRenderer::render);

        // Force l'init des types avant d'enregistrer les screens
        var _sh = TestBenchScreenHandler.TYPE;
        var _tc = TestCaseScreenHandler.TYPE;

        HandledScreens.register(TestBenchScreenHandler.TYPE, TestBenchScreen::new);
        HandledScreens.register(TestCaseScreenHandler.TYPE, TestCaseScreen::new);

        // Réception du scan → ouvre le 2e GUI si l'écran TestBench est ouvert
        ClientPlayNetworking.registerGlobalReceiver(SyncScannedBlocksPacket.ID, (payload, context) -> {
            context.client().execute(() -> {
                lastScannedBench     = payload.bench();
                lastScannedInjectors = payload.injectors();
                lastScannedSensors   = payload.sensors();

                // Actualise le screen si c'est un TestCaseScreen ouvert
                if (context.client().currentScreen instanceof TestCaseScreen screen) {
                    screen.onScanReceived(payload.injectors(), payload.sensors());
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncTestResultPacket.ID, (payload, ctx) -> {
            ctx.client().execute(() -> {
                if (ctx.client().currentScreen instanceof TestCaseScreen screen) {
                    if (payload.caseIdx() >= 0) {
                        screen.onCaseResult(payload.caseIdx(), payload.pass());
                    } else {
                        screen.onAllResults(payload.allResults());
                    }
                }
            });
        });
    }
}
