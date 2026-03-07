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
import org.BonneChaussure.network.RunTestsPacket;
import org.BonneChaussure.network.SaveTestCasesPacket;
import org.BonneChaussure.network.ScanBenchPacket;
import org.BonneChaussure.tests.TestCase;
import org.BonneChaussure.teststone.client.TeststoneClient;

import java.util.*;

public class TestCaseScreen extends HandledScreen<TestCaseScreenHandler> {

    private final List<BlockPos> injectors = new ArrayList<>();
    private final List<BlockPos> sensors   = new ArrayList<>();
    private final Map<BlockPos, String> injectorNames = new LinkedHashMap<>();
    private final Map<BlockPos, String> sensorNames   = new LinkedHashMap<>();

    private final List<TestCase> editableCases;
    private final Boolean[] caseResults;
    private int selectedCase = 0;
    private TextFieldWidget caseNameField;

    public TestCaseScreen(TestCaseScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth  = 340;
        this.backgroundHeight = 220;
        this.injectors.addAll(handler.injectors);
        this.sensors.addAll(handler.sensors);
        this.injectorNames.putAll(TeststoneClient.lastInjectorNames);
        this.sensorNames.putAll(TeststoneClient.lastSensorNames);
        this.editableCases = new ArrayList<>(handler.cases);
        if (editableCases.isEmpty()) addNewCase();
        this.caseResults = new Boolean[editableCases.size()];
    }

    // ── Cas de test ───────────────────────────────────────────────────────────

    private void addNewCase() {
        Map<BlockPos, Boolean> inj = new LinkedHashMap<>();
        injectors.forEach(p -> inj.put(p, false));
        Map<BlockPos, Boolean> sen = new LinkedHashMap<>();
        sensors.forEach(p -> sen.put(p, false));
        editableCases.add(new TestCase("Cas " + (editableCases.size() + 1), inj, sen));
    }

    private void commitCurrentName() {
        if (caseNameField == null || editableCases.isEmpty()) return;
        TestCase cur = editableCases.get(selectedCase);
        editableCases.set(selectedCase,
                new TestCase(caseNameField.getText(), cur.injectorValues(), cur.sensorExpected()));
    }

    private void moveCaseUp(int idx) {
        if (idx <= 0) return;
        commitCurrentName();
        Collections.swap(editableCases, idx, idx - 1);
        if (selectedCase == idx) selectedCase--;
        else if (selectedCase == idx - 1) selectedCase++;
        rebuildWidgets();
    }

    private void moveCaseDown(int idx) {
        if (idx >= editableCases.size() - 1) return;
        commitCurrentName();
        Collections.swap(editableCases, idx, idx + 1);
        if (selectedCase == idx) selectedCase++;
        else if (selectedCase == idx + 1) selectedCase--;
        rebuildWidgets();
    }

    private void deleteCase(int idx) {
        commitCurrentName();
        editableCases.remove(idx);
        if (editableCases.isEmpty()) addNewCase();
        if (selectedCase >= editableCases.size()) selectedCase = editableCases.size() - 1;
        rebuildWidgets();
    }

    // ── Injectors / Sensors ───────────────────────────────────────────────────

    private void moveInjectorUp(int idx) {
        if (idx <= 0) return;
        Collections.swap(injectors, idx, idx - 1);
        rebuildWidgets();
    }

    private void moveInjectorDown(int idx) {
        if (idx >= injectors.size() - 1) return;
        Collections.swap(injectors, idx, idx + 1);
        rebuildWidgets();
    }

    private void deleteInjector(int idx) {
        BlockPos removed = injectors.remove(idx);
        // Retire la clé de tous les cas
        for (int i = 0; i < editableCases.size(); i++) {
            TestCase tc = editableCases.get(i);
            Map<BlockPos, Boolean> newInj = new LinkedHashMap<>(tc.injectorValues());
            newInj.remove(removed);
            editableCases.set(i, new TestCase(tc.name(), newInj, tc.sensorExpected()));
        }
        rebuildWidgets();
    }

    private void moveSensorUp(int idx) {
        if (idx <= 0) return;
        Collections.swap(sensors, idx, idx - 1);
        rebuildWidgets();
    }

    private void moveSensorDown(int idx) {
        if (idx >= sensors.size() - 1) return;
        Collections.swap(sensors, idx, idx + 1);
        rebuildWidgets();
    }

