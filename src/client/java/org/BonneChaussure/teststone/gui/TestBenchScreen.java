package org.BonneChaussure.teststone.gui;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.BonneChaussure.gui.TestBenchScreenHandler;
import org.BonneChaussure.gui.TestCaseScreenHandler;
import org.BonneChaussure.network.ScanBenchPacket;
import org.BonneChaussure.network.UpdateBenchPacket;
import org.BonneChaussure.teststone.client.TeststoneClient;

import java.util.List;

public class TestBenchScreen extends HandledScreen<TestBenchScreenHandler> {

    private TextFieldWidget fieldSizeX, fieldSizeY, fieldSizeZ, fieldColor;

    public TestBenchScreen(TestBenchScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth  = 200;
        this.backgroundHeight = 150;
    }

    @Override
    protected void init() {
        super.init();
        int x = (width  - backgroundWidth)  / 2;
        int y = (height - backgroundHeight) / 2;

        // Champs de saisie
        fieldSizeX = addDrawableChild(new TextFieldWidget(textRenderer, x + 80, y + 20, 40, 16, Text.literal("SizeX")));
        fieldSizeY = addDrawableChild(new TextFieldWidget(textRenderer, x + 80, y + 44, 40, 16, Text.literal("SizeY")));
        fieldSizeZ = addDrawableChild(new TextFieldWidget(textRenderer, x + 80, y + 68, 40, 16, Text.literal("SizeZ")));
        fieldColor = addDrawableChild(new TextFieldWidget(textRenderer, x + 80, y + 92, 60, 16, Text.literal("Color")));

        // Valeurs initiales depuis le handler
        fieldSizeX.setText(String.valueOf(handler.sizeX));
        fieldSizeY.setText(String.valueOf(handler.sizeY));
        fieldSizeZ.setText(String.valueOf(handler.sizeZ));
        fieldColor.setText(String.format("%06X", handler.color));

        // Limites de saisie
        fieldSizeX.setMaxLength(3);
        fieldSizeY.setMaxLength(3);
        fieldSizeZ.setMaxLength(3);
        fieldColor.setMaxLength(6);

        // Bouton Sauvegarder
        addDrawableChild(ButtonWidget.builder(Text.literal("Save"), btn -> save())
                .dimensions(x + 75, y + 118, 90, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Cas de test →"), btn -> openTestCases())
                .dimensions(x + 150, y + 118, 80, 20)
                .build());
    }

    private void save() {
        try {
            // Parsing robuste : accepte "7.8" → 7, rejette les négatifs et le zéro
            int sx = parsePositiveInt(fieldSizeX.getText());
            int sy = parsePositiveInt(fieldSizeY.getText());
            int sz = parsePositiveInt(fieldSizeZ.getText());
            int col = Integer.parseInt(fieldColor.getText().trim(), 16);

            ClientPlayNetworking.send(new UpdateBenchPacket(handler.benchPos, sx, sy, sz, col));
            close();
        } catch (IllegalArgumentException e) {
            // Affiche les champs en rouge pour signaler l'erreur
            fieldSizeX.setEditableColor(isValidSize(fieldSizeX.getText()) ? 0xFFFFFF : 0xFF4444);
            fieldSizeY.setEditableColor(isValidSize(fieldSizeY.getText()) ? 0xFFFFFF : 0xFF4444);
            fieldSizeZ.setEditableColor(isValidSize(fieldSizeZ.getText()) ? 0xFFFFFF : 0xFF4444);
            fieldColor.setEditableColor(isValidColor(fieldColor.getText()) ? 0xFFFFFF : 0xFF4444);
        }
    }

    private int parsePositiveInt(String s) {
        // Coupe la partie décimale si présente ("7.8" → "7")
        String trimmed = s.trim().split("[.,]")[0];
        int val = Integer.parseInt(trimmed);
        if (val <= 0) throw new IllegalArgumentException("Valeur doit être > 0");
        if (val > 64) throw new IllegalArgumentException("Valeur trop grande (max 64)");
        return val;
    }

    private boolean isValidSize(String s) {
        try { parsePositiveInt(s); return true; }
        catch (Exception e) { return false; }
    }

    private boolean isValidColor(String s) {
        try { Integer.parseInt(s.trim(), 16); return true; }
        catch (Exception e) { return false; }
    }

    private void openTestCases() {
        // Priorité aux données déjà scannées du BE (via SyncData)
        // Fallback sur le dernier scan en mémoire si le BE n'en a pas
        List<BlockPos> inj = !handler.injectors.isEmpty()
                ? handler.injectors
                : TeststoneClient.lastScannedInjectors;
        List<BlockPos> sen = !handler.sensors.isEmpty()
                ? handler.sensors
                : TeststoneClient.lastScannedSensors;

        var h = new TestCaseScreenHandler(0, client.player.getInventory(),
                new TestCaseScreenHandler.SyncData(
                        handler.benchPos,
                        inj,
                        sen,
                        handler.testCases
                ));
        client.setScreen(new TestCaseScreen(h, client.player.getInventory(), Text.literal("Cas de test")));
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        // Empty background
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
        int lx = 14;
        ctx.drawText(textRenderer, "TestBench", lx, 6,  0x404040, false);
        ctx.drawText(textRenderer, "Size X :", lx, 24, 0x404040, false);
        ctx.drawText(textRenderer, "Size Y :", lx, 48, 0x404040, false);
        ctx.drawText(textRenderer, "Size Z :", lx, 72, 0x404040, false);
        ctx.drawText(textRenderer, "Couleur (hex) :", lx, 96, 0x404040, false);
    }

    @Override
    public boolean shouldPause() { return false; }
}
