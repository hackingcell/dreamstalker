package com.playze.dreamstalker.client;

import com.playze.dreamstalker.Settings;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Phase 2: a FAKE desktop drawn entirely inside Minecraft. It does not capture your screen; the icons
 * are built from the NAMES of the items on your real Desktop (see DesktopScanner), nothing else.
 * ESC is disabled. Clicking any icon triggers a jumpscare.
 * Safety net: closes itself after Settings.DESKTOP_TIMEOUT_TICKS so nobody is ever trapped.
 */
public class FakeDesktopScreen extends Screen {
    private static final int CELL_W = 110;
    private static final int CELL_H = 96;
    private static final int MARGIN = 30;
    private static final int TASKBAR = 28;

    private List<DesktopScanner.Entry> icons = List.of();
    private int perColumn = 1;
    private int jumpscareCount = 1;
    private int ticks;

    public FakeDesktopScreen() {
        super(Component.literal(""));
    }

    @Override
    protected void init() {
        icons = DesktopScanner.scan();
        jumpscareCount = Assets.jumpscareCount();
        perColumn = Math.max(1, (this.height - TASKBAR - MARGIN) / CELL_H);
    }

    @Override
    public void tick() {
        if (++ticks >= Settings.DESKTOP_TIMEOUT_TICKS) this.onClose();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int cellX(int i) { return MARGIN + (i / perColumn) * CELL_W; }
    private int cellY(int i) { return MARGIN + (i % perColumn) * CELL_H; }

    private boolean hit(int i, double mx, double my) {
        int x = cellX(i), y = cellY(i);
        return mx >= x - 8 && mx <= x + 80 && my >= y - 6 && my <= y + 84;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (Assets.has(Assets.DESKTOP)) {
            Assets.drawFullscreen(g, Assets.DESKTOP, 0, 0, this.width, this.height);
        } else {
            g.fillGradient(0, 0, this.width, this.height, 0xFF0A1A33, 0xFF000000);
        }

        for (int i = 0; i < icons.size(); i++) {
            DesktopScanner.Entry e = icons.get(i);
            int x = cellX(i), y = cellY(i);
            if (hit(i, mouseX, mouseY)) g.fill(x - 8, y - 6, x + 80, y + 84, 0x553A7BD5);

            g.fill(x + 12, y, x + 60, y + 48, e.color());
            g.drawCenteredString(this.font, e.tag(), x + 36, y + 20, 0xFF000000);

            String label = this.font.plainSubstrByWidth(e.label(), 92);
            if (label.length() < e.label().length()) label = label.substring(0, Math.max(0, label.length() - 1)) + "...";
            g.drawCenteredString(this.font, label, x + 36, y + 56, 0xFFFFFFFF);
        }

        g.fill(0, this.height - TASKBAR, this.width, this.height, 0xEE101018);
        g.fill(6, this.height - 24, 30, this.height - 4, 0xFF3A7BD5);
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        g.drawString(this.font, time, this.width - 40, this.height - 18, 0xFFFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (int i = 0; i < icons.size(); i++) {
            if (hit(i, event.x(), event.y())) {
                int scare = Math.floorMod(icons.get(i).label().hashCode(), jumpscareCount);
                this.minecraft.setScreen(new JumpscareScreen(scare));
                return true;
            }
        }
        return true; // swallow clicks on empty desktop
    }
}
