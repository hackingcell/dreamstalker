package com.playze.dreamstalker.client;

import com.playze.dreamstalker.Settings;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Fullscreen scare for ~2.5s. Supply textures/gui/jumpscare_0..N.png and a "jumpscare" sound. */
public class JumpscareScreen extends Screen {
    private final int index;
    private int ticks;

    public JumpscareScreen(int index) {
        super(Component.literal(""));
        this.index = index;
    }

    @Override
    protected void init() {
        Assets.playJumpscare();
    }

    @Override
    public void tick() {
        if (++ticks >= 50) {
            this.onClose();
            if (Settings.LORE_FILES_ENABLED) {
                Optional<Path> written = LoreWriter.writeNext();
                if (written.isPresent() && this.minecraft.player != null) {
                    this.minecraft.player.displayClientMessage(
                            Component.literal("Something was left on your computer: " + written.get().getFileName()), false);
                }
            }
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // little shake
        int dx = (int) ((Math.random() - 0.5) * 16);
        int dy = (int) ((Math.random() - 0.5) * 16);

        Identifier tex = Assets.jumpscareTexture(index);
        if (Assets.has(tex)) {
            g.fill(0, 0, this.width, this.height, 0xFF000000);
            Assets.drawFullscreen(g, tex, dx, dy, this.width, this.height);
        } else {
            // fallback so the mod works before you add art
            g.fill(0, 0, this.width, this.height, (ticks / 3) % 2 == 0 ? 0xFF8A0000 : 0xFF000000);
            g.pose().pushMatrix();
            g.pose().translate(this.width / 2f + dx, this.height / 2f + dy);
            g.pose().scale(6f, 6f);
            g.drawCenteredString(this.font, "IT SEES YOU", 0, -4, 0xFFFFFFFF);
            g.pose().popMatrix();
        }
    }
}
