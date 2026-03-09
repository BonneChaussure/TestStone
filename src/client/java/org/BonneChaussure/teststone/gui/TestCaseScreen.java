package org.BonneChaussure.teststone.gui;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.BonneChaussure.blocks.InjectorBlock;
import org.BonneChaussure.blocks.ModBlocks;
import org.BonneChaussure.blocks.SensorBlock;
import org.BonneChaussure.gui.TestCaseScreenHandler;
import org.BonneChaussure.network.RunSingleTestPacket;
import org.BonneChaussure.network.RunTestsPacket;
import org.BonneChaussure.network.SaveTestCasesPacket;
import org.BonneChaussure.network.ScanBenchPacket;
import org.BonneChaussure.tests.TestCase;

import java.util.*;

public class TestCaseScreen extends HandledScreen<TestCaseScreenHandler> {

    private final List<BlockPos>         injectors     = new ArrayList<>();
    private final List<BlockPos>         sensors       = new ArrayList<>();
    private final Map<BlockPos, String>  injectorNames = new LinkedHashMap<>();
    private final Map<BlockPos, String>  sensorNames   = new LinkedHashMap<>();
    private final List<TestCase>         editableCases;
    private final List<Boolean>          caseResults   = new ArrayList<>();
    private int selectedCase = 0;
    private TextFieldWidget caseNameField;

    private static final int ROW_H     = 18;
    private static final int LIST_H    = 140;  // réduit pour laisser place au champ nom
    private static final int BOX_Y_OFF = 22;
    private static final int CX = 5,   CW = 128;
    private static final int IX = 143, IW = 118;
    private static final int SX = 271, SW = 118;
    private static final int SCROLL_W  = 6;

    private int scrollCase = 0, scrollInj = 0, scrollSen = 0;

    private enum DragTarget { NONE, CASE, INJ, SEN }
    private DragTarget dragging       = DragTarget.NONE;
    private int        dragStartMouseY;
    private int        dragStartScroll;

    public TestCaseScreen(TestCaseScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth  = 400;
        this.backgroundHeight = 210;
        injectors.addAll(handler.injectors);
        sensors.addAll(handler.sensors);
        injectorNames.putAll(handler.injectorNames);
        sensorNames.putAll(handler.sensorNames);
        editableCases = new ArrayList<>(handler.cases);
        if (editableCases.isEmpty()) addNewCase();
        selectedCase = Math.min(handler.selectedCaseIndex, editableCases.size() - 1);
        for (int i = 0; i < editableCases.size(); i++) caseResults.add(null);
    }

    // ── Scroll helpers ────────────────────────────────────────────────────────

    private int caseTotalH() { return (editableCases.size() + 1) * ROW_H; }
    private int injTotalH()  { return Math.max(ROW_H, injectors.size() * ROW_H); }
    private int senTotalH()  { return Math.max(ROW_H, sensors.size()   * ROW_H); }
    private int maxScrollCase() { return Math.max(0, caseTotalH() - LIST_H); }
    private int maxScrollInj()  { return Math.max(0, injTotalH()  - LIST_H); }
    private int maxScrollSen()  { return Math.max(0, senTotalH()  - LIST_H); }

    private void clampScrolls() {
        scrollCase = Math.max(0, Math.min(scrollCase, maxScrollCase()));
        scrollInj  = Math.max(0, Math.min(scrollInj,  maxScrollInj()));
        scrollSen  = Math.max(0, Math.min(scrollSen,  maxScrollSen()));
    }
    private int thumbH(int maxScroll, int totalH) {
        if (totalH <= 0 || maxScroll <= 0) return LIST_H;
        return Math.max(16, LIST_H * LIST_H / totalH);
    }
    private int thumbY(int scrollVal, int maxScroll, int totalH) {
        if (maxScroll <= 0) return 0;
        return (int)((float) scrollVal / maxScroll * (LIST_H - thumbH(maxScroll, totalH)));
    }

    // ── Logique cas ───────────────────────────────────────────────────────────

