package org.BonneChaussure.teststone.gui;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import org.BonneChaussure.gui.RenameBlockScreenHandler;
import org.BonneChaussure.network.RenameBlockPacket;

public class RenameBlockScreen extends HandledScreen<RenameBlockScreenHandler> {

    private TextFieldWidget nameField;

    public RenameBlockScreen(RenameBlockScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth  = 200;
        this.backgroundHeight = 80;
    }

    @Override
    protected void init() {
        super.init();
        int x = (width  - backgroundWidth)  / 2;
        int y = (height - backgroundHeight) / 2;

        nameField = addDrawableChild(new TextFieldWidget(
                textRenderer, x + 10, y + 25, 180, 16, Text.literal("Nom")));
        nameField.setText(handler.currentName);
        nameField.setMaxLength(32);
        nameField.setFocused(true);  // focus visuel sur le champ
        setFocused(nameField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Sauvegarder"), b -> save())
                .dimensions(x + 55, y + 50, 90, 20).build());
    }

    private void save() {
        ClientPlayNetworking.send(new RenameBlockPacket(
                handler.blockPos, nameField.getText(), handler.isInjector));
        close();
    }

    // Valider avec Entrée
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) { // ENTER ou NUMPAD_ENTER
            save();
            return true;
        }

        if (keyCode == 256) {
            close();
            return true;
        }

        // Si le champ a le focus, il consomme la touche — ne pas propager au screen parent
        if (nameField.isFocused()) {
            return nameField.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {}

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
        String title = handler.isInjector ? "Renommer Injector" : "Renommer Sensor";
        ctx.drawText(textRenderer, title, 10, 10, 0x404040, false);
    }

    @Override public boolean shouldPause() { return false; }
}
