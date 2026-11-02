package com.playze.dreamstalker;

import java.util.function.BiConsumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Bridge so the rest of the mod never touches Simple Voice Chat classes directly.
 * The voice plugin sets {@link #hook} only if Simple Voice Chat is installed.
 */
public final class VoiceMimic {
    private VoiceMimic() {}

    public static volatile BiConsumer<Entity, ServerPlayer> hook;

    public static void mimic(Entity source, ServerPlayer target) {
        BiConsumer<Entity, ServerPlayer> h = hook;
        if (h != null && Settings.VOICE_MIMIC) h.accept(source, target);
    }
}
