package com.playze.dreamstalker.client;

import com.playze.dreamstalker.DreamManager;
import com.playze.dreamstalker.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;

public final class ClientEvents {
    private ClientEvents() {}

    private static int cooldown = 20 * 90; // grace period before the first fake crash

    private static int ambientTimer = nextAmbientDelay();

    private static int nextAmbientDelay() {
        return Settings.AMBIENT_MIN_TICKS
                + (int) (Math.random() * (Settings.AMBIENT_MAX_TICKS - Settings.AMBIENT_MIN_TICKS));
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();

        // Very faint scare sound every ~5 minutes while you're in a world
        if (Settings.AMBIENT_SCARE_ENABLED && mc.level != null && mc.player != null && !mc.isPaused()) {
            if (--ambientTimer <= 0) {
                Assets.playSound("faint", Settings.AMBIENT_VOLUME);
                ambientTimer = nextAmbientDelay();
            }
        }

        if (!Settings.FAKE_CRASH_ENABLED) return;
        if (mc.level == null || mc.player == null || mc.screen != null) return;
        if (!DreamManager.isDream(mc.level)) return;
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        if (mc.player.getRandom().nextInt(Settings.FAKE_CRASH_AVG_TICKS) == 0) {
            cooldown = 20 * 120;
            mc.setScreen(new FakeCrashScreen());
        }
    }

    /** /dreamstalker crash | jumpscare | cleanup  (client-side testing commands) */
    public static void commands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("dreamstalker")
                .then(Commands.literal("crash").executes(ctx -> {
                    Minecraft mc = Minecraft.getInstance();
                    mc.execute(() -> mc.setScreen(new FakeCrashScreen()));
                    return 1;
                }))
                .then(Commands.literal("jumpscare").executes(ctx -> {
                    Minecraft mc = Minecraft.getInstance();
                    mc.execute(() -> mc.setScreen(new JumpscareScreen(0)));
                    return 1;
                }))
                .then(Commands.literal("cleanup").executes(ctx -> {
                    int n = LoreWriter.cleanup();
                    Minecraft.getInstance().gui.getChat().addMessage(
                            net.minecraft.network.chat.Component.literal("Dream Stalker: removed " + n + " file(s)."));
                    return 1;
                })));
    }
}
