package io.myf.fortune.entity;

import io.myf.fortune.FortuneIds.Anim;
import io.myf.fortune.FortuneIds.Ask;
import io.myf.fortune.FortuneIds.Face;
import io.myf.fortune.FortuneIds.Result;
import io.myf.fortune.FortuneTuning;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public class FateCardEntity extends Entity {
    private static final EntityDataAccessor<Byte> FACE =
            SynchedEntityData.defineId(FateCardEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> ASK =
            SynchedEntityData.defineId(FateCardEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> ANIM =
            SynchedEntityData.defineId(FateCardEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> RESULT =
            SynchedEntityData.defineId(FateCardEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> OWNER_ID =
            SynchedEntityData.defineId(FateCardEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SESSION_ID =
            SynchedEntityData.defineId(FateCardEntity.class, EntityDataSerializers.INT);

    public int clientAnim, animTimer;
    private Vec3 orbitCenter;
    private Vec3 landingStart;
    private Vec3 movementTarget = Vec3.ZERO;
    private int shuffleTick, angleOffset, interpolationSteps;

    public FateCardEntity(EntityType<? extends FateCardEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(FACE, Face.HIDDEN);
        entityData.define(ASK, Ask.NONE);
        entityData.define(ANIM, Anim.NOT_HERE);
        entityData.define(RESULT, Result.NONE);
        entityData.define(OWNER_ID, -1);
        entityData.define(SESSION_ID, -1);
    }

    public byte getFace() { return entityData.get(FACE); }
    public byte getAsk() { return entityData.get(ASK); }
    public byte getAnim() { return entityData.get(ANIM); }
    public byte getResult() { return entityData.get(RESULT); }
    public int getOwnerId() { return entityData.get(OWNER_ID); }
    public int getSessionId() { return entityData.get(SESSION_ID); }

    public void setFace(byte face) { entityData.set(FACE, face); }
    public void setAsk(byte ask) { entityData.set(ASK, ask); }
    public void setAnim(byte anim) { entityData.set(ANIM, anim); }
    public void setResult(byte result) { entityData.set(RESULT, result); }

    public void bindTo(int ownerId, int sessionId) {
        entityData.set(OWNER_ID, ownerId);
        entityData.set(SESSION_ID, sessionId);
    }

    public void startShuffle(Vec3 center, int offsetDegrees, Vec3 landingSlot) {
        orbitCenter = center;
        angleOffset = offsetDegrees;
        movementTarget = landingSlot;
        shuffleTick = 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (interpolationSteps > 0) {
                setPos(position().lerp(movementTarget, 1.0 / interpolationSteps--));
            }
            updateClientAnimation();
        } else if (orbitCenter != null) {
            tickShuffle();
        }
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps, boolean teleport) {
        if (!level().isClientSide || steps <= 0) {
            super.lerpTo(x, y, z, yaw, pitch, steps, teleport);
            return;
        }
        movementTarget = new Vec3(x, y, z);
        interpolationSteps = steps;
        setRot(yaw, pitch);
    }

    private void updateClientAnimation() {
        int animation = getAnim();
        if (animation != clientAnim) {
            clientAnim = animation;
            animTimer = Anim.DURATION[animation];
        } else if (animTimer > 0) {
            --animTimer;
        }
    }

    private void tickShuffle() {
        int approach = FortuneTuning.SHUFFLE_APPROACH_TICKS;
        int acceleration = FortuneTuning.SHUFFLE_ACCEL_TICKS;
        int orbitTicks = approach + 2 * acceleration;
        ++shuffleTick;
        Vec3 movement;
        if (shuffleTick <= orbitTicks) {
            int rotatingTicks = Math.max(0, shuffleTick - approach);
            int remaining = orbitTicks - shuffleTick;
            double degrees = rotatingTicks <= acceleration
                    ? 0.5 * rotatingTicks * rotatingTicks
                    : acceleration * acceleration - 0.5 * remaining * remaining;
            double angle = (angleOffset + degrees) * Mth.DEG_TO_RAD;
            Vec3 ringPoint = orbitCenter.add(FortuneTuning.ORBIT_RADIUS * Math.cos(angle), 0,
                    -FortuneTuning.ORBIT_RADIUS * Math.sin(angle));
            movement = ringPoint.subtract(position());
            double speedLimit = shuffleTick * FortuneTuning.ORBIT_APPROACH_ACCEL;
            if (shuffleTick <= approach && movement.lengthSqr() > speedLimit * speedLimit) {
                movement = movement.normalize().scale(speedLimit);
            }
        } else {
            int landingTick = shuffleTick - orbitTicks;
            movement = landingPosition(landingTick).subtract(position());
        }
        setDeltaMovement(movement);
        move(MoverType.SELF, movement);
        if (shuffleTick == orbitTicks) landingStart = position();
        if (shuffleTick >= orbitTicks + FortuneTuning.SHUFFLE_LAND_TICKS) {
            setPos(movementTarget);
            setDeltaMovement(Vec3.ZERO);
            orbitCenter = null;
            landingStart = null;
        }
    }

    private Vec3 landingPosition(int landingTick) {
        int delay = angleOffset / 36 + 1;
        int elapsed = Math.max(0, landingTick - delay);
        int linearTicks = FortuneTuning.SHUFFLE_SETTLE_TICKS;
        int rampTicks = FortuneTuning.SHUFFLE_LAND_TICKS - delay - linearTicks;
        // Accelerate from rest, then keep that same speed for the final ten ticks.
        double fullSpeed = 1.0 / (rampTicks * 0.5 + linearTicks);
        double fraction = elapsed <= rampTicks
                ? fullSpeed * elapsed * elapsed / (2.0 * rampTicks)
                : fullSpeed * (rampTicks * 0.5 + elapsed - rampTicks);
        return landingStart.lerp(movementTarget, fraction);
    }

    @Override public boolean isPickable() { return false; }
    @Override public boolean isAttackable() { return false; }
    @Override public boolean skipAttackInteraction(Entity attacker) { return true; }
    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override public boolean shouldBeSaved() { return false; }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
