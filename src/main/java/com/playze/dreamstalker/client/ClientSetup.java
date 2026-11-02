package com.playze.dreamstalker.client;

import com.playze.dreamstalker.ModEntities;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.event.TickEvent;

public final class ClientSetup {
    private ClientSetup() {}

    public static void init(BusGroup modBus) {
        EntityRenderersEvent.RegisterRenderers.getBus(modBus).addListener(
                e -> e.registerEntityRenderer(ModEntities.WATCHER.get(), WatcherRenderer::new));
        TickEvent.ClientTickEvent.Post.BUS.addListener(e -> ClientEvents.tick());
        RegisterClientCommandsEvent.BUS.addListener(ClientEvents::commands);
    }
}
