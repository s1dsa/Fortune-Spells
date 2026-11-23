package io.myf.fortune.expansion.rose;

import io.myf.fortune.expansion.SpellTargets;
import lykrast.meetyourfight.entity.RoseSpiritEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

public final class RoseWraith extends RoseSpiritEntity {
    public enum Phase { SUMMONING, GUARDING, SEEKING }
    private static final Phase[] PHASES = Phase.values();
    public static final int SUMMON_TICKS = 12;
    private static final EntityDataAccessor<Integer> LINK = SynchedEntityData.defineId(RoseWraith.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> PHASE = SynchedEntityData.defineId(RoseWraith.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Long> START = SynchedEntityData.defineId(RoseWraith.class, EntityDataSerializers.LONG);
    private LivingEntity quarry;
    private long guardEndTick;
    private int side;
    private float power;

    public RoseWraith(EntityType<? extends RoseWraith> type, Level world) {
        super(type, world);
        setNoAi(true);
        setNoGravity(true);
        xpReward = 0;
    }

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(LINK, -1);
        entityData.define(PHASE, (byte) Phase.SUMMONING.ordinal());
        entityData.define(START, 0L);
    }

    // The player-bound spirit has its own flight and attack timeline.
    @Override protected void registerGoals() {}
    @Override protected boolean shouldDespawnInPeaceful() { return false; }
    @Override protected boolean shouldDropLoot() { return false; }
    @Override public boolean shouldDropExperience() { return false; }
    @Override public boolean canBeSeenByAnyone() { return phase() != Phase.SUMMONING && super.canBeSeenByAnyone(); }
    @Override public boolean isAttackable() { return phase() != Phase.SUMMONING && super.isAttackable(); }
    @Override public boolean isPickable() { return phase() != Phase.SUMMONING && super.isPickable(); }
    @Override public boolean isPushable() { return false; }
    @Override protected void pushEntities() {}
    @Override public void travel(Vec3 input) { setDeltaMovement(Vec3.ZERO); }

    public Phase phase() { return PHASES[entityData.get(PHASE)]; }
    public Entity link() { return level().getEntity(entityData.get(LINK)); }
    public float summonProgress(float partialTick) {
        return Mth.clamp((level().getGameTime() - entityData.get(START) + partialTick) / SUMMON_TICKS, 0, 1);
    }

    void bind(LivingEntity caster, float power, int side) {
        this.power = power;
        this.side = side;
        entityData.set(LINK, caster.getId());
        entityData.set(START, level().getGameTime());
        moveTo(guardPosition(caster));
        setYRot(caster.getYRot());
    }

    void guard(long endTick) {
        guardEndTick = endTick;
        entityData.set(PHASE, (byte) Phase.GUARDING.ordinal());
        setStatus(RISING);
    }

    boolean guardExpired() { return level().getGameTime() >= guardEndTick; }

    void seek() {
        entityData.set(PHASE, (byte) Phase.SEEKING.ordinal());
        setStatus(ATTACKING);
    }

    float counterDamage() { return 15 * power; }

    @Override public boolean isAlliedTo(Entity other) {
        Entity owner = link();
        return other == owner || (other instanceof RoseWraith spirit && owner != null && spirit.link() == owner)
                || super.isAlliedTo(other);
    }

    @Override public boolean hurt(DamageSource source, float damage) {
        if (phase() == Phase.SUMMONING || (source.getEntity() != null && isAlliedTo(source.getEntity()))) return false;
        return super.hurt(source, damage);
    }

    @Override public void remove(RemovalReason reason) {
        if (!isRemoved() && !level().isClientSide && link() instanceof LivingEntity caster) {
            RoseGuard.onDepart(caster, this);
        }
        super.remove(reason);
    }

    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel world)) return;
        if (!(link() instanceof LivingEntity owner) || !owner.isAlive() || !isAlive()
                || tickCount >= SUMMON_TICKS + 200 || distanceToSqr(owner) > 32 * 32) {
            discard();
            return;
        }
        switch (phase()) {
            case SUMMONING -> {
                if (world.getGameTime() - entityData.get(START) >= SUMMON_TICKS) RoseGuard.appear(owner, this);
            }
            case GUARDING -> {
                if (guardExpired() || !RoseGuard.isGuarding(owner)) {
                    RoseGuard.release(owner);
                } else {
                    if (getStatus() == RISING && world.getGameTime() - entityData.get(START) >= SUMMON_TICKS + 10)
                        setStatus(OUT);
                    flyTo(guardPosition(owner), .8);
                    setYRot(owner.getYRot());
                }
            }
            case SEEKING -> pursue(world, owner);
        }
    }

    private Vec3 guardPosition(LivingEntity caster) {
        double yaw = caster.getYRot() * Mth.DEG_TO_RAD;
        double height = caster.getBbHeight() * .58 + .12 * Math.sin(tickCount * .12);
        return caster.position().add(side * Math.cos(yaw) * 1.5, height, side * Math.sin(yaw) * 1.5);
    }

    private boolean eligible(LivingEntity owner, LivingEntity candidate) {
        return !(candidate instanceof RoseWraith) && candidate.level() == level() && SpellTargets.canHit(owner, candidate);
    }

    private void pursue(ServerLevel world, LivingEntity owner) {
        if (quarry == null || !eligible(owner, quarry) || quarry.distanceToSqr(owner) > 28 * 28) {
            quarry = null;
            if (tickCount % 4 == 0) {
                quarry = world.getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(16),
                        candidate -> eligible(owner, candidate)).stream()
                        .min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
            }
        }
        if (quarry == null) {
            flyTo(guardPosition(owner), .38);
            return;
        }
        Vec3 destination = quarry.getBoundingBox().getCenter();
        if (position().distanceToSqr(destination) >= 1.6) {
            flyTo(destination, .38);
            lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, destination);
            return;
        }
        world.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY(), getZ(), 36, 1, .6, 1, .15);
        world.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
        world.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, .6F, 1.45F);
        var damage = damageSources().indirectMagic(this, owner);
        for (LivingEntity hit : world.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(3),
                candidate -> eligible(owner, candidate) && candidate.distanceToSqr(this) <= 9)) {
            hit.hurt(damage, 10 * power);
        }
        discard();
    }

    private void flyTo(Vec3 destination, double speed) {
        Vec3 delta = destination.subtract(position());
        double distance = delta.length();
        if (distance > speed) delta = delta.scale(speed / distance);
        setPos(position().add(delta));
    }
}
