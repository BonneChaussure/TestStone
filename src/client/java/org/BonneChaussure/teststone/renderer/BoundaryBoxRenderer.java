package org.BonneChaussure.teststone.renderer;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import org.joml.Matrix4f;

public class BoundaryBoxRenderer {

    public static void render(WorldRenderContext ctx) {
        if (BoundaryBoxClientData.boxes.isEmpty()) return;

        MatrixStack matrices = ctx.matrixStack();
        if (matrices == null) return;

        VertexConsumerProvider.Immediate consumers = (VertexConsumerProvider.Immediate) ctx.consumers();
        if (consumers == null) return;

        matrices.push();
        var cam = ctx.camera().getPos();
        matrices.translate(-cam.x, -cam.y, -cam.z);

        VertexConsumer vc = consumers.getBuffer(RenderLayer.getLines());
        Matrix4f mat = matrices.peek().getPositionMatrix();

        // Itère sur toutes les boxes
        for (BoundaryBoxClientData.BoxData box : BoundaryBoxClientData.boxes.values()) {
            // Extraire r,g,b depuis le int color
            float r = ((box.color() >> 16) & 0xFF) / 255f;
            float g = ((box.color() >> 8)  & 0xFF) / 255f;
            float b = ( box.color()        & 0xFF) / 255f;

            BlockPos c1 = box.corner1();
            BlockPos c2 = box.corner2();

            float x1 = Math.min(c1.getX(), c2.getX());
            float y1 = Math.min(c1.getY(), c2.getY());
            float z1 = Math.min(c1.getZ(), c2.getZ());
            float x2 = Math.max(c1.getX(), c2.getX()) + 1f;
            float y2 = Math.max(c1.getY(), c2.getY()) + 1f;
            float z2 = Math.max(c1.getZ(), c2.getZ()) + 1f;

            drawCubeFaces(vc, mat, x1,y1,z1, x2,y2,z2, r,g,b, 1f);
        }

        consumers.draw(RenderLayer.getLines());
        matrices.pop();
    }

    private static void line(VertexConsumer vc, Matrix4f mat,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float r, float g, float b, float a) {
        // Le format LINES attend : position + color + normal
        vc.vertex(mat, x1, y1, z1).color(r, g, b, a).normal(0f, 1f, 0f);
        vc.vertex(mat, x2, y2, z2).color(r, g, b, a).normal(0f, 1f, 0f);
    }

    private static void drawCubeFaces(VertexConsumer vc, Matrix4f mat, float x1,float y1,float z1, float x2,float y2,float z2, float r, float g, float b, float a){
        // Bottom
        line(vc, mat, x1,y1,z1, x2,y1,z1, r,g,b,a);
        line(vc, mat, x2,y1,z1, x2,y1,z2, r,g,b,a);
        line(vc, mat, x2,y1,z2, x1,y1,z2, r,g,b,a);
        line(vc, mat, x1,y1,z2, x1,y1,z1, r,g,b,a);
        // Top
        line(vc, mat, x1,y2,z1, x2,y2,z1, r,g,b,a);
        line(vc, mat, x2,y2,z1, x2,y2,z2, r,g,b,a);
        line(vc, mat, x2,y2,z2, x1,y2,z2, r,g,b,a);
        line(vc, mat, x1,y2,z2, x1,y2,z1, r,g,b,a);
        // Piliers
        line(vc, mat, x1,y1,z1, x1,y2,z1, r,g,b,a);
        line(vc, mat, x2,y1,z1, x2,y2,z1, r,g,b,a);
        line(vc, mat, x2,y1,z2, x2,y2,z2, r,g,b,a);
        line(vc, mat, x1,y1,z2, x1,y2,z2, r,g,b,a);
    }
}