    private void deleteSensor(int idx) {
        BlockPos removed = sensors.remove(idx);
        for (int i = 0; i < editableCases.size(); i++) {
            TestCase tc = editableCases.get(i);
            Map<BlockPos, Boolean> newSen = new LinkedHashMap<>(tc.sensorExpected());
            newSen.remove(removed);
            editableCases.set(i, new TestCase(tc.name(), tc.injectorValues(), newSen));
        }
        rebuildWidgets();
    }

    // ── Construction des widgets ──────────────────────────────────────────────

    @Override
    protected void init() {
        super.init();
        rebuildWidgets();
    }

    private void rebuildWidgets() {
        clearChildren();

        int x = (width  - backgroundWidth)  / 2;
        int y = (height - backgroundHeight) / 2;

        // ── Colonne gauche : liste des cas ────────────────────────────────────
        for (int i = 0; i < editableCases.size(); i++) {
            final int idx = i;

            // Couleur selon résultat
            int labelColor = (i >= caseResults.length || caseResults[i] == null) ? 0x404040
                    : caseResults[i] ? 0x00AA00 : 0xAA0000;

            String label = (i == selectedCase ? "▶ " : "  ") + editableCases.get(i).name();
            ButtonWidget btn = addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> {
                commitCurrentName();
                selectedCase = idx;
                rebuildWidgets();
            }).dimensions(x + 5, y + 20 + i * 22, 70, 18).build());
            btn.setMessage(Text.literal(label).styled(s -> s.withColor(labelColor)));

