package com.playze.dreamstalker;

import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Dream Stalker - entry point.
 *
 * NOTE: written against Forge 61.x (Minecraft 1.21.11): EventBus 7 (BusGroup / Event.BUS) and the
 * ResourceLocation -> Identifier rename. I could not compile this here, so expect to fix a few
 * import paths / method names against your MDK. Everything is kept small so fixes are easy.
 */
@Mod(DreamStalker.MODID)
public class DreamStalker {
    public static final String MODID = "dreamstalker";

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    public DreamStalker(FMLJavaModLoadingContext context) {
        BusGroup modBus = context.getModBusGroup();

        ModEntities.ENTITIES.register(modBus);
        EntityAttributeCreationEvent.getBus(modBus).addListener(ModEntities::attributes);

        // Server side: sleep -> dream dimension -> wake after 5 minutes
        TickEvent.ServerTickEvent.Post.BUS.addListener(DreamManager::onServerTick);
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(DreamManager::onLogout);
        ServerStoppedEvent.BUS.addListener(e -> {
            DreamManager.reset();
            PrankManager.reset();
        });

        // Prank chain: server chat watcher + the one packet that triggers the fake crash on a client
        Net.init();
        TickEvent.ServerTickEvent.Post.BUS.addListener(PrankManager::onServerTick);
        net.minecraftforge.event.ServerChatEvent.BUS.addListener(PrankManager::onChat);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.playze.dreamstalker.client.ClientSetup.init(modBus);
        }
    }
}
