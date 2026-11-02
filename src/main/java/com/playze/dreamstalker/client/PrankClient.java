package com.playze.dreamstalker.client;

import com.playze.dreamstalker.Settings;
import net.minecraft.client.Minecraft;

/** Runs on the targeted player's client when the server says "crash now". */
public final class PrankClient {
    private PrankClient() {}

    public static void run() {
        // Drop the note first, so it also shows up as an icon on the fake desktop a few seconds later.
        if (Settings.LORE_FILES_ENABLED) {
            LoreWriter.writeText(Settings.PRANK_FILE_NAME, "hello " + Settings.PRANK_FULL_NAME + System.lineSeparator());
        }
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new FakeCrashScreen());
    }
}