            // ▲
            addDrawableChild(ButtonWidget.builder(Text.literal("▲"), b -> moveCaseUp(idx))
                    .dimensions(x + 77, y + 20 + i * 22, 14, 18).build());
            // ▼
            addDrawableChild(ButtonWidget.builder(Text.literal("▼"), b -> moveCaseDown(idx))
                    .dimensions(x + 93, y + 20 + i * 22, 14, 18).build());
            // ✕ rouge
            ButtonWidget del = addDrawableChild(ButtonWidget.builder(Text.literal("✕"), b -> deleteCase(idx))
                    .dimensions(x + 109, y + 20 + i * 22, 14, 18).build());
            del.setMessage(Text.literal("✕").styled(s -> s.withColor(0xFF4444)));
        }

        // Bouton + Ajouter
        addDrawableChild(ButtonWidget.builder(Text.literal("+ Ajouter"), b -> {
            commitCurrentName();
            addNewCase();
            selectedCase = editableCases.size() - 1;
            rebuildWidgets();
        }).dimensions(x + 5, y + 20 + editableCases.size() * 22, 80, 18).build());

        // ── Colonne droite : édition du cas sélectionné ───────────────────────
        if (editableCases.isEmpty()) return;

        final int caseIdx = selectedCase;
        TestCase current = editableCases.get(caseIdx);

        // Nom du cas
        caseNameField = addDrawableChild(new TextFieldWidget(
                textRenderer, x + 130, y + 10, 120, 14, Text.literal("Nom")));
        caseNameField.setText(current.name());
        caseNameField.setMaxLength(32);

        // ── Injectors ─────────────────────────────────────────────────────────
        for (int i = 0; i < injectors.size(); i++) {
            final BlockPos p = injectors.get(i);
            final int label = i + 1;
            final int fi = i;
            boolean val = current.injectorValues().getOrDefault(p, false);

            String injDisplay = injectorNames.getOrDefault(p, "");
            if (injDisplay.isEmpty()) injDisplay = "Inj " + label;
            String injButtonLabel = injDisplay + " : " + (val ? "ON" : "OFF");

            // Bouton ON/OFF
            addDrawableChild(ButtonWidget.builder(
                    Text.literal(injButtonLabel), b -> { TestCase cur = editableCases.get(caseIdx);
                        Map<BlockPos, Boolean> newInj = new LinkedHashMap<>(cur.injectorValues());
                        newInj.put(p, !newInj.getOrDefault(p, false));
                        editableCases.set(caseIdx, new TestCase(caseNameField.getText(), newInj, cur.sensorExpected()));
                        rebuildWidgets(); }
            ).dimensions(x + 130, y + 32 + i * 20, 70, 16).build());

            // ▲ ▼ ✕
            addDrawableChild(ButtonWidget.builder(Text.literal("▲"), b -> moveInjectorUp(fi))
                    .dimensions(x + 202, y + 32 + i * 20, 14, 16).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("▼"), b -> moveInjectorDown(fi))
                    .dimensions(x + 218, y + 32 + i * 20, 14, 16).build());
            ButtonWidget del = addDrawableChild(ButtonWidget.builder(Text.literal("✕"), b -> deleteInjector(fi))
                    .dimensions(x + 234, y + 32 + i * 20, 14, 16).build());
            del.setMessage(Text.literal("✕").styled(s -> s.withColor(0xFF4444)));
        }

        // ── Sensors ───────────────────────────────────────────────────────────
        for (int i = 0; i < sensors.size(); i++) {
            final BlockPos p = sensors.get(i);
            final int label = i + 1;
            final int fi = i;
            boolean val = current.sensorExpected().getOrDefault(p, false);

            String senDisplay = sensorNames.getOrDefault(p, "");
            if (senDisplay.isEmpty()) senDisplay = "Sen " + label;
            String senButtonLabel = senDisplay + " : " + (val ? "ON" : "OFF");

            // Bouton ON/OFF
            addDrawableChild(ButtonWidget.builder(
                    Text.literal(senButtonLabel), b -> { TestCase cur = editableCases.get(caseIdx);
                        Map<BlockPos, Boolean> newSen = new LinkedHashMap<>(cur.sensorExpected());
                        newSen.put(p, !newSen.getOrDefault(p, false));
                        editableCases.set(caseIdx, new TestCase(caseNameField.getText(), cur.injectorValues(), newSen));
                        rebuildWidgets(); }
            ).dimensions(x + 255, y + 32 + i * 20, 70, 16).build());

            // ▲ ▼ ✕
            addDrawableChild(ButtonWidget.builder(Text.literal("▲"), b -> moveSensorUp(fi))
                    .dimensions(x + 327, y + 32 + i * 20, 14, 16).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("▼"), b -> moveSensorDown(fi))
                    .dimensions(x + 343, y + 32 + i * 20, 14, 16).build());
            ButtonWidget del = addDrawableChild(ButtonWidget.builder(Text.literal("✕"), b -> deleteSensor(fi))
                    .dimensions(x + 359, y + 32 + i * 20, 14, 16).build());
            del.setMessage(Text.literal("✕").styled(s -> s.withColor(0xFF4444)));
        }

        // ── Boutons du bas ────────────────────────────────────────────────────
        addDrawableChild(ButtonWidget.builder(Text.literal("Scanner"), b -> scan())
                .dimensions(x + 5, y + backgroundHeight - 25, 70, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("▶ Lancer"), b -> runTests())
                .dimensions(x + backgroundWidth - 170, y + backgroundHeight - 25, 80, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Sauvegarder"), b -> save())
                .dimensions(x + backgroundWidth - 85, y + backgroundHeight - 25, 80, 20).build());
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void save() {
        commitCurrentName();
        ClientPlayNetworking.send(new SaveTestCasesPacket(
                handler.bench, editableCases, injectors, sensors));
        close();
    }

    private void scan() {
        ClientPlayNetworking.send(new ScanBenchPacket(handler.bench));
    }

    private void runTests() {
        commitCurrentName();
        ClientPlayNetworking.send(new SaveTestCasesPacket(
                handler.bench, editableCases, injectors, sensors));
        ClientPlayNetworking.send(new RunTestsPacket(handler.bench));
        Arrays.fill(caseResults, null);
        rebuildWidgets();
    }

    public void onScanReceived(List<BlockPos> newInjectors, Map<BlockPos, String> newInjectorNames,
                               List<BlockPos> newSensors,   Map<BlockPos, String> newSensorNames) {
        injectors.clear();     injectors.addAll(newInjectors);
        sensors.clear();       sensors.addAll(newSensors);
        injectorNames.clear(); injectorNames.putAll(newInjectorNames);
        sensorNames.clear();   sensorNames.putAll(newSensorNames);

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

    public void onCaseResult(int idx, boolean pass) {
        if (idx < caseResults.length) caseResults[idx] = pass;
        rebuildWidgets();
    }

    public void onAllResults(boolean[] results) {
        for (int i = 0; i < results.length && i < caseResults.length; i++)
            caseResults[i] = results[i];
        rebuildWidgets();
    }

    // ── Rendu ─────────────────────────────────────────────────────────────────

    @Override protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {}
    @Override public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
        ctx.drawText(textRenderer, "Cas de test", 5,   6, 0x404040, false);
        ctx.drawText(textRenderer, "Injectors",  130, 22, 0x404040, false);
        ctx.drawText(textRenderer, "Sensors",    255, 22, 0x404040, false);
    }

    @Override public boolean shouldPause() { return false; }
}
