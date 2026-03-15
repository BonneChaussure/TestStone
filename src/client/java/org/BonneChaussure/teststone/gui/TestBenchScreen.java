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
    private static final int H           = 180;
    private static final int ROW_H       = 24;   // hauteur d'une ligne de paramètre
    private static final int LABEL_COLOR = 0xAAAAAA;
    private static final int VAL_COLOR   = 0xFFFFFF;
    private static final int ERR_COLOR   = 0xFF4444;
    private static final int MIN_SIZE    = 1;
    private static final int MAX_SIZE    = 64;

    // État local — modifié en direct, envoyé à chaque changement
    private int sizeX, sizeY, sizeZ, color;

    private TextFieldWidget fieldColor;

    public TestBenchScreen(TestBenchScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth  = W;
        this.backgroundHeight = H;
        this.sizeX  = handler.sizeX;
        this.sizeY  = handler.sizeY;
        this.sizeZ  = handler.sizeZ;
        this.color  = handler.color;
    }

    @Override
    protected void init() {
        super.init();
        int gx = (width  - W) / 2;
        int gy = (height - H) / 2;

        // ── Ligne Size X ──────────────────────────────────────────────────────
        int row0 = gy + 30;
        addSizeRow(gx, row0, "Size X", () -> sizeX,
                v -> { sizeX = v; autoSave(); });

        // ── Ligne Size Y ──────────────────────────────────────────────────────
        int row1 = row0 + ROW_H + 6;
        addSizeRow(gx, row1, "Size Y", () -> sizeY,
                v -> { sizeY = v; autoSave(); });

        // ── Ligne Size Z ──────────────────────────────────────────────────────
        int row2 = row1 + ROW_H + 6;
        addSizeRow(gx, row2, "Size Z", () -> sizeZ,
                v -> { sizeZ = v; autoSave(); });

        // ── Ligne Color ───────────────────────────────────────────────────────
        int row3 = row2 + ROW_H + 12;
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

        // ── Bouton Test cases ─────────────────────────────────────────────────
        int btnY = gy + H - 26;
        addDrawableChild(ButtonWidget.builder(Text.literal("Test cases →"), b -> openTestCases())
                .dimensions(gx + W / 2 - 55, btnY, 110, 20).build());
    }

    // ── Construit une ligne label + valeur + boutons -/+ ─────────────────────

    private interface IntGetter { int get(); }
    private interface IntSetter { void set(int v); }

    private void addSizeRow(int gx, int ry, String label, IntGetter getter, IntSetter setter) {
        int fieldX = gx + 110;
        int fieldW = 36;
        int btnW   = 20;
        int gap    = 2;

        // Bouton −
        addDrawableChild(ButtonWidget.builder(Text.literal("−"), b -> {
            int v = Math.max(MIN_SIZE, getter.get() - 1);
            setter.set(v);
            // Rebuild pour rafraîchir la valeur affichée
            clearChildren();
            init();
        }).dimensions(fieldX - btnW - gap, ry, btnW, 16).build());

        // Affichage de la valeur (non éditable — les boutons suffisent)
        // On utilise un TextFieldWidget read-only pour rester cohérent visuellement
        TextFieldWidget field = addDrawableChild(new TextFieldWidget(
                textRenderer, fieldX, ry, fieldW, 16, Text.literal(label)));
        field.setText(String.valueOf(getter.get()));
        field.setEditable(false);
        field.setEditableColor(VAL_COLOR);

        // Bouton +
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> {
            int v = Math.min(MAX_SIZE, getter.get() + 1);
            setter.set(v);
            clearChildren();
            init();
        }).dimensions(fieldX + fieldW + gap, ry, btnW, 16).build());
    }

    // ── Sauvegarde automatique ────────────────────────────────────────────────

    private void autoSave() {
        ClientPlayNetworking.send(new UpdateBenchPacket(handler.benchPos, sizeX, sizeY, sizeZ, color));
    }

    // ── Ouvre l'écran des cas de test ─────────────────────────────────────────

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
        int row1 = row0 + ROW_H + 6;
        int row2 = row1 + ROW_H + 6;
        int row3 = row2 + ROW_H + 12;

        ctx.drawText(textRenderer, "Size X :", gx + 8, row0 + 4, LABEL_COLOR, false);
        ctx.drawText(textRenderer, "Size Y :", gx + 8, row1 + 4, LABEL_COLOR, false);
        ctx.drawText(textRenderer, "Size Z :", gx + 8, row2 + 4, LABEL_COLOR, false);
        ctx.drawText(textRenderer, "Color (hex) :", gx + 8, row3 + 4, LABEL_COLOR, false);
    }

    @Override protected void drawBackground(DrawContext ctx, float delta, int mx, int my) {}
    @Override protected void drawForeground(DrawContext ctx, int mx, int my) {}
    @Override public boolean shouldPause() { return false; }
}