package org.BonneChaussure.teststone.gui;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import org.BonneChaussure.gui.TestBenchScreenHandler;
import org.BonneChaussure.gui.TestCaseScreenHandler;
import org.BonneChaussure.network.UpdateBenchPacket;

public class TestBenchScreen extends HandledScreen<TestBenchScreenHandler> {

    private static final int W           = 220;
    private static final int H           = 230;
    private static final int ROW_H       = 24;
    private static final int LABEL_COLOR = 0xAAAAAA;
    private static final int VAL_COLOR   = 0xFFFFFF;
    private static final int ERR_COLOR   = 0xFF4444;
    private static final int MIN_SIZE    = 1;
    private static final int MAX_SIZE    = 64;

    private static final String[] ROTATION_LABELS = {
            "None  (0°)",
            "Rot   (90°)",
            "Rot  (180°)",
            "Rot  (270°)"
    };

    private int sizeX, sizeY, sizeZ, color, rotation;
    private boolean captureEntities;

    private TextFieldWidget fieldColor;

    public TestBenchScreen(TestBenchScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth  = W;
        this.backgroundHeight = H;
        this.sizeX           = handler.sizeX;
        this.sizeY           = handler.sizeY;
        this.sizeZ           = handler.sizeZ;
        this.color           = handler.color;
        this.rotation        = handler.rotation;
        this.captureEntities = handler.captureEntities;
    }

    @Override
    protected void init() {
        super.init();
        int gx = (width  - W) / 2;
        int gy = (height - H) / 2;

        int row0 = gy + 30;
        int row1 = row0 + ROW_H + 4;
        int row2 = row1 + ROW_H + 4;
        int row3 = row2 + ROW_H + 10;
        int row4 = row3 + ROW_H + 10;
        int row5 = row4 + ROW_H + 4;

        addSizeRow(gx, row0, "Size X", () -> sizeX, v -> { sizeX = v; autoSave(); });
        addSizeRow(gx, row1, "Size Y", () -> sizeY, v -> { sizeY = v; autoSave(); });
        addSizeRow(gx, row2, "Size Z", () -> sizeZ, v -> { sizeZ = v; autoSave(); });

        fieldColor = addDrawableChild(new TextFieldWidget(
                textRenderer, gx + 110, row3, 70, 16, Text.literal("Color")));
        fieldColor.setText(String.format("%06X", color));
        fieldColor.setMaxLength(6);
        fieldColor.setChangedListener(text -> {
            try {
                color = Integer.parseInt(text.trim(), 16);
                fieldColor.setEditableColor(VAL_COLOR);
                autoSave();
            } catch (NumberFormatException e) {
                fieldColor.setEditableColor(ERR_COLOR);
            }
        });

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Rotate: " + ROTATION_LABELS[rotation]),
                b -> { rotation = (rotation + 1) % 4; autoSave(); clearChildren(); init(); }
        ).dimensions(gx + 8, row4, W - 16, 18).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Entities: " + (captureEntities ? "ON" : "OFF")),
                b -> { captureEntities = !captureEntities; autoSave(); clearChildren(); init(); }
        ).dimensions(gx + 8, row5, W - 16, 18).build());

        int btnY = gy + H - 26;
        addDrawableChild(ButtonWidget.builder(Text.literal("Test cases ->"), b -> openTestCases())
                .dimensions(gx + W / 2 - 55, btnY, 110, 20).build());
    }

    private interface IntGetter { int get(); }
    private interface IntSetter { void set(int v); }

    private void addSizeRow(int gx, int ry, String label, IntGetter getter, IntSetter setter) {
        int fieldX = gx + 110;
        int fieldW = 36;
        int btnW   = 20;
        int gap    = 2;

        addDrawableChild(ButtonWidget.builder(Text.literal("-"), b -> {
            setter.set(Math.max(MIN_SIZE, getter.get() - 1));
            clearChildren(); init();
        }).dimensions(fieldX - btnW - gap, ry, btnW, 16).build());

        TextFieldWidget field = addDrawableChild(new TextFieldWidget(
                textRenderer, fieldX, ry, fieldW, 16, Text.literal(label)));
        field.setText(String.valueOf(getter.get()));
        field.setEditable(false);
        field.setEditableColor(VAL_COLOR);

        addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> {
            setter.set(Math.min(MAX_SIZE, getter.get() + 1));
            clearChildren(); init();
        }).dimensions(fieldX + fieldW + gap, ry, btnW, 16).build());
    }

    private void autoSave() {
        ClientPlayNetworking.send(new UpdateBenchPacket(
                handler.benchPos, sizeX, sizeY, sizeZ, color, rotation, captureEntities));
    }

    private void openTestCases() {
        assert client != null && client.player != null;
        var h = new TestCaseScreenHandler(0, client.player.getInventory(),
                new TestCaseScreenHandler.SyncData(
                        handler.benchPos,
                        handler.injectors,
                        handler.injectorNames,
                        handler.sensors,
                        handler.sensorNames,
                        handler.testCases,
                        handler.selectedCaseIndex
                ));
        client.setScreen(new TestCaseScreen(h, client.player.getInventory(), Text.literal("")));
    }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {}

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        int gx = (width  - W) / 2;
        int gy = (height - H) / 2;

        ctx.fill(gx, gy, gx + W, gy + H, 0xCC1A1A1A);
        ctx.fill(gx - 1, gy - 1, gx + W + 1, gy,         0xFF555555);
        ctx.fill(gx - 1, gy + H, gx + W + 1, gy + H + 1, 0xFF555555);
        ctx.fill(gx - 1, gy,     gx,          gy + H,     0xFF555555);
        ctx.fill(gx + W, gy,     gx + W + 1,  gy + H,     0xFF555555);

        super.render(ctx, mx, my, delta);

        ctx.drawText(textRenderer, "TestBench Settings", gx + 8, gy + 10, LABEL_COLOR, false);

        int row0 = gy + 30;
        int row1 = row0 + ROW_H + 4;
        int row2 = row1 + ROW_H + 4;
        int row3 = row2 + ROW_H + 10;
        int row4 = row3 + ROW_H + 10;
        int row5 = row4 + ROW_H + 4;

        ctx.drawText(textRenderer, "Size X :",      gx + 8, row0 + 4, LABEL_COLOR, false);
        ctx.drawText(textRenderer, "Size Y :",      gx + 8, row1 + 4, LABEL_COLOR, false);
        ctx.drawText(textRenderer, "Size Z :",      gx + 8, row2 + 4, LABEL_COLOR, false);
        ctx.drawText(textRenderer, "Color (hex) :", gx + 8, row3 + 4, LABEL_COLOR, false);

        ctx.fill(gx + 4, row4 - 5, gx + W - 4, row4 - 4, 0xFF444444);
        ctx.fill(gx + 4, row5 - 2, gx + W - 4, row5 - 1, 0xFF333333);
    }

    @Override protected void drawBackground(DrawContext ctx, float delta, int mx, int my) {}
    @Override protected void drawForeground(DrawContext ctx, int mx, int my) {}
    @Override public boolean shouldPause() { return false; }
}