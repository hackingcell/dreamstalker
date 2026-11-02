package com.playze.dreamstalker;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Stands far away and stares. Never moves, never attacks.
 * Vanishes if you get close, stare at it too long, or after 90 seconds.
 * Occasionally whispers your own voice back at you (Simple Voice Chat, optional).
 */
public class Watcher extends Monster {
    private int stareTicks;
    private int lifeTicks;
    private int lonelyTicks;

    public Watcher(EntityType<? extends Watcher> type, Level level) {
        super(type, level);
        this.setInvulnerable(true);
        this.setPersistenceRequired();
        this.setSilent(true);
    }

    /** UUID of the player whose skin this Watcher wears (synced to clients so everyone sees the same face). */
    private static final EntityDataAccessor<Optional<UUID>> SKIN_OWNER =
            SynchedEntityData.defineId(Watcher.class, EntityDataSerializers.OPTIONAL_UUID);

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SKIN_OWNER, Optional.empty());
    }

    public void setSkinOwner(UUID owner) {
        this.entityData.set(SKIN_OWNER, Optional.ofNullable(owner));
    }

    public Optional<UUID> getSkinOwner() {
        return this.entityData.get(SKIN_OWNER);
    }

    @Override
    protected void registerGoals() {
        // Only goal: turn the head toward the nearest player. No movement, no targeting.
        this.goalSelector.addGoal(0, new LookAtPlayerGoal(this, Player.class, 80.0f, 1.0f));
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) return;
        ServerLevel level = (ServerLevel) this.level();

        Player player = level.getNearestPlayer(this, 80.0);
        if (player == null) {
            if (++lonelyTicks > 200) this.discard();
            return;
        }
        lonelyTicks = 0;

        double dist = this.distanceTo(player);
        Vec3 look = player.getViewVector(1.0f).normalize();
        Vec3 toMe = this.position().add(0, 1.6, 0).subtract(player.getEyePosition()).normalize();
        boolean staring = look.dot(toMe) > 0.985 && dist < 60;
        stareTicks = staring ? stareTicks + 1 : Math.max(0, stareTicks - 2);

        if (dist < 14 || stareTicks > 70 || ++lifeTicks > 20 * 90) {
            vanish(level);
            return;
        }

        // Every 20s (offset), 50% chance to whisper the player's own voice
        if (lifeTicks % 400 == 200 && this.random.nextFloat() < 0.5f && player instanceof ServerPlayer sp) {
            VoiceMimic.mimic(this, sp);
        }
    }

    private void vanish(ServerLevel level) {
        level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0, getZ(), 20, 0.3, 0.8, 0.3, 0.02);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0f, 0.5f);
        this.discard();
    }
}