    private void addNewCase() {
        Map<BlockPos, Boolean> inj = new LinkedHashMap<>();
        injectors.forEach(p -> inj.put(p, false));
        Map<BlockPos, Boolean> sen = new LinkedHashMap<>();
        sensors.forEach(p -> sen.put(p, false));
        editableCases.add(new TestCase("Case " + (editableCases.size() + 1), inj, sen));
        caseResults.add(null);
    }
    private void commitCurrentName() {
        if (caseNameField == null || editableCases.isEmpty()) return;
        TestCase cur = editableCases.get(selectedCase);
        editableCases.set(selectedCase,
                new TestCase(caseNameField.getText(), cur.injectorValues(), cur.sensorExpected()));
    }
    private void moveCaseUp(int idx) {
        if (idx <= 0) return; commitCurrentName();
        Collections.swap(editableCases, idx, idx-1); Collections.swap(caseResults, idx, idx-1);
        if (selectedCase==idx) selectedCase--; else if (selectedCase==idx-1) selectedCase++;
        initWidgets(); autoSave();
    }
    private void moveCaseDown(int idx) {
        if (idx >= editableCases.size()-1) return; commitCurrentName();
        Collections.swap(editableCases, idx, idx+1); Collections.swap(caseResults, idx, idx+1);
        if (selectedCase==idx) selectedCase++; else if (selectedCase==idx+1) selectedCase--;
        initWidgets(); autoSave();
    }
    private void deleteCase(int idx) {
        commitCurrentName(); editableCases.remove(idx);
        if (idx < caseResults.size()) caseResults.remove(idx);
        if (editableCases.isEmpty()) addNewCase();
        if (selectedCase >= editableCases.size()) selectedCase = editableCases.size()-1;
        initWidgets(); autoSave();
    }

    // ── Logique injectors/sensors ─────────────────────────────────────────────

    private void moveInjectorUp(int idx)   { if (idx>0) { Collections.swap(injectors,idx,idx-1); initWidgets(); autoSave(); } }
    private void moveInjectorDown(int idx) { if (idx<injectors.size()-1) { Collections.swap(injectors,idx,idx+1); initWidgets(); autoSave(); } }
    private void deleteInjector(int idx) {
        BlockPos removed = injectors.remove(idx);
        for (int i=0;i<editableCases.size();i++) { TestCase tc=editableCases.get(i); Map<BlockPos,Boolean> m=new LinkedHashMap<>(tc.injectorValues()); m.remove(removed); editableCases.set(i,new TestCase(tc.name(),m,tc.sensorExpected())); }
        initWidgets(); autoSave();
    }
    private void moveSensorUp(int idx)   { if (idx>0) { Collections.swap(sensors,idx,idx-1); initWidgets(); autoSave(); } }
    private void moveSensorDown(int idx) { if (idx<sensors.size()-1) { Collections.swap(sensors,idx,idx+1); initWidgets(); autoSave(); } }
    private void deleteSensor(int idx) {
        BlockPos removed = sensors.remove(idx);
        for (int i=0;i<editableCases.size();i++) { TestCase tc=editableCases.get(i); Map<BlockPos,Boolean> m=new LinkedHashMap<>(tc.sensorExpected()); m.remove(removed); editableCases.set(i,new TestCase(tc.name(),tc.injectorValues(),m)); }
        initWidgets(); autoSave();
    }
    private void toggleInjector(int caseIdx, BlockPos p) {
        commitCurrentName(); TestCase cur=editableCases.get(caseIdx);
        Map<BlockPos,Boolean> m=new LinkedHashMap<>(cur.injectorValues()); m.put(p,!m.getOrDefault(p,false));
        editableCases.set(caseIdx,new TestCase(cur.name(),m,cur.sensorExpected())); initWidgets(); autoSave();
    }
    private void toggleSensor(int caseIdx, BlockPos p) {
        commitCurrentName(); TestCase cur=editableCases.get(caseIdx);
        Map<BlockPos,Boolean> m=new LinkedHashMap<>(cur.sensorExpected()); m.put(p,!m.getOrDefault(p,false));
        editableCases.set(caseIdx,new TestCase(cur.name(),cur.injectorValues(),m)); initWidgets(); autoSave();
    }

