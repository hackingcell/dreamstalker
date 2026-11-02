package com.playze.dreamstalker.client;

import com.playze.dreamstalker.DreamStalker;
import com.playze.dreamstalker.Watcher;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Humanoid model wearing the skin of a random online player (chosen by the server at spawn, synced to
 * everyone). Falls back to textures/entity/watcher.png if that player isn't known to this client.
 */
public class WatcherRenderer
        extends HumanoidMobRenderer<Watcher, WatcherRenderer.WatcherRenderState, HumanoidModel<WatcherRenderer.WatcherRenderState>> {

    public static class WatcherRenderState extends HumanoidRenderState {
        public Identifier skin;
    }

    private static final Identifier FALLBACK = DreamStalker.id("textures/entity/watcher.png");

    public WatcherRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5f);
    }

    @Override
    public WatcherRenderState createRenderState() {
        return new WatcherRenderState();
    }

    @Override
    public void extractRenderState(Watcher entity, WatcherRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.skin = resolveSkin(entity.getSkinOwner().orElse(null));
    }

    private static Identifier resolveSkin(UUID owner) {
        if (owner == null) return FALLBACK;
        ClientPacketListener conn = Minecraft.getInstance().getConnection();
        if (conn == null) return FALLBACK;
        PlayerInfo info = conn.getPlayerInfo(owner);
        if (info == null) return FALLBACK;
        // 1.21.9+ : PlayerSkin.body().texturePath().  If your mappings still have PlayerSkin.texture(), use that instead.
        return info.getSkin().body().texturePath();
    }

    @Override
    public Identifier getTextureLocation(WatcherRenderState state) {
        return state.skin != null ? state.skin : FALLBACK;
    }
}
