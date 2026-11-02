package com.playze.dreamstalker;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** Server -> one client: "start the fake crash now". Carries no data. */
public record FakeCrashPacket() {
    public static final StreamCodec<RegistryFriendlyByteBuf, FakeCrashPacket> CODEC =
            StreamCodec.unit(new FakeCrashPacket());

    public static void handle(FakeCrashPacket msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                com.playze.dreamstalker.client.PrankClient.run();
            }
        });
    }
}
