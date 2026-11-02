package com.playze.dreamstalker.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Phase 1: the game "freezes" and shows a fake not-responding dialog. Then it becomes the fake desktop. */
public class FakeCrashScreen extends Screen {
    private int ticks;

    public FakeCrashScreen() {
        super(Component.literal(""));
    }

    @Override
    protected void init() {
        Assets.playSound("glitch", 0.6f);
    }

    @Override
    public void tick() {
        if (++ticks >= 70) {
            this.minecraft.setScreen(new FakeDesktopScreen());
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false; // ESC does nothing
    }

    @Override
    public boolean isPauseScreen() {
        return false; // the world keeps running behind it
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0xFF000000);

        int w = 300, h = 100;
        int x = (this.width - w) / 2, y = (this.height - h) / 2;
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF404040);
        g.fill(x, y, x + w, y + h, 0xFFF0F0F0);
        g.fill(x, y, x + w, y + 18, 0xFF1F4E9C);
        g.drawString(this.font, "Minecraft", x + 6, y + 5, 0xFFFFFFFF, false);
        g.drawString(this.font, "Minecraft has stopped responding.", x + 14, y + 36, 0xFF000000, false);
        String dots = ".".repeat((ticks / 10) % 4);
        g.drawString(this.font, "Collecting information" + dots, x + 14, y + 56, 0xFF555555, false);
    }
}
