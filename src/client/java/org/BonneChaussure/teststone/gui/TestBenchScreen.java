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
    private static final int H           = 268;
    private static final int ROW_H       = 24;   // hauteur d'une ligne de paramètre
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
    private int maxTicks, minObserveTicks;

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
        this.maxTicks        = handler.maxTicks;
        this.minObserveTicks = handler.minObserveTicks;
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
        int row4 = row3 + ROW_H + 6;
        int row5 = row4 + ROW_H + 6;
        int row6 = row5 + ROW_H + 10;
        int row7 = row6 + ROW_H + 4;

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

        addIntRow(gx, row4, "Max ticks :", () -> maxTicks,
                v -> { maxTicks = v; autoSave(); }, 1, 2000);
        addIntRow(gx, row5, "Min observe :", () -> minObserveTicks,
                v -> { minObserveTicks = v; autoSave(); }, 1, 200);

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Rotate: " + ROTATION_LABELS[rotation]),
                b -> { rotation = (rotation + 1) % 4; autoSave(); clearChildren(); init(); }
        ).dimensions(gx + 8, row6, W - 16, 18).build());

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Entities: " + (captureEntities ? "ON" : "OFF")),
                b -> { captureEntities = !captureEntities; autoSave(); clearChildren(); init(); }
        ).dimensions(gx + 8, row7, W - 16, 18).build());

        // Bouton retour ← — haut à droite
        addDrawableChild(ButtonWidget.builder(Text.literal("←"), b -> openTestCases())
                .dimensions(gx + W - 20, gy + 4, 18, 14).build());
    }

    // ── Construit une ligne label + valeur + boutons -/+ ─────────────────────

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
        field.setEditable(true);
        field.setEditableColor(VAL_COLOR);
        field.setChangedListener(text -> {
            try {
                int v = Integer.parseInt(text.trim());
                if (v >= MIN_SIZE && v <= MAX_SIZE) {
                    field.setEditableColor(VAL_COLOR);
                    setter.set(v);
                    autoSave();
                } else {
                    field.setEditableColor(ERR_COLOR);
                }
            } catch (NumberFormatException e) {
                field.setEditableColor(ERR_COLOR);
            }
        });

        // Bouton +
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> {
            setter.set(Math.min(MAX_SIZE, getter.get() + 1));
            clearChildren(); init();
        }).dimensions(fieldX + fieldW + gap, ry, btnW, 16).build());
    }

    /** Ligne label + champ entier éditable avec min/max personnalisables. */
    private void addIntRow(int gx, int ry, String label, IntGetter getter, IntSetter setter,
                           int min, int max) {
        int fieldX = gx + 110;
        int fieldW = 44;
        int btnW   = 20;
        int gap    = 2;

        addDrawableChild(ButtonWidget.builder(Text.literal("-"), b -> {
            setter.set(Math.max(min, getter.get() - 1));
            clearChildren(); init();
        }).dimensions(fieldX - btnW - gap, ry, btnW, 16).build());

        final TextFieldWidget field = addDrawableChild(new TextFieldWidget(
                textRenderer, fieldX, ry, fieldW, 16, Text.literal(label)));
        field.setText(String.valueOf(getter.get()));
        field.setMaxLength(5);
        field.setEditableColor(VAL_COLOR);
        field.setChangedListener(text -> {
            try {
                int v = Integer.parseInt(text.trim());
                if (v >= min && v <= max) {
                    field.setEditableColor(VAL_COLOR);
                    setter.set(v);
                    autoSave();
                } else {
                    field.setEditableColor(ERR_COLOR);
                }
            } catch (NumberFormatException e) {
                field.setEditableColor(ERR_COLOR);
            }
        });

        addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> {
            setter.set(Math.min(max, getter.get() + 1));
            clearChildren(); init();
        }).dimensions(fieldX + fieldW + gap, ry, btnW, 16).build());
    }

    private void autoSave() {
        ClientPlayNetworking.send(new UpdateBenchPacket(
                handler.benchPos, sizeX, sizeY, sizeZ, color, rotation, captureEntities,
                maxTicks, minObserveTicks));
    }

    // ── Ouvre l'écran des cas de test ─────────────────────────────────────────

    private void openTestCases() {
        assert client != null && client.player != null;
        var h = new TestCaseScreenHandler(0, client.player.getInventory(),
                new TestCaseScreenHandler.SyncData(
                        handler.benchPos,
                        sizeX, sizeY, sizeZ, color, rotation, captureEntities,
                        maxTicks, minObserveTicks,
                        handler.injectors, handler.injectorNames,
                        handler.sensors, handler.sensorNames,
                        handler.testCases, handler.selectedCaseIndex
                ));
        client.setScreen(new TestCaseScreen(h, client.player.getInventory(), Text.literal("")));
    }

    // ── Rendu ─────────────────────────────────────────────────────────────────

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {}

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        int gx = (width  - W) / 2;
        int gy = (height - H) / 2;

        // Fond sombre — même style que TestCaseScreen
        ctx.fill(gx, gy, gx + W, gy + H, 0xCC1A1A1A);
        // Bordure fine
        ctx.fill(gx - 1, gy - 1, gx + W + 1, gy,         0xFF555555);
        ctx.fill(gx - 1, gy + H, gx + W + 1, gy + H + 1, 0xFF555555);
        ctx.fill(gx - 1, gy,     gx,          gy + H,     0xFF555555);
        ctx.fill(gx + W, gy,     gx + W + 1,  gy + H,     0xFF555555);

        super.render(ctx, mx, my, delta);

        // Titre
        ctx.drawText(textRenderer, "TestBench Settings", gx + 8, gy + 10, LABEL_COLOR, false);

        // Labels des lignes
        int row0 = gy + 30;
        int row1 = row0 + ROW_H + 4;
        int row2 = row1 + ROW_H + 4;
        int row3 = row2 + ROW_H + 10;
        int row4 = row3 + ROW_H + 6;
        int row5 = row4 + ROW_H + 6;

        ctx.drawText(textRenderer, "Size X :",      gx + 8, row0 + 4, LABEL_COLOR, false);
        ctx.drawText(textRenderer, "Size Y :",      gx + 8, row1 + 4, LABEL_COLOR, false);
        ctx.drawText(textRenderer, "Size Z :",      gx + 8, row2 + 4, LABEL_COLOR, false);
        ctx.drawText(textRenderer, "Color (hex) :", gx + 8, row3 + 4, LABEL_COLOR, false);
        ctx.drawText(textRenderer, "Max ticks :",   gx + 8, row4 + 4, LABEL_COLOR, false);
        ctx.drawText(textRenderer, "Min observe :", gx + 8, row5 + 4, LABEL_COLOR, false);
    }

    @Override protected void drawBackground(DrawContext ctx, float delta, int mx, int my) {}
    @Override protected void drawForeground(DrawContext ctx, int mx, int my) {}
    @Override public boolean shouldPause() { return false; }
}