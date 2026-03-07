package org.BonneChaussure.teststone.gui;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.BonneChaussure.gui.TestCaseScreenHandler;
import org.BonneChaussure.network.SaveTestCasesPacket;
import org.BonneChaussure.network.ScanBenchPacket;
import org.BonneChaussure.tests.TestCase;

import java.util.*;

public class TestCaseScreen extends HandledScreen<TestCaseScreenHandler> {

    // Liste de référence — partagée par tous les cas
    private final List<BlockPos> injectors = new ArrayList<>();
    private final List<BlockPos> sensors   = new ArrayList<>();

    // État local des cas en cours d'édition
    private final List<TestCase> editableCases;
    private int selectedCase = 0;
    private TextFieldWidget caseNameField;

    public TestCaseScreen(TestCaseScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth  = 300;
        this.backgroundHeight = 200;
        this.injectors.addAll(handler.injectors);
        this.sensors.addAll(handler.sensors);
        this.editableCases = new ArrayList<>(handler.cases);
        if (editableCases.isEmpty()) addNewCase();
    }

    private void addNewCase() {
        // Tous les cas ont exactement les mêmes clés — les valeurs scannées
        Map<BlockPos, Boolean> inj = new LinkedHashMap<>();
        injectors.forEach(p -> inj.put(p, false));
        Map<BlockPos, Boolean> sen = new LinkedHashMap<>();
        sensors.forEach(p -> sen.put(p, false));
        editableCases.add(new TestCase("Cas " + (editableCases.size() + 1), inj, sen));
    }

    // Commit le nom du champ texte dans le cas courant SANS rebuildWidgets
    private void commitCurrentName() {
        if (caseNameField == null || editableCases.isEmpty()) return;
        TestCase cur = editableCases.get(selectedCase);
        editableCases.set(selectedCase,
                new TestCase(caseNameField.getText(), cur.injectorValues(), cur.sensorExpected()));
    }

    @Override
    protected void init() {
        super.init();
        rebuildWidgets();
    }

    private void rebuildWidgets() {
        clearChildren();

        int x = (width  - backgroundWidth)  / 2;
        int y = (height - backgroundHeight) / 2;

        // ── Colonne gauche : liste des cas ─────────────────────────────────
        for (int i = 0; i < editableCases.size(); i++) {
            final int idx = i;
            String label = (i == selectedCase ? "▶ " : "  ") + editableCases.get(i).name();
            addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> {
                commitCurrentName();
                selectedCase = idx;
                rebuildWidgets();
            }).dimensions(x + 5, y + 20 + i * 22, 80, 18).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("+ Ajouter"), b -> {
            commitCurrentName();
            addNewCase();
            selectedCase = editableCases.size() - 1;
            rebuildWidgets();
        }).dimensions(x + 5, y + 20 + editableCases.size() * 22, 80, 18).build());

        // ── Colonne droite : édition du cas sélectionné ────────────────────
        if (editableCases.isEmpty()) return;

        // Snapshot immuable du cas courant pour les lambdas
        final int caseIdx = selectedCase;
        TestCase current = editableCases.get(caseIdx);

        // Nom
        caseNameField = addDrawableChild(new TextFieldWidget(
                textRenderer, x + 95, y + 10, 120, 14, Text.literal("Nom")));
        caseNameField.setText(current.name());
        caseNameField.setMaxLength(32);

        // Injectors — on itère sur la liste de référence, pas sur les clés de la map
        for (int i = 0; i < injectors.size(); i++) {
            final BlockPos p = injectors.get(i);
            final int label = i + 1;
            boolean val = current.injectorValues().getOrDefault(p, false);

            addDrawableChild(ButtonWidget.builder(
                    Text.literal("Inj " + label + " : " + (val ? "ON" : "OFF")),
                    b -> {
                        TestCase cur = editableCases.get(caseIdx);
                        Map<BlockPos, Boolean> newInj = new LinkedHashMap<>(cur.injectorValues());
                        newInj.put(p, !newInj.getOrDefault(p, false));
                        editableCases.set(caseIdx,
                                new TestCase(caseNameField.getText(), newInj, cur.sensorExpected()));
                        rebuildWidgets();
                    }
            ).dimensions(x + 95, y + 32 + i * 20, 90, 16).build());
        }

        // Sensors — même approche
        for (int i = 0; i < sensors.size(); i++) {
            final BlockPos p = sensors.get(i);
            final int label = i + 1;
            boolean val = current.sensorExpected().getOrDefault(p, false);

            addDrawableChild(ButtonWidget.builder(
                    Text.literal("Sen " + label + " : " + (val ? "ON" : "OFF")),
                    b -> {
                        TestCase cur = editableCases.get(caseIdx);
                        Map<BlockPos, Boolean> newSen = new LinkedHashMap<>(cur.sensorExpected());
                        newSen.put(p, !newSen.getOrDefault(p, false));
                        editableCases.set(caseIdx,
                                new TestCase(caseNameField.getText(), cur.injectorValues(), newSen));
                        rebuildWidgets();
                    }
            ).dimensions(x + 195, y + 32 + i * 20, 90, 16).build());
        }

        // Boutons du bas
        addDrawableChild(ButtonWidget.builder(Text.literal("Scanner"), b -> scan()
        ).dimensions(x + 5, y + backgroundHeight - 25, 80, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Sauvegarder"), b -> save())
                .dimensions(x + backgroundWidth - 95, y + backgroundHeight - 25, 90, 20).build());
    }

    private void save() {
        commitCurrentName();
        ClientPlayNetworking.send(new SaveTestCasesPacket(handler.bench, editableCases));
        close();
    }

    private void scan(){
        ClientPlayNetworking.send(new ScanBenchPacket(handler.bench));
    }

    public void onScanReceived(List<BlockPos> newInjectors, List<BlockPos> newSensors) {
        // Met à jour les listes de référence
        injectors.clear();
        injectors.addAll(newInjectors);
        sensors.clear();
        sensors.addAll(newSensors);

        // Synchronise les maps de chaque cas existant avec les nouveaux blocs
        for (int i = 0; i < editableCases.size(); i++) {
            TestCase tc = editableCases.get(i);
            Map<BlockPos, Boolean> newInj = new LinkedHashMap<>();
            newInjectors.forEach(p -> newInj.put(p, tc.injectorValues().getOrDefault(p, false)));
            Map<BlockPos, Boolean> newSen = new LinkedHashMap<>();
            newSensors.forEach(p -> newSen.put(p, tc.sensorExpected().getOrDefault(p, false)));
            editableCases.set(i, new TestCase(tc.name(), newInj, newSen));
        }

        rebuildWidgets();
    }

    @Override protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {}
    @Override public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
        ctx.drawText(textRenderer, "Cas de test", 5,   6,  0x404040, false);
        ctx.drawText(textRenderer, "Injectors",   95,  22, 0x404040, false);
        ctx.drawText(textRenderer, "Sensors",     195, 22, 0x404040, false);
    }

    @Override public boolean shouldPause() { return false; }
}