    // ── Widgets fixes ─────────────────────────────────────────────────────────

    @Override
    protected void init() { super.init(); initWidgets(); }

    private void initWidgets() {
        clearChildren(); clampScrolls();
        int gx=(width-backgroundWidth)/2, gy=(height-backgroundHeight)/2;
        if (editableCases.isEmpty()) return;

        // Champ nom — placé entre les boîtes et les boutons du bas
        // Y = gy + BOX_Y_OFF + LIST_H + 4  (juste sous les boîtes)
        int nameY = gy + BOX_Y_OFF + LIST_H + 5;
        caseNameField = addDrawableChild(new TextFieldWidget(
                textRenderer, gx + 95, nameY, 160, 14, Text.literal("Nom")));
        caseNameField.setText(editableCases.get(selectedCase).name());
        caseNameField.setMaxLength(32);
        caseNameField.setChangedListener(text -> {
            if (!editableCases.isEmpty()) {
                TestCase cur = editableCases.get(selectedCase);
                editableCases.set(selectedCase, new TestCase(text, cur.injectorValues(), cur.sensorExpected()));
                autoSave();
            }
        });

        // Boutons du bas
        int btnY = gy + backgroundHeight - 22;
        addDrawableChild(ButtonWidget.builder(Text.literal("Update blocks"), b -> scan())
                .dimensions(gx+5, btnY, 80, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("▶ Start tests"), b -> runTests())
                .dimensions(gx+backgroundWidth-170, btnY, 80, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset"), b -> clearPreview())
                .dimensions(gx+backgroundWidth-75, btnY, 70, 18).build());

        applyPreview(editableCases.get(selectedCase));
    }

    // ── Rendu ─────────────────────────────────────────────────────────────────

    @Override public void renderBackground(DrawContext ctx, int mx, int my, float delta) {}

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        // Réapplique le preview à chaque frame — corrige le cas où un clic droit
        // sur un bloc (ex: RenameBlockScreen) remet son état visuel à zéro
        if (!editableCases.isEmpty()) applyPreview(editableCases.get(selectedCase));
        int gx=(width-backgroundWidth)/2, gy=(height-backgroundHeight)/2;
        ctx.fill(gx, gy, gx+backgroundWidth, gy+backgroundHeight, 0xCC1A1A1A);

        super.render(ctx, mx, my, delta); // widgets fixes

        // Titres
        ctx.drawText(textRenderer,"Test cases", gx+CX+2,   gy+BOX_Y_OFF-9, 0xAAAAAA, false);
        ctx.drawText(textRenderer,"Injectors",   gx+IX+2,   gy+BOX_Y_OFF-9, 0xAAAAAA, false);
        ctx.drawText(textRenderer,"Sensors",     gx+SX+2,   gy+BOX_Y_OFF-9, 0xAAAAAA, false);

        // Label du champ nom
        int nameY = gy + BOX_Y_OFF + LIST_H + 5;
        ctx.drawText(textRenderer, "Test case name :", gx + 5, nameY + 3, 0xAAAAAA, false);

        int boxTop = gy + BOX_Y_OFF;
        drawBox(ctx, gx+CX, boxTop, CW);
        drawBox(ctx, gx+IX, boxTop, IW);
        drawBox(ctx, gx+SX, boxTop, SW);

        renderCaseList(ctx, gx+CX, boxTop, mx, my);
        renderInjList( ctx, gx+IX, boxTop, mx, my);
        renderSenList( ctx, gx+SX, boxTop, mx, my);

        drawScrollBar(ctx, gx+CX, boxTop, CW, scrollCase, maxScrollCase(), caseTotalH());
        drawScrollBar(ctx, gx+IX, boxTop, IW, scrollInj,  maxScrollInj(),  injTotalH());
        drawScrollBar(ctx, gx+SX, boxTop, SW, scrollSen,  maxScrollSen(),  senTotalH());
    }

