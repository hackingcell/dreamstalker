package com.playze.dreamstalker.voice;

import com.playze.dreamstalker.Settings;
import com.playze.dreamstalker.VoiceMimic;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.EntityAudioChannel;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.PlayerDisconnectedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Keeps the last ~45 seconds of each player's (already Opus-encoded) mic packets IN MEMORY ONLY
 * and, when a Watcher "whispers", replays a stitched random slice of the target player's own voice
 * from the Watcher's position - audible to that player only.
 * Nothing is written to disk; the buffer is dropped when the player disconnects.
 */
@ForgeVoicechatPlugin
public class StalkerVoicePlugin implements VoicechatPlugin {
    private static final int MAX_FRAMES = 50 * 45; // 20 ms per frame
    private final Map<UUID, ArrayDeque<byte[]>> buffers = new ConcurrentHashMap<>();
    private final Random rng = new Random();
    private volatile VoicechatServerApi api;
    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "dreamstalker-mimic");
        t.setDaemon(true);
        return t;
    });

    @Override
    public String getPluginId() {
        return "dreamstalker";
    }

    @Override
    public void initialize(VoicechatApi voicechatApi) {
        VoiceMimic.hook = this::mimic;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, e -> api = e.getVoicechat());
        registration.registerEvent(MicrophonePacketEvent.class, this::onMic);
        registration.registerEvent(PlayerDisconnectedEvent.class, e -> buffers.remove(e.getPlayerUuid()));
    }

    private void onMic(MicrophonePacketEvent event) {
        if (!Settings.VOICE_MIMIC) return;
        VoicechatConnection con = event.getSenderConnection();
        if (con == null) return;
        byte[] data = event.getPacket().getOpusEncodedData();
        if (data == null || data.length == 0) return; // empty = end of speech

        ArrayDeque<byte[]> buf = buffers.computeIfAbsent(con.getPlayer().getUuid(), k -> new ArrayDeque<>());
        synchronized (buf) {
            buf.addLast(data.clone());
            while (buf.size() > MAX_FRAMES) buf.removeFirst();
        }
    }

    private void mimic(Entity source, ServerPlayer target) {
        VoicechatServerApi server = this.api;
        if (server == null) return;
        ArrayDeque<byte[]> buf = buffers.get(target.getUUID());
        if (buf == null) return;

        byte[][] frames;
        synchronized (buf) {
            frames = buf.toArray(new byte[0][]);
        }
        if (frames.length < 60) return; // need at least ~1.2s of speech

        int len = Math.min(frames.length, 30 + rng.nextInt(120));
        int start = rng.nextInt(frames.length - len + 1);

        EntityAudioChannel channel = server.createEntityAudioChannel(UUID.randomUUID(), server.fromEntity(source));
        if (channel == null) return;
        channel.setDistance(40f);
        UUID only = target.getUUID();
        channel.setFilter(p -> p.getUuid().equals(only)); // only the person being stalked hears it

        for (int i = 0; i < len; i++) {
            byte[] frame = frames[start + i];
            exec.schedule(() -> {
                try {
                    channel.send(frame);
                } catch (Exception ignored) {
                }
            }, i * 20L, TimeUnit.MILLISECONDS);
        }
        exec.schedule(() -> {
            try {
                channel.flush();
            } catch (Exception ignored) {
            }
        }, len * 20L + 20, TimeUnit.MILLISECONDS);
    }
}
