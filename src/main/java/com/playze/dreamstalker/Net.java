package com.playze.dreamstalker;

import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;

/**
 * One clientbound packet. Written against the Forge 1.21.x SimpleChannel builder; if the builder
 * calls don't match your Forge 61 version, only this file and FakeCrashPacket need adjusting.
 */
public final class Net {
    private Net() {}

    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(DreamStalker.id("main"))
            .networkProtocolVersion(1)
            .simpleChannel()
            .play()
            .clientbound()
            .add(FakeCrashPacket.class, FakeCrashPacket.CODEC, FakeCrashPacket::handle)
            .build();

    /** Call from the mod constructor so the channel is registered before anyone connects. */
    public static void init() {}
}