    private void renderCaseList(DrawContext ctx, int bx, int by, int mx, int my) {
        ctx.enableScissor(bx, by, bx+CW-SCROLL_W, by+LIST_H);
        for (int i=0; i<editableCases.size(); i++) {
            int iy = by + i*ROW_H - scrollCase;
            if (iy+ROW_H<=by || iy>=by+LIST_H) continue;
            boolean sel = i==selectedCase;
            Boolean res = i<caseResults.size() ? caseResults.get(i) : null;
            ctx.fill(bx, iy, bx+CW-SCROLL_W, iy+ROW_H-1, sel ? 0x88336699 : 0x55000000);
            int col = res==null ? (sel?0xFFFFFF:0xCCCCCC) : res?0x55FF55:0xFF5555;
            ctx.drawText(textRenderer, (sel?"▶ ":"  ")+editableCases.get(i).name(), bx+3, iy+5, col, false);
            int ix = bx+CW-SCROLL_W-48;
            drawIcon(ctx,"▲",ix,   iy,mx,my,0xAAAAAA);
            drawIcon(ctx,"▼",ix+12,iy,mx,my,0xAAAAAA);
            drawIcon(ctx,"✕",ix+24,iy,mx,my,0xFF5555);
            drawIcon(ctx,"▶",ix+36,iy,mx,my,0x55FF55);
        }
        int addY = by + editableCases.size()*ROW_H - scrollCase;
        if (addY>=by && addY+ROW_H<=by+LIST_H) {
            ctx.fill(bx, addY, bx+CW-SCROLL_W, addY+ROW_H-1, 0x55003300);
            ctx.drawText(textRenderer,"+ Add", bx+3, addY+5, 0x55FF55, false);
        }
        ctx.disableScissor();
    }

    private void renderInjList(DrawContext ctx, int bx, int by, int mx, int my) {
        ctx.enableScissor(bx, by, bx+IW-SCROLL_W, by+LIST_H);
        TestCase cur = editableCases.get(selectedCase);
        for (int i=0; i<injectors.size(); i++) {
            BlockPos p = injectors.get(i);
            int iy = by + i*ROW_H - scrollInj;
            if (iy+ROW_H<=by || iy>=by+LIST_H) continue;
            boolean val = cur.injectorValues().getOrDefault(p, false);
            String name = injectorNames.getOrDefault(p,""); if (name.isEmpty()) name="Inj "+(i+1);
            ctx.fill(bx, iy, bx+IW-SCROLL_W, iy+ROW_H-1, val?0x6600AA00:0x55000000);
            ctx.drawText(textRenderer, name+" : "+(val?"ON":"OFF"), bx+3, iy+5, val?0x55FF55:0xCCCCCC, false);
            int ix=bx+IW-SCROLL_W-36;
            drawIcon(ctx,"▲",ix,   iy,mx,my,0xAAAAAA);
            drawIcon(ctx,"▼",ix+12,iy,mx,my,0xAAAAAA);
            drawIcon(ctx,"✕",ix+24,iy,mx,my,0xFF5555);
        }
        ctx.disableScissor();
    }

    private void renderSenList(DrawContext ctx, int bx, int by, int mx, int my) {
        ctx.enableScissor(bx, by, bx+SW-SCROLL_W, by+LIST_H);
        TestCase cur = editableCases.get(selectedCase);
        for (int i=0; i<sensors.size(); i++) {
            BlockPos p = sensors.get(i);
            int iy = by + i*ROW_H - scrollSen;
            if (iy+ROW_H<=by || iy>=by+LIST_H) continue;
            boolean val = cur.sensorExpected().getOrDefault(p, false);
            String name = sensorNames.getOrDefault(p,""); if (name.isEmpty()) name="Sen "+(i+1);
            ctx.fill(bx, iy, bx+SW-SCROLL_W, iy+ROW_H-1, val?0x6600AA00:0x55000000);
            ctx.drawText(textRenderer, name+" : "+(val?"ON":"OFF"), bx+3, iy+5, val?0x55FF55:0xCCCCCC, false);
            int ix=bx+SW-SCROLL_W-36;
            drawIcon(ctx,"▲",ix,   iy,mx,my,0xAAAAAA);
            drawIcon(ctx,"▼",ix+12,iy,mx,my,0xAAAAAA);
            drawIcon(ctx,"✕",ix+24,iy,mx,my,0xFF5555);
        }
        ctx.disableScissor();
    }

