package com.playze.dreamstalker.client;

import com.playze.dreamstalker.DreamStalker;
import com.playze.dreamstalker.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/** Helpers for the images/sounds you supply. Missing files fall back to plain colors, never crash. */
final class Assets {
    private Assets() {}

    /** Pixel size of your fullscreen images (wallpaper + jumpscares). Change if yours differ. */
    static final int TEX_W = 1920;
    static final int TEX_H = 1080;

    static final Identifier DESKTOP = DreamStalker.id("textures/gui/desktop.png");

    static Identifier jumpscareTexture(int index) {
        return DreamStalker.id("textures/gui/jumpscare_" + index + ".png");
    }

    /** How many consecutive jumpscare_0.png, jumpscare_1.png ... exist (at least 1, so fallbacks still work). */
    static int jumpscareCount() {
        int n = 0;
        while (n < 16 && has(jumpscareTexture(n))) n++;
        return Math.max(1, n);
    }

    static boolean has(Identifier id) {
        return Minecraft.getInstance().getResourceManager().getResource(id).isPresent();
    }

    static void drawFullscreen(GuiGraphics g, Identifier tex, int x, int y, int w, int h) {
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0f, 0f, w, h, TEX_W, TEX_H, TEX_W, TEX_H);
    }

    static void playSound(String name, float volume) {
        SoundEvent ev = SoundEvent.createVariableRangeEvent(DreamStalker.id(name));
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ev, 1.0f, volume));
    }

    static void playJumpscare() {
        playSound("jumpscare", Settings.JUMPSCARE_VOLUME);
    }
}
