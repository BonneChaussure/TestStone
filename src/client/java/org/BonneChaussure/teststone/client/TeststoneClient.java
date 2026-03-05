package org.BonneChaussure.teststone.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import org.BonneChaussure.teststone.renderer.BoundaryBoxRenderer;

public class TeststoneClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(BoundaryBoxRenderer::render);
    }
}
