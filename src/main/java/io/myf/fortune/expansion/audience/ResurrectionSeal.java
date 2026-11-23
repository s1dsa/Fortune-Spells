package io.myf.fortune.expansion.audience;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;

public final class ResurrectionSeal extends Entity {
    public static final int REVEAL = 34;
    private static final EntityDataAccessor<Long> START = SynchedEntityData.defineId(ResurrectionSeal.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> REBIRTH = SynchedEntityData.defineId(ResurrectionSeal.class, EntityDataSerializers.BOOLEAN);

    public ResurrectionSeal(EntityType<ResurrectionSeal> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    static void appear(ServerPlayer player, boolean rebirth, int remaining) {
        ServerLevel world = player.serverLevel();
        ResurrectionSeal seal = new ResurrectionSeal(AudienceContent.RESURRECTION_SEAL.get(), world);
        seal.setPos(player.getX(), Math.max(world.getMinBuildHeight(), player.getY()) + .08, player.getZ());
        seal.entityData.set(REBIRTH, rebirth);
        seal.entityData.set(START, world.getGameTime() - seal.duration() + remaining);
        world.addFreshEntity(seal);
        world.playSound(null, seal.getX(), seal.getY(), seal.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 1.5f, rebirth ? 1.45f : .65f);
        if (rebirth) world.sendParticles(ParticleTypes.END_ROD, seal.getX(), seal.getY() + 1, seal.getZ(), 36, 1.1, 1.5, 1.1, .04);
    }

    public boolean rebirth() { return entityData.get(REBIRTH); }
    public int duration() { return rebirth() ? REVEAL : StaffResurrection.DELAY; }
    public float age(float partial) { return level().getGameTime() - entityData.get(START) + partial; }

    @Override public void tick() {
        super.tick();
        if (!level().isClientSide && age(0) >= duration()) discard();
    }
    @Override protected void defineSynchedData() {
        entityData.define(START, 0L);
        entityData.define(REBIRTH, false);
    }
    @Override protected void readAdditionalSaveData(CompoundTag data) {}
    @Override protected void addAdditionalSaveData(CompoundTag data) {}
    @Override public AABB getBoundingBoxForCulling() { return getBoundingBox().inflate(10, 7, 10); }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 128 * 128; }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