    private void drawIcon(DrawContext ctx, String icon, int x, int y, int mx, int my, int color) {
        boolean hover = mx>=x && mx<x+11 && my>=y && my<y+ROW_H;
        if (hover) ctx.fill(x, y, x+11, y+ROW_H-1, 0x44FFFFFF);
        ctx.drawText(textRenderer, icon, x+2, y+5, color, false);
    }

    private void drawBox(DrawContext ctx, int bx, int by, int bw) {
        ctx.fill(bx, by, bx+bw, by+LIST_H, 0x55000000);
        ctx.fill(bx-1, by-1, bx+bw+1, by,             0xFF555555);
        ctx.fill(bx-1, by+LIST_H, bx+bw+1, by+LIST_H+1, 0xFF555555);
        ctx.fill(bx-1, by, bx, by+LIST_H,               0xFF555555);
        ctx.fill(bx+bw, by, bx+bw+1, by+LIST_H,         0xFF555555);
    }

    private void drawScrollBar(DrawContext ctx, int bx, int by, int bw,
                               int scrollVal, int maxScroll, int totalH) {
        int tx = bx+bw-SCROLL_W;
        ctx.fill(tx, by, tx+SCROLL_W, by+LIST_H, 0xFF222222);
        if (maxScroll<=0) { ctx.fill(tx+1, by+1, tx+SCROLL_W-1, by+LIST_H-1, 0xFF444444); return; }
        int th = thumbH(maxScroll, totalH);
        int ty = by + thumbY(scrollVal, maxScroll, totalH);
        ctx.fill(tx+1, ty, tx+SCROLL_W-1, ty+th, 0xFFAAAAAA);
    }

    // ── Clics ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int gx=(width-backgroundWidth)/2, gy=(height-backgroundHeight)/2;
        int boxTop = gy+BOX_Y_OFF;

