package com.playze.dreamstalker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

/** Sleep -> teleported to a dream dimension -> wake up at your bed after 5 minutes. */
public final class DreamManager {
    private DreamManager() {}

    private record Dream(ResourceKey<Level> originDim, Vec3 pos, float yRot, float xRot, int wakeTick) {}

    private static final String[] DREAM_DIMS = {"pale_woods", "ash_flats", "the_hush"};
    private static final Map<UUID, Dream> DREAMS = new HashMap<>();
    /** Players who just woke from a dream: their next sleep is normal (so the night can still pass). */
    private static final Set<UUID> RESTED = new HashSet<>();

    public static boolean isDream(Level level) {
        // ResourceKey.toString() looks like "ResourceKey[minecraft:dimension / dreamstalker:pale_woods]".
        // Done this way so it survives the location()/identifier() rename.
        return level.dimension().toString().contains(DreamStalker.MODID + ":");
    }

    public static void reset() {
        DREAMS.clear();
        RESTED.clear();
    }

    public static void onServerTick(TickEvent.ServerTickEvent.Post event) {
        tick(event.server());
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            Dream d = DREAMS.get(sp.getUUID());
            MinecraftServer server = sp.getServer();
            if (d != null && server != null) wake(server, sp, d); // bring them home before the save
        }
    }

    private static void tick(MinecraftServer server) {
        int now = server.getTickCount();
        for (ServerPlayer p : new ArrayList<>(server.getPlayerList().getPlayers())) {
            Dream d = DREAMS.get(p.getUUID());
            if (d == null) {
                if (p.isSleeping() && p.getSleepTimer() >= Settings.SLEEP_TRIGGER_TICKS && !isDream(p.level())) {
                    if (RESTED.remove(p.getUUID())) continue; // sleep normally this time
                    if (p.getRandom().nextFloat() <= Settings.DREAM_CHANCE) enter(server, p);
                }
            } else if (now >= d.wakeTick()) {
                wake(server, p, d);
            } else if (now % 100 == 0) {
                trySpawnWatcher(p);
            }
        }
    }

    private static void enter(MinecraftServer server, ServerPlayer p) {
        RandomSource r = p.getRandom();
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION,
                DreamStalker.id(DREAM_DIMS[r.nextInt(DREAM_DIMS.length)]));
        ServerLevel target = server.getLevel(key);
        if (target == null) return; // dimension JSON missing/invalid - fail safe, sleep normally

        DREAMS.put(p.getUUID(), new Dream(p.level().dimension(), p.position(), p.getYRot(), p.getXRot(),
                server.getTickCount() + Settings.DREAM_TICKS));

        p.stopSleepInBed(true, false);

        int x = r.nextInt(-3000, 3000);
        int z = r.nextInt(-3000, 3000);
        target.getChunk(x >> 4, z >> 4); // force-load
        int y = target.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        p.teleportTo(target, x + 0.5, y, z + 0.5, Set.of(), r.nextFloat() * 360f, 0f, true);
    }

    private static void wake(MinecraftServer server, ServerPlayer p, Dream d) {
        DREAMS.remove(p.getUUID());
        RESTED.add(p.getUUID());
        ServerLevel origin = server.getLevel(d.originDim());
        if (origin == null) origin = server.overworld();
        p.teleportTo(origin, d.pos().x, d.pos().y, d.pos().z, Set.of(), d.yRot(), d.xRot(), true);
        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
        p.displayClientMessage(Component.literal("You wake up. It felt like something was watching."), true);
    }

    private static void trySpawnWatcher(ServerPlayer p) {
        ServerLevel level = (ServerLevel) p.level();
        if (level.getEntitiesOfClass(Watcher.class, p.getBoundingBox().inflate(70)).size() >= Settings.MAX_WATCHERS) return;
        RandomSource r = p.getRandom();
        if (r.nextFloat() > 0.5f) return;

        // Somewhere roughly in front of the player, 28-40 blocks out.
        double yaw = Math.toRadians(p.getYRot() + (r.nextDouble() - 0.5) * 100.0);
        double dist = 28 + r.nextDouble() * 12;
        double x = p.getX() - Math.sin(yaw) * dist;
        double z = p.getZ() + Math.cos(yaw) * dist;
        BlockPos column = BlockPos.containing(x, p.getY(), z);
        if (!level.hasChunkAt(column)) return;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());

        Watcher w = new Watcher(ModEntities.WATCHER.get(), level);
        w.setPos(x, y, z);
        // Wear the face of a random player currently on the world/server.
        var online = level.getServer().getPlayerList().getPlayers();
        if (!online.isEmpty()) w.setSkinOwner(online.get(r.nextInt(online.size())).getUUID());
        level.addFreshEntity(w);
    }
}
