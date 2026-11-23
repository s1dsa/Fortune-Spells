package io.myf.fortune.expansion.audience;

import io.myf.fortune.expansion.SpellTargets;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.myf.fortune.combat.StaffSpellDamage;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.*;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.network.NetworkHooks;

import java.util.LinkedHashSet;
import java.util.Set;

/** One authoritative anchor for the entire cast. Release, contact, and dissipation are distinct cues. */
public final class CataclysmField extends Entity {
    public static final int FALL_TICKS = 12, AFTERGLOW_TICKS = 42;
    public static final float IMPACT_RADIUS = 64;
    private static final String ACTIVE_FIELD = "fortune_spells_cataclysm_field";
    private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(CataclysmField.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> START = SynchedEntityData.defineId(CataclysmField.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> RELEASE = SynchedEntityData.defineId(CataclysmField.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> IMPACT = SynchedEntityData.defineId(CataclysmField.class, EntityDataSerializers.LONG);
    private float damage;

    public CataclysmField(EntityType<? extends CataclysmField> type, Level world) {
        super(type, world);
        noPhysics = true;
        setNoGravity(true);
    }

    @Override protected void defineSynchedData() {
        entityData.define(OWNER, 0);
        entityData.define(START, 0L);
        entityData.define(RELEASE, -1L);
        entityData.define(IMPACT, -1L);
    }

    public int ownerId() { return entityData.get(OWNER); }
    public boolean released() { return entityData.get(RELEASE) >= 0; }
    public boolean impacted() { return entityData.get(IMPACT) >= 0; }
    public float age(float partial) { return elapsed(START, partial); }
    public float phaseAge(float partial) { return released() ? elapsed(RELEASE, partial) : age(partial); }
    public float impactAge(float partial) { return impacted() ? elapsed(IMPACT, partial) : 0; }
    private float elapsed(EntityDataAccessor<Long> start, float partial) {
        return Math.max(0, level().getGameTime() - entityData.get(start) + partial);
    }

    public static CataclysmField active(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData();
        if (!tag.hasUUID(ACTIVE_FIELD)) return null;
        Entity entity = player.serverLevel().getEntity(tag.getUUID(ACTIVE_FIELD));
        return entity instanceof CataclysmField field && field.ownerId() == player.getId() ? field : null;
    }

    static void begin(ServerPlayer player) {
        ServerLevel world = player.serverLevel();
        var field = new CataclysmField(AudienceContent.CATACLYSM_FIELD.get(), world);
        field.setPos(aim(player));
        field.entityData.set(OWNER, player.getId());
        field.entityData.set(START, world.getGameTime());
        if (!world.addFreshEntity(field)) return;
        var previous = active(player);
        if (previous != null && !previous.released()) previous.discard();
        player.getPersistentData().putUUID(ACTIVE_FIELD, field.getUUID());
    }

    private static Vec3 aim(ServerPlayer player) {
        ServerLevel world = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(80));
        var block = world.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, player));
        Vec3 hit = block.getLocation();
        var entity = ProjectileUtil.getEntityHitResult(player, eye, hit, new AABB(eye, hit).inflate(1),
                e -> e instanceof LivingEntity living && SpellTargets.canHit(player, living, true), eye.distanceToSqr(hit));
        if (entity != null) return entity.getEntity().position();
        if (block.getType() != HitResult.Type.MISS) return hit;
        BlockPos column = BlockPos.containing(hit);
        if (world.hasChunkAt(column)) {
            int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
            if (y > world.getMinBuildHeight()) return new Vec3(hit.x, y, hit.z);
        }
        return player.position();
    }

    boolean release(float strength) {
        if (isRemoved() || released()) return false;
        damage = strength;
        entityData.set(RELEASE, level().getGameTime());
        level().playSound(null, blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 3, .52f);
        return true;
    }

    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel world)) return;
        if (!(world.getEntity(ownerId()) instanceof ServerPlayer player) || !player.isAlive()) {
            discard();
            return;
        }
        if (!released()) {
            var data = MagicData.getPlayerMagicData(player);
            if (!data.isCasting() || !CataclysmSpell.ID.toString().equals(data.getCastingSpellId())
                    || !AudienceStaffItem.isStaffSlot(player, data.getCastingEquipmentSlot())) discard();
            return;
        }
        if (!impacted() && phaseAge(0) >= FALL_TICKS) {
            entityData.set(IMPACT, world.getGameTime());
            strike(world, player);
        }
        if (impacted() && impactAge(0) >= AFTERGLOW_TICKS) discard();
    }

    private void strike(ServerLevel world, ServerPlayer player) {
        Vec3 p = position();
        AABB bounds = new AABB(p, p).inflate(IMPACT_RADIUS, 32, IMPACT_RADIUS);
        Set<LivingEntity> targets = new LinkedHashSet<>();
        for (Entity part : world.getEntities(player, bounds, e -> e instanceof LivingEntity || e instanceof PartEntity<?>)) {
            Entity parent = part instanceof PartEntity<?> multipart ? multipart.getParent() : part;
            if (!(parent instanceof LivingEntity living) || !SpellTargets.canHit(player, living, true)) continue;
            AABB box = part.getBoundingBox();
            double dx = Math.max(0, Math.max(box.minX - p.x, p.x - box.maxX));
            double dz = Math.max(0, Math.max(box.minZ - p.z, p.z - box.maxZ));
            if (dx * dx + dz * dz <= IMPACT_RADIUS * IMPACT_RADIUS) targets.add(living);
        }
        var source = SpellRegistry.getSpell(CataclysmSpell.ID).getDamageSource(this, player);
        for (LivingEntity target : targets) {
            if (SpellTargets.canHit(player, target, true)) StaffSpellDamage.apply(target, source, damage);
        }
        world.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 6, .68f);
    }

    @Override public void remove(RemovalReason reason) {
        if (level() instanceof ServerLevel world && world.getEntity(ownerId()) instanceof ServerPlayer owner) {
            var tag = owner.getPersistentData();
            if (tag.hasUUID(ACTIVE_FIELD) && tag.getUUID(ACTIVE_FIELD).equals(getUUID())) tag.remove(ACTIVE_FIELD);
        }
        super.remove(reason);
    }

    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 2048.0 * 2048; }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