        DragTarget t = hitScrollBar(mx, my, gx, boxTop);
        if (t!=DragTarget.NONE && btn==0) {
            dragging=t; dragStartMouseY=(int)my; dragStartScroll=currentScroll(t); return true;
        }
        if (btn==0) {
            if (clickInBox(mx,my,gx+CX,boxTop,CW)) return handleCaseClick(mx,my,gx+CX,boxTop);
            if (clickInBox(mx,my,gx+IX,boxTop,IW)) return handleInjClick(mx,my,gx+IX,boxTop);
            if (clickInBox(mx,my,gx+SX,boxTop,SW)) return handleSenClick(mx,my,gx+SX,boxTop);
        }
        return super.mouseClicked(mx, my, btn);
    }

    private boolean handleCaseClick(double mx, double my, int bx, int by) {
        for (int i=0; i<editableCases.size(); i++) {
            int iy = by+i*ROW_H-scrollCase;
            if (my<iy||my>=iy+ROW_H) continue;
            int ix=bx+CW-SCROLL_W-48;
            if (mx>=ix    &&mx<ix+12) { moveCaseUp(i);    return true; }
            if (mx>=ix+12 &&mx<ix+24) { moveCaseDown(i);  return true; }
            if (mx>=ix+24 &&mx<ix+36) { deleteCase(i);    return true; }
            if (mx>=ix+36 &&mx<ix+48) { runSingleTest(i); return true; }
            commitCurrentName(); selectedCase=i; initWidgets(); autoSave(); return true;
        }
        int addY = by+editableCases.size()*ROW_H-scrollCase;
        if (my>=addY&&my<addY+ROW_H) {
            commitCurrentName(); addNewCase(); selectedCase=editableCases.size()-1;
            scrollCase=maxScrollCase(); initWidgets(); autoSave(); return true;
        }
        return false;
    }

    private boolean handleInjClick(double mx, double my, int bx, int by) {
        for (int i=0; i<injectors.size(); i++) {
            int iy = by+i*ROW_H-scrollInj;
            if (my<iy||my>=iy+ROW_H) continue;
            int ix=bx+IW-SCROLL_W-36;
            if (mx>=ix    &&mx<ix+12) { moveInjectorUp(i);   return true; }
            if (mx>=ix+12 &&mx<ix+24) { moveInjectorDown(i); return true; }
            if (mx>=ix+24 &&mx<ix+36) { deleteInjector(i);   return true; }
            toggleInjector(selectedCase, injectors.get(i)); return true;
        }
        return false;
    }

    private boolean handleSenClick(double mx, double my, int bx, int by) {
        for (int i=0; i<sensors.size(); i++) {
            int iy = by+i*ROW_H-scrollSen;
            if (my<iy||my>=iy+ROW_H) continue;
            int ix=bx+SW-SCROLL_W-36;
            if (mx>=ix    &&mx<ix+12) { moveSensorUp(i);   return true; }
            if (mx>=ix+12 &&mx<ix+24) { moveSensorDown(i); return true; }
            if (mx>=ix+24 &&mx<ix+36) { deleteSensor(i);   return true; }
            toggleSensor(selectedCase, sensors.get(i)); return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
        int gx=(width-backgroundWidth)/2, gy=(height-backgroundHeight)/2;
        int boxTop=gy+BOX_Y_OFF, delta=(int)(-vAmt*ROW_H);
        if (clickInBox(mx,my,gx+CX,boxTop,CW)) { scrollCase=Math.max(0,Math.min(scrollCase+delta,maxScrollCase())); initWidgets(); return true; }
        if (clickInBox(mx,my,gx+IX,boxTop,IW)) { scrollInj =Math.max(0,Math.min(scrollInj +delta,maxScrollInj())); initWidgets(); return true; }
        if (clickInBox(mx,my,gx+SX,boxTop,SW)) { scrollSen =Math.max(0,Math.min(scrollSen +delta,maxScrollSen())); initWidgets(); return true; }
        return super.mouseScrolled(mx, my, hAmt, vAmt);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (btn==0 && dragging!=DragTarget.NONE) {
            int diffY=(int)my-dragStartMouseY, maxS=maxScrollForTarget(dragging);
            int th=thumbH(maxS,totalHForTarget(dragging)), trackH=LIST_H-th;
            if (trackH>0) { setScroll(dragging, Math.max(0,Math.min(dragStartScroll+(int)((float)diffY/trackH*maxS),maxS))); initWidgets(); }
            return true;
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (btn==0) dragging=DragTarget.NONE;
        return super.mouseReleased(mx, my, btn);
    }

    private boolean clickInBox(double mx,double my,int bx,int by,int bw) { return mx>=bx&&mx<bx+bw&&my>=by&&my<by+LIST_H; }
    private DragTarget hitScrollBar(double mx,double my,int gx,int boxTop) {
        if (onScrollBar(mx,my,gx+CX,boxTop,CW)) return DragTarget.CASE;
        if (onScrollBar(mx,my,gx+IX,boxTop,IW)) return DragTarget.INJ;
        if (onScrollBar(mx,my,gx+SX,boxTop,SW)) return DragTarget.SEN;
        return DragTarget.NONE;
    }
    private boolean onScrollBar(double mx,double my,int bx,int by,int bw) { return mx>=bx+bw-SCROLL_W&&mx<bx+bw&&my>=by&&my<by+LIST_H; }
    private int currentScroll(DragTarget t) { return switch(t){case CASE->scrollCase;case INJ->scrollInj;case SEN->scrollSen;default->0;}; }
    private int maxScrollForTarget(DragTarget t) { return switch(t){case CASE->maxScrollCase();case INJ->maxScrollInj();case SEN->maxScrollSen();default->0;}; }
    private int totalHForTarget(DragTarget t) { return switch(t){case CASE->caseTotalH();case INJ->injTotalH();case SEN->senTotalH();default->1;}; }
    private void setScroll(DragTarget t,int v) { switch(t){case CASE->scrollCase=v;case INJ->scrollInj=v;case SEN->scrollSen=v;} }

    // ── Réseau ────────────────────────────────────────────────────────────────

    /** Envoie l'état courant au serveur sans fermer l'écran. */
    private void autoSave() {
        ClientPlayNetworking.send(new SaveTestCasesPacket(handler.bench, editableCases, injectors, sensors, selectedCase));
    }

    private void scan() { ClientPlayNetworking.send(new ScanBenchPacket(handler.bench)); }

    private void runTests() {
        commitCurrentName();
        autoSave();
        ClientPlayNetworking.send(new RunTestsPacket(handler.bench));
        Collections.fill(caseResults, null); initWidgets();
    }
    private void runSingleTest(int caseIdx) {
        commitCurrentName();
        selectedCase = caseIdx;
        autoSave();
        ClientPlayNetworking.send(new RunSingleTestPacket(handler.bench, caseIdx));
        if (caseIdx < caseResults.size()) caseResults.set(caseIdx, null); initWidgets();
    }

    public void onScanReceived(List<BlockPos> newInj,Map<BlockPos,String> newInjNames,
                               List<BlockPos> newSen,Map<BlockPos,String> newSenNames) {
        injectors.clear(); injectors.addAll(newInj);
        sensors.clear();   sensors.addAll(newSen);
        injectorNames.clear(); injectorNames.putAll(newInjNames);
        sensorNames.clear();   sensorNames.putAll(newSenNames);
        for (int i=0;i<editableCases.size();i++) {
            TestCase tc=editableCases.get(i);
            Map<BlockPos,Boolean> mi=new LinkedHashMap<>(); newInj.forEach(p->mi.put(p,tc.injectorValues().getOrDefault(p,false)));
            Map<BlockPos,Boolean> ms=new LinkedHashMap<>(); newSen.forEach(p->ms.put(p,tc.sensorExpected().getOrDefault(p,false)));
            editableCases.set(i,new TestCase(tc.name(),mi,ms));
        }
        initWidgets();
    }
    public void onCaseResult(int idx,boolean pass) {
        while(caseResults.size()<=idx) caseResults.add(null);
        caseResults.set(idx,pass); initWidgets();
    }
    public void onAllResults(boolean[] results) {
        for (int i=0;i<results.length;i++) { while(caseResults.size()<=i) caseResults.add(null); caseResults.set(i,results[i]); }
        initWidgets();
    }

    // ── Preview ───────────────────────────────────────────────────────────────

    private void applyPreview(TestCase tc) {
        var world=MinecraftClient.getInstance().world; if (world==null) return;
        for (var e:tc.injectorValues().entrySet()) { var s=world.getBlockState(e.getKey()); if (s.isOf(ModBlocks.INJECTOR)) world.setBlockState(e.getKey(),s.with(InjectorBlock.POWERED,e.getValue()),2); }
        for (var e:tc.sensorExpected().entrySet()) { var s=world.getBlockState(e.getKey()); if (s.isOf(ModBlocks.SENSOR)) world.setBlockState(e.getKey(),s.with(SensorBlock.EXPECTED,e.getValue()),2); }
    }
    private void clearPreview() {
        var world=MinecraftClient.getInstance().world; if (world==null) return;
        for (BlockPos p:injectors) { var s=world.getBlockState(p); if (s.isOf(ModBlocks.INJECTOR)) world.setBlockState(p,s.with(InjectorBlock.POWERED,false),2); }
        for (BlockPos p:sensors)   { var s=world.getBlockState(p); if (s.isOf(ModBlocks.SENSOR))   world.setBlockState(p,s.with(SensorBlock.EXPECTED,false),2); }
    }
    @Override public void removed() { super.removed(); }
    @Override protected void drawBackground(DrawContext ctx, float delta, int mx, int my) {}
    @Override protected void drawForeground(DrawContext ctx, int mx, int my) {} // bloque "Inventory" de HandledScreen
    @Override public boolean shouldPause() { return false; }
}