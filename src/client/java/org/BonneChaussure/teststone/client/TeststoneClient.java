package org.BonneChaussure.teststone.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.util.math.BlockPos;
import org.BonneChaussure.network.RemoveBoundaryBoxPacket;
import org.BonneChaussure.network.SetBoundaryBoxPacket;
import org.BonneChaussure.teststone.renderer.BoundaryBoxClientData;
import org.BonneChaussure.teststone.renderer.BoundaryBoxRenderer;

public class TeststoneClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(SetBoundaryBoxPacket.ID, (payload, context) -> {
            BoundaryBoxClientData.boxes.put(payload.bench(), new BlockPos[]{payload.corner1(), payload.corner2()});
        });

        ClientPlayNetworking.registerGlobalReceiver(RemoveBoundaryBoxPacket.ID, (payload, context) -> {
            BoundaryBoxClientData.boxes.remove(payload.bench());
        });

        WorldRenderEvents.AFTER_TRANSLUCENT.register(BoundaryBoxRenderer::render);
    }
}
