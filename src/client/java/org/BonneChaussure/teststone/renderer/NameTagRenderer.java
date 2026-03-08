package org.BonneChaussure.teststone.renderer;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import org.BonneChaussure.blocks.InjectorBlockEntity;
import org.BonneChaussure.blocks.SensorBlockEntity;
import org.joml.Matrix4f;

public class NameTagRenderer {

    public static void render(WorldRenderContext ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        // Uniquement si le joueur vise un bloc
        if (client.crosshairTarget == null
                || client.crosshairTarget.getType() != HitResult.Type.BLOCK) return;

        BlockPos pos = ((BlockHitResult) client.crosshairTarget).getBlockPos();

        // Récupère le nom selon le type de block entity
        String name = null;
        if (client.world.getBlockEntity(pos) instanceof InjectorBlockEntity be)
            name = be.getCustomName();
        else if (client.world.getBlockEntity(pos) instanceof SensorBlockEntity be)
            name = be.getCustomName();

        if (name == null || name.isEmpty()) return;

        // ── Rendu ─────────────────────────────────────────────────────────────
        MatrixStack matrices = ctx.matrixStack();
        if (matrices == null) return;

        VertexConsumerProvider.Immediate consumers =
                (VertexConsumerProvider.Immediate) ctx.consumers();
        if (consumers == null) return;

        matrices.push();

        // Translation : centre du bloc + décalage caméra + hauteur
        var cam = ctx.camera().getPos();
        matrices.translate(
                pos.getX() + 0.5 - cam.x,
                pos.getY() + 1.35  - cam.y,
                pos.getZ() + 0.5  - cam.z
        );

        // Billboard — fait face à la caméra
        matrices.multiply(ctx.camera().getRotation());
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f));

        float scale = 0.025f;
        matrices.scale(-scale, -scale, scale);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        TextRenderer textRenderer = client.textRenderer;
        int textWidth = textRenderer.getWidth(name);

        textRenderer.draw(
                name,
                -textWidth / 2f,
                0f,
                0xFFFFFF,
                false,
                matrix,
                consumers,
                TextRenderer.TextLayerType.NORMAL,
                0x40000000,
                0xF000F0    // light max — le nametag est toujours bien éclairé
        );

        consumers.draw();
        matrices.pop();
    }
}
