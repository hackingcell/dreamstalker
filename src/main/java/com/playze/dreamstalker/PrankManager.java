package com.playze.dreamstalker;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.network.PacketDistributor;

/**
 * The prank chain, all driven from server chat:
 *
 *  1. 5 minutes in:                    <H0ST> you're not real
 *  2. someone says "im real" etc:      "James joined the game"  then  <James> hello I'm the real james not you hobo
 *  3. someone answers no / nuh uh ...: <James> are you sure?       then the fake crash on that player's PC
 *  4. the crash also drops "hello James Fullbrook.txt" on their Desktop (and it shows up on the fake desktop)
 *
 * Everything the "characters" say is a plain server chat line, so nobody needs a real account called James/H0ST.
 * Runs once per world/server session. State is reset when the server stops.
 */
public final class PrankManager {
    private PrankManager() {}

    private record Task(int tick, Runnable action) {}

    private static final List<Task> TASKS = new ArrayList<>();
    /** 0 = waiting for H0ST, 1 = waiting for "im real", 2 = James is entering, 3 = waiting for denial, 4 = done */
    private static int stage = 0;
    private static int jamesSpokeTick = Integer.MAX_VALUE;

    private static final Pattern REAL_CLAIM =
            Pattern.compile("(?i)\\b(i\\s*'?\\s*m|im|i\\s+am)\\s+(the\\s+)?real\\b");
    private static final Pattern DENIAL = Pattern.compile(
            "(?i)^\\W*(no+|nope|nuh+\\s*-?\\s*uh+|nu+\\s*uh+|nah|not\\s+true|you\\s*'?\\s*re\\s+not|ur\\s+not|u\\s+r\\s+not|that'?s\\s+not\\s+true)\\b");

    public static void reset() {
        TASKS.clear();
        stage = 0;
        jamesSpokeTick = Integer.MAX_VALUE;
    }

    public static void onServerTick(TickEvent.ServerTickEvent.Post event) {
        if (!Settings.PRANK_ENABLED) return;
        MinecraftServer server = event.server();
        int now = server.getTickCount();

        if (stage == 0 && now >= Settings.PRANK_HOST_DELAY_TICKS && !server.getPlayerList().getPlayers().isEmpty()) {
            say(server, "H0ST", "you're not real");
            stage = 1;
        }

        for (Task t : new ArrayList<>(TASKS)) {
            if (now >= t.tick()) {
                TASKS.remove(t);
                t.action().run();
            }
        }
    }

    public static void onChat(ServerChatEvent event) {
        if (!Settings.PRANK_ENABLED) return;
        ServerPlayer player = event.getPlayer();
        MinecraftServer server = player.getServer();
        if (server == null) return;
        String text = event.getRawText();
        UUID speaker = player.getUUID();
        server.execute(() -> handleChat(server, speaker, text)); // keep all state on the server thread
    }

    private static void handleChat(MinecraftServer server, UUID speaker, String text) {
        int now = server.getTickCount();

        if (stage == 1 && REAL_CLAIM.matcher(text).find()) {
            stage = 2;
            later(now + 20, () -> server.getPlayerList().broadcastSystemMessage(
                    Component.translatable("multiplayer.player.joined", "James").withStyle(ChatFormatting.YELLOW), false));
            later(now + 70, () -> {
                say(server, "James", "hello I'm the real james not you hobo");
                jamesSpokeTick = server.getTickCount();
                stage = 3;
            });
            return;
        }

        if (stage == 3 && server.getTickCount() > jamesSpokeTick && DENIAL.matcher(text).find()) {
            stage = 4;
            later(now + 20, () -> say(server, "James", "are you sure?"));
            later(now + 120, () -> crashVictim(server, speaker));
        }
    }

    private static void crashVictim(MinecraftServer server, UUID speaker) {
        ServerPlayer victim = Settings.PRANK_TARGET_USERNAME.isBlank()
                ? server.getPlayerList().getPlayer(speaker)
                : server.getPlayerList().getPlayerByName(Settings.PRANK_TARGET_USERNAME);
        if (victim != null) {
            Net.CHANNEL.send(new FakeCrashPacket(), PacketDistributor.PLAYER.with(victim));
        }
    }

    private static void say(MinecraftServer server, String name, String message) {
        server.getPlayerList().broadcastSystemMessage(Component.literal("<" + name + "> " + message), false);
    }

    private static void later(int tick, Runnable action) {
        TASKS.add(new Task(tick, action));
    }
}
