package io.myf.fortune.expansion.offense;

import io.myf.fortune.FortuneIds;
import io.myf.fortune.expansion.SpellTargets;
import io.myf.fortune.network.FSNetwork;
import io.myf.fortune.network.S2CFortuneFx;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.myf.fortune.combat.StaffSpellDamage;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.sounds.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.entity.PartEntity;

import java.util.*;
import java.util.function.Predicate;

/** Each cast owns its child effects; figures are transient and never written to a world save. */
public final class SpellFigure extends Entity implements IEntityAdditionalSpawnData {
    public enum Kind { JAWS, TOWER, CHIME, DANCE, PETAL, COLUMN, SWAMP_RUNE, BELL_RUNE, FIRE_FIELD, WING_HALO, WING_RUNE }
    public enum RunePhase { DRAWING, RELEASED, SHATTERING }
    private static final RunePhase[] RUNE_PHASES = RunePhase.values();
    private static final Kind[] KINDS = Kind.values();
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(SpellFigure.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ANIMATION_AGE = SynchedEntityData.defineId(SpellFigure.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RUNE_PROGRESS = SynchedEntityData.defineId(SpellFigure.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> RUNE_PHASE = SynchedEntityData.defineId(SpellFigure.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DANCE_STRIKE = SynchedEntityData.defineId(SpellFigure.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> STRIKE_YAW = SynchedEntityData.defineId(SpellFigure.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> EMPOWERED = SynchedEntityData.defineId(SpellFigure.class, EntityDataSerializers.BOOLEAN);
    public static final float JAWS_RANGE = 20;
    public static final int FIRST_BITE = 12;
    public static final int BITE_INTERVAL = 16;
    private static final int BITE_COUNT = 5;
    public static final int LAST_BITE = FIRST_BITE + (BITE_COUNT - 1) * BITE_INTERVAL;
    public static final int JAWS_DURATION = LAST_BITE + 8;
    public static final float TOWER_RADIUS = 24;
    public static final float COLUMN_RADIUS = 3.5f;
    public static final float SLASH_RANGE = 6;
    public static final float FIRE_RADIUS = 14;
    public static final float AREA_HEIGHT = 8;
    private static final double JAWS_MIN_DOT = Math.cos(Math.toRadians(70));
    private static final double SLASH_MIN_DOT = Math.cos(Math.toRadians(80));
    private static final int BEAT = 15;
    private UUID owner;
    private Vec3 direction = Vec3.ZERO;
    private float power, mana;
    private int spellLevel, strikes;
    private InteractionHand channelHand;
    private int clientStrikeStart;
    public static final int DANCE_TICKS = 8 * 20;
    public static final int SWING_TICKS = 13;
    private int lastRuneTick;
    private long serverAnimationOrigin;
    private int clientAgeOffset, lastAgeSample;
    private long serverPhaseStart;
    private int clientPhaseStart;
    private RunePhase clientPhase = RunePhase.DRAWING;
    private boolean fed;
    private Map<LivingEntity, Float> deferredBites;
    private Set<UUID> victims;
    private List<SpellFigure> petals;
    public record BurstPetal(float x, float y, float z, int age) {}
    private List<BurstPetal> burstPetals = List.of();

    public List<BurstPetal> burstPetals() { return burstPetals; }

    public SpellFigure(EntityType<? extends SpellFigure> type, Level world) {
        super(type,world);
        noPhysics = true;
        noCulling = true;
    }

    static SpellFigure spawn(ServerLevel world, Kind kind, LivingEntity caster, Vec3 at, Vec3 direction,
                             float power, float mana, int level, boolean empowered) {
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        return spawn(world, kind, caster, at, direction, yaw, power, mana, level, empowered);
    }

    static SpellFigure spawn(ServerLevel world, Kind kind, LivingEntity caster, Vec3 at, Vec3 direction,
                             float yaw, float power, float mana, int level, boolean empowered) {
        SpellFigure figure = create(world,kind,caster,at,direction,yaw,power,mana,level,empowered);
        world.addFreshEntity(figure);
        return figure;
    }

    private static SpellFigure create(ServerLevel world, Kind kind, LivingEntity caster, Vec3 at, Vec3 direction,
                                      float yaw, float power, float mana, int level, boolean empowered) {
        SpellFigure e = new SpellFigure(OffensiveSpells.FIGURE.get(),world);
        e.owner = caster.getUUID();
        e.direction = direction.normalize();
        e.setPos(at);
        e.setYRot(yaw);
        e.setXRot((float)Math.toDegrees(-Math.asin(Math.min(1,Math.max(-1,e.direction.y)))));
        e.entityData.set(KIND,kind.ordinal());
        e.entityData.set(EMPOWERED,empowered);
        e.serverAnimationOrigin=world.getGameTime();
        e.power = power;
        e.mana = mana;
        e.spellLevel = level;
        if (kind == Kind.DANCE) {
            e.petals = new ArrayList<>(48);
            e.strikes = 8 + caster.getRandom().nextInt(5);
        }
        if (kind == Kind.JAWS) e.deferredBites = new HashMap<>();
        if (kind == Kind.CHIME || kind == Kind.PETAL) e.victims = new HashSet<>();
        return e;
    }

    public Kind kind() { return KINDS[entityData.get(KIND)]; }
    public boolean empowered() { return entityData.get(EMPOWERED); }
    public int effectScale() { return empowered() ? 2 : 1; }
    void bindToCast(InteractionHand hand) { channelHand = hand; }
    public int strikeNumber() { return entityData.get(DANCE_STRIKE); }
    public int ticksAfterStrike() { return tickCount - clientStrikeStart; }
    public float strikeYaw() { return entityData.get(STRIKE_YAW); }
    public int age() {
        return level().isClientSide ? Math.max(0,tickCount+clientAgeOffset) : tickCount;
    }
    @Override public boolean shouldRenderAtSqrDistance(double distance) {
        int range = isRune() || hasReleaseCircles() ? 192 : 128;
        range *= effectScale();
        return distance < range * range;
    }
    @Override public boolean broadcastToPlayer(ServerPlayer player) {
        // The enlarged tracking radius is for the large assemblies. Keep small projectiles local.
        if (!isRune() && !hasReleaseCircles()) {
            int range = player.server.getScaledTrackingDistance(128);
            if (Math.abs(getX() - player.getX()) > range || Math.abs(getZ() - player.getZ()) > range) return false;
        }
        return super.broadcastToPlayer(player);
    }
    public int runeProgress() { return entityData.get(RUNE_PROGRESS); }
    public RunePhase runePhase() { return RUNE_PHASES[entityData.get(RUNE_PHASE)]; }
    public int runePhaseAge() {
        if (runePhase() == RunePhase.DRAWING) return 0;
        return level().isClientSide ? Math.max(0,tickCount-clientPhaseStart)
                : (int)(level().getGameTime()-serverPhaseStart);
    }
    public boolean isRune() { return kind()==Kind.BELL_RUNE || kind()==Kind.SWAMP_RUNE || kind()==Kind.WING_RUNE; }
    public boolean hasReleaseCircles() {
        return switch (kind()) {
            case JAWS, TOWER, DANCE, FIRE_FIELD, WING_HALO -> true;
            default -> false;
        };
    }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> field) {
        super.onSyncedDataUpdated(field);
        if (KIND.equals(field)) noCulling=!(isRune() || hasReleaseCircles());
        if (DANCE_STRIKE.equals(field)) clientStrikeStart = tickCount;
        if (ANIMATION_AGE.equals(field) && level().isClientSide && entityData.get(ANIMATION_AGE)>lastAgeSample) {
            lastAgeSample=entityData.get(ANIMATION_AGE);
            clientAgeOffset=lastAgeSample-tickCount;
        }
        if (RUNE_PHASE.equals(field) && level().isClientSide && runePhase()!=clientPhase) {
            clientPhase = runePhase();
            clientPhaseStart = tickCount;
        }
    }
    @Override public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeVarInt(tickCount);
        buffer.writeEnum(runePhase());
        buffer.writeVarInt(runePhaseAge());
        if (!burstPetals.isEmpty()) {
            buffer.writeVarInt(burstPetals.size());
            for (BurstPetal petal : burstPetals) {
                buffer.writeFloat(petal.x()); buffer.writeFloat(petal.y()); buffer.writeFloat(petal.z());
                buffer.writeByte(petal.age());
            }
        }
    }
    @Override public void readSpawnData(FriendlyByteBuf buffer) {
        // A new viewer joins the current fade. An existing viewer starts at zero when the phase changes.
        lastAgeSample=buffer.readVarInt();
        clientAgeOffset=lastAgeSample-tickCount;
        clientPhase = buffer.readEnum(RunePhase.class);
        clientPhaseStart = tickCount - buffer.readVarInt();
        entityData.set(RUNE_PHASE,(byte)clientPhase.ordinal());
        if (buffer.isReadable()) {
            int count = buffer.readVarInt();
            List<BurstPetal> bursts = new ArrayList<>();
            for (int i = 0; i < count; i++)
                bursts.add(new BurstPetal(buffer.readFloat(),buffer.readFloat(),buffer.readFloat(),buffer.readUnsignedByte()));
            burstPetals = List.copyOf(bursts);
        }
    }
    @Override public AABB getBoundingBoxForCulling() {
        int s = effectScale();
        if (kind()==Kind.DANCE || (isRune() && runePhase()==RunePhase.DRAWING))
            return getBoundingBox().inflate(70*s,0,70*s).expandTowards(0,90*s,0).expandTowards(0,-8*s,0);
        if (hasReleaseCircles()) return getBoundingBox().inflate(45*s,0,45*s).expandTowards(0,43*s,0).expandTowards(0,-3*s,0);
        if (!isRune()) return super.getBoundingBoxForCulling();
        // Broken vertical gate: center 4.85 - 3.2, halo radius 6 * 1.015, outward drift up to
        // 6 * .31, plus .3 global drop. Eight blocks also cover tilted fragments and interpolation.
        return getBoundingBox().inflate((TOWER_RADIUS + 1)*s,0,(TOWER_RADIUS + 1)*s).expandTowards(0,40*s,0).expandTowards(0,-8*s,0);
    }
    void startRune() {
        sound(SoundEvents.BEACON_AMBIENT,2.5f,.55f);
    }
    void traceRune(int ticks) {
        if (runePhase()!=RunePhase.DRAWING) return;
        lastRuneTick = age();
        int previous=runeProgress();
        int progress=Math.min(200,ticks);
        entityData.set(RUNE_PROGRESS,progress);
        if (kind()==Kind.WING_RUNE) return;
        if (crossed(previous,progress,80)) sound(SoundEvents.AMETHYST_BLOCK_CHIME,2.25f,.82f);
        if (crossed(previous,progress,110)) sound(SoundEvents.BEACON_ACTIVATE,2.25f,.95f);
        if (crossed(previous,progress,150)) sound(SoundEvents.BELL_RESONATE,2.25f,.9f);
        if (progress>=160 && progress<200 && previous/8!=progress/8)
            sound(SoundEvents.AMETHYST_BLOCK_CHIME,2.25f,1f+(progress-160)/40f);
    }
    private static boolean crossed(int previous,int progress,int threshold) {
        return previous<threshold && progress>=threshold;
    }
    void lightRune() {
        if (runePhase()!=RunePhase.DRAWING) return;
        entityData.set(RUNE_PROGRESS,200);
        finishRune(RunePhase.RELEASED);
        releaseSound();
    }
    void releaseSound() {
        sound(SoundEvents.BELL_BLOCK,3.2f,.54f);
        sound(SoundEvents.BEACON_ACTIVATE,2.5f,.76f);
    }
    void shatterRune() {
        if (runePhase()!=RunePhase.DRAWING) return;
        if (kind()==Kind.WING_RUNE) {
            discard();
            return;
        }
        finishRune(RunePhase.SHATTERING);
        sound(SoundEvents.AMETHYST_BLOCK_BREAK,2f,.66f);
    }
    private void finishRune(RunePhase phase) {
        entityData.set(RUNE_PHASE,(byte)phase.ordinal());
        serverPhaseStart=level().getGameTime();
        OffensiveSpells.forgetRitual(this);
    }
    @Override protected void defineSynchedData() {
        entityData.define(KIND,0); entityData.define(ANIMATION_AGE,0); entityData.define(RUNE_PROGRESS,0);
        entityData.define(RUNE_PHASE,(byte)0);
        entityData.define(DANCE_STRIKE,0); entityData.define(STRIKE_YAW,0f);
        entityData.define(EMPOWERED,false);
    }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    @Override protected void readAdditionalSaveData(CompoundTag data) {}
    @Override protected void addAdditionalSaveData(CompoundTag data) {}
    @Override public void remove(RemovalReason reason) {
        if (kind()==Kind.DANCE && level() instanceof ServerLevel world) {
            if (owner != null && world.getEntity(owner) instanceof LivingEntity caster) OffensiveControl.endDance(caster,this);
            if (petals != null) for (SpellFigure petal : petals) if (!petal.isRemoved()) petal.discard();
        }
        if (!level().isClientSide && isRune())
            OffensiveSpells.forgetRitual(this);
        super.remove(reason);
    }

    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel world)) return;
        // A newly loaded chunk can start ticking later than its neighbors. Advance attacks by
        // simulation ticks and correct the client's animation origin when the two clocks diverge.
        long animationStart = world.getGameTime() - tickCount;
        if (animationStart!=serverAnimationOrigin) {
            serverAnimationOrigin=animationStart;
            entityData.set(ANIMATION_AGE,tickCount);
        }
        if (isRune()) {
            tickRune(world);
            return;
        }
        if (!(world.getEntity(owner) instanceof LivingEntity caster) || !caster.isAlive()) {
            discard();
            return;
        }
        int t = age();
        if (t == 0) return;
        switch(kind()) {
            case JAWS -> jaws(world,caster,t);
            case TOWER -> tower(world,caster,t);
            case WING_HALO -> { if (t >= 52) discard(); }
            case CHIME -> {
                travel(world,caster,1.6,1.05,(8 + 2 * (spellLevel - 1))*power,"bell_shockwave");
                if (!world.getBlockState(blockPosition()).isAir())
                    world.sendParticles(ParticleTypes.WAX_ON,getX(),getY(),getZ(),5,.3,.3,.3,.03);
                if (t >= 20) discard();
            }
            case DANCE -> {
                if (!OffensiveControl.isActiveDance(caster, this)) {
                    discard();
                    return;
                }
                setPos(caster.position());
                // Normal player casts are driven by Iron's casting clock. Immediate command/mob casts
                // have no such clock and advance on the figure's own ticks.
                if (channelHand == null) {
                    if (t == DANCE_TICKS + 1) releaseDance(world,caster);
                    else strikeOnBeat(world,caster,t);
                }
            }
            case PETAL -> {
                if (t <= 14) travel(world,caster,.56*effectScale(),.55*effectScale(),3*power,"sunset_blade_dance");
                if (t >= DANCE_TICKS + 10) discard();
            }
            case COLUMN -> { if (t >= 10) discard(); }
            case FIRE_FIELD -> {
                if (t%4==0) {
                    for (LivingEntity enemy : areaTargets(caster,position(),FIRE_RADIUS)) enemy.setSecondsOnFire(1);
                }
                if (t >= 100) discard();
            }
        }
    }

    private void tickRune(ServerLevel world) {
        RunePhase phase=runePhase();
        if (phase!=RunePhase.DRAWING) {
            if (runePhaseAge()>=(phase==RunePhase.RELEASED ? 17 : 10)) discard();
            return;
        }
        if (!(world.getEntity(owner) instanceof LivingEntity caster) || !caster.isAlive() || age()-lastRuneTick>3) {
            shatterRune();
            return;
        }
        if (runeProgress()<200) {
            setPos(caster.position());
            setYRot(caster.getYRot());
        }
    }

    private void jaws(ServerLevel world, LivingEntity caster, int t) {
        if (t >= JAWS_DURATION) {
            if (fed) {
                caster.heal(caster.getMaxHealth() * power * .1f);
                world.sendParticles(ParticleTypes.HEART,caster.getX(),caster.getY()+1.2,caster.getZ(),5,.4,.4,.4,.04);
            }
            discard();
            return;
        }
        if (t > LAST_BITE || (t > 1 && t % 2 != 0)) return;

        boolean bite = t >= FIRST_BITE && (t - FIRST_BITE) % BITE_INTERVAL == 0;
        Vec3 mouth = position().add(Vec3.directionFromRotation(0, getYRot()).scale(4.5*effectScale()));
        List<LivingEntity> nearby = areaTargets(caster, position(), JAWS_RANGE, JAWS_MIN_DOT);
        if (!deferredBites.isEmpty()) deferredBites.keySet().retainAll(new HashSet<>(nearby));
        for (LivingEntity target : nearby) {
            if (bite) {
                float portion = power * mana / BITE_COUNT;
                // Smaller creatures stay in the jaws. Each landed preliminary bite banks its unspent
                // share for this target only; escaping the sector forfeits that balance.
                float damage = t == LAST_BITE ? portion + deferredBites.getOrDefault(target, 0f)
                        : Math.min(portion, target.getMaxHealth() * .1f);
                if (!hit(caster, target, "croc_devour", damage, true)) continue;
                fed = true;
                if (target.isAlive() && !deferredBites.containsKey(target)) {
                    var decay = new MobEffectInstance(OffensiveSpells.DECAY.get(), 200, empowered() ? 1 : 0);
                    if (target instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) {
                        target.forceAddEffect(decay, caster);
                    } else {
                        target.addEffect(decay, caster);
                    }
                }
                if (t == LAST_BITE) OffensiveControl.bury(target);
                else deferredBites.merge(target, portion - damage, Float::sum);
            }
            if (t < LAST_BITE && target.isAlive() && !target.isInvulnerable()) {
                // Pull toward (never past) the jaws. Use normal movement so solid walls still obstruct the pull.
                Vec3 offset = mouth.subtract(target.position());
                Vec3 step = offset.scale(Math.min(0.65 / Math.max(0.01, offset.length()), 0.45));
                target.setDeltaMovement(step);
                target.hurtMarked = true;
            }
        }
        if (bite) {
            sound(SoundEvents.EVOKER_FANGS_ATTACK, 1.4f, .55f);
            world.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MUD.defaultBlockState()),
                    mouth.x, mouth.y + .3, mouth.z, 28, 1.5, .35, 1.5, .12);
            world.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BONE_BLOCK.defaultBlockState()),
                    mouth.x, mouth.y + 1, mouth.z, 16, 1, .4, 1, .09);
        }
    }

    private void tower(ServerLevel world, LivingEntity caster, int t) {
        if (t == 0 || t % BEAT != 0) return;
        if (t <= 8*BEAT) {
            List<LivingEntity> nearby = areaTargets(caster,position(),TOWER_RADIUS);
            Set<LivingEntity> caught = new HashSet<>();
            for (int i=0;i<3;i++) {
                double a=world.random.nextDouble()*Math.PI*2;
                double reach=(TOWER_RADIUS-COLUMN_RADIUS)*effectScale();
                double r=Math.sqrt(world.random.nextDouble())*reach;
                Vec3 point = position().add(r*Math.cos(a),0,r*Math.sin(a));
                if (i==0 && !nearby.isEmpty()) {
                    Vec3 offset = nearby.get(world.random.nextInt(nearby.size())).position().subtract(position()).multiply(1,0,1);
                    double distance = offset.length();
                    if (distance > reach) offset = offset.scale(reach/distance);
                    point = position().add(offset);
                }
                spawn(world,Kind.COLUMN,caster,point,Vec3.ZERO,power,mana,1,empowered());
                caught.addAll(areaTargets(caster,point,COLUMN_RADIUS));
            }
            for (LivingEntity enemy : caught) hit(caster,enemy,"phantom_belfry",power*mana/20f,false);
            sound(SoundEvents.BELL_BLOCK,1.4f,.78f + (t/BEAT%4)*.075f);
        } else if (t >= 8*BEAT+30) {
            for (LivingEntity enemy : areaTargets(caster,position(),TOWER_RADIUS)) {
                if (hit(caster,enemy,"phantom_belfry",power*mana,false)) OffensiveSpells.stun(enemy,60);
            }
            for (int i=0;i<80;i++) OffensiveSpells.runePoint(world,position(),OffensiveSpells.GOLD,i*Math.PI/40,TOWER_RADIUS*effectScale());
            sound(SoundEvents.BELL_RESONATE,2f,.62f);
            discard();
        }
    }

    void channelTick(ServerPlayer caster, int elapsed) {
        if (channelHand != null) strikeOnBeat(caster.serverLevel(),caster,elapsed);
    }

    void releaseDance(ServerLevel world, LivingEntity caster) {
        setPos(caster.position());
        if (caster.hasEffect(OffensiveSpells.STUNNED.get())) {
            discard();
            return;
        }
        List<LivingEntity> caught = areaTargets(caster,position(),FIRE_RADIUS);
        List<BurstPetal> bursts = new ArrayList<>(petals.size());
        for (SpellFigure petal : petals) {
            if (petal.isRemoved()) continue;
            Vec3 offset = petal.position().subtract(position()).scale(1.0/effectScale());
            bursts.add(new BurstPetal((float)offset.x,(float)offset.y,(float)offset.z,petal.age()));
            petal.discard();
        }
        for (LivingEntity enemy : caught) {
            if (hit(caster,enemy,"sunset_blade_dance",power*mana,true)) enemy.setSecondsOnFire(5);
        }
        SpellFigure field = create(world,Kind.FIRE_FIELD,caster,position(),Vec3.ZERO,0,power,mana,1,empowered());
        field.burstPetals = List.copyOf(bursts);
        world.addFreshEntity(field);
        sound(SoundEvents.GENERIC_EXPLODE,1.3f,.8f);
        discard();
    }

    private void strikeOnBeat(ServerLevel world, LivingEntity caster, int elapsed) {
        if (!caster.isAlive() || caster.hasEffect(OffensiveSpells.STUNNED.get())) {
            discard();
            return;
        }
        if (elapsed < 1 || elapsed > DANCE_TICKS) return;
        int strike = (elapsed - 1) * strikes / DANCE_TICKS;
        if (strikeNumber() >= strike + 1) return;
        setPos(caster.position());
        setYRot(caster.getYRot());
        entityData.set(STRIKE_YAW,getYRot());
        entityData.set(DANCE_STRIKE,strike+1);
        InteractionHand hand = channelHand == null ? InteractionHand.MAIN_HAND : channelHand;
        caster.swing(hand,true);
        if (caster instanceof ServerPlayer) {
            int motion = (strike & 1) + (hand == InteractionHand.OFF_HAND ? 2 : 0);
            FSNetwork.sendTracking(caster,new S2CFortuneFx(FortuneIds.FxKind.SUNSET_SWING,caster.getId(),motion));
        }
        direction = caster.getLookAngle();
        for (LivingEntity enemy : areaTargets(caster,position(),SLASH_RANGE,SLASH_MIN_DOT)) {
            float health = enemy.getHealth();
            float absorption = enemy.getAbsorptionAmount();
            if (hit(caster, enemy, "sunset_blade_dance", 6 * power, true) && enemy.isAlive()
                    && (enemy.getHealth() < health || enemy.getAbsorptionAmount() < absorption)) {
                ExposedEffect.applySlash(enemy, caster);
            }
        }
        Vec3 forward = Vec3.directionFromRotation(0,caster.getYRot());
        for (int i=0;i<4;i++) {
            int side=i%2==0 ? -1 : 1;
            float angle = (float)(side*(.55 + world.random.nextDouble()*.58));
            Vec3 vector = forward.yRot(angle).add(0,world.random.nextDouble()*.08,0).normalize();
            petals.add(spawn(world,Kind.PETAL,caster,caster.position().add(0,1.1*effectScale(),0),vector,power,mana,1,empowered()));
        }
        sound(SoundEvents.PLAYER_ATTACK_SWEEP,.8f,1.1f+world.random.nextFloat()*.4f);
    }

    private void travel(ServerLevel world, LivingEntity caster, double speed, double width, float damage, String spell) {
        Vec3 start = position(), end = start.add(direction.scale(speed));
        boolean petal = kind() == Kind.PETAL;
        double height = petal ? AREA_HEIGHT * effectScale() : width;
        AABB sweep = new AABB(start,end).inflate(width,height,width);
        for (LivingEntity target : targets(caster,sweep,part -> {
            AABB bounds = part.getBoundingBox().inflate(width,height,width);
            if (petal) bounds = bounds.setMaxY(part.getY() + height);
            return bounds.contains(start) || bounds.clip(start,end).isPresent();
        })) {
            if (victims.add(target.getUUID())) hit(caster,target,spell,damage,petal);
        }
        setPos(end);
    }

    private List<LivingEntity> areaTargets(LivingEntity caster, Vec3 center, double radius) {
        return areaTargets(caster,center,radius,-1);
    }

    private List<LivingEntity> areaTargets(LivingEntity caster, Vec3 center, double radius, double minimumDot) {
        double reach = radius * effectScale();
        double height = AREA_HEIGHT * effectScale();
        AABB bounds = new AABB(center,center).inflate(reach,height + 1e-6,reach);
        double yaw = Math.toRadians(getYRot());
        double facingX = -Math.sin(yaw), facingZ = Math.cos(yaw);
        return targets(caster,bounds,part -> {
            double dx = part.getX() - center.x, dz = part.getZ() - center.z;
            double distanceSquared = dx*dx + dz*dz;
            return Math.abs(part.getY() - center.y) <= height + 1e-7
                    && distanceSquared <= reach*reach + 1e-7
                    && (minimumDot < 0 || dx*facingX + dz*facingZ + 1e-7 >= minimumDot*Math.sqrt(distanceSquared));
        });
    }

    private List<LivingEntity> targets(LivingEntity caster, AABB bounds, Predicate<Entity> intersects) {
        // Forge includes registered multipart bodies in this query, even when the parent is outside the area.
        Set<LivingEntity> targets = new LinkedHashSet<>();
        for (Entity part : level().getEntities(caster,bounds,e -> e instanceof LivingEntity || e instanceof PartEntity<?>)) {
            Entity parent = part instanceof PartEntity<?> body ? body.getParent() : part;
            if (parent instanceof LivingEntity target && SpellTargets.canHit(caster,target,empowered()) && intersects.test(part))
                targets.add(target);
        }
        return new ArrayList<>(targets);
    }

    private boolean hit(LivingEntity caster, LivingEntity target, String id, float damage, boolean rapid) {
        var spell = SpellRegistry.getSpell(FortuneIds.id(id));
        var source = spell.getDamageSource(this,caster);
        if (rapid) source.setIFrames(0);
        if (empowered()) return StaffSpellDamage.apply(target,source,damage*effectScale());
        return damage > 0 && DamageSources.applyDamage(target,damage,source);
    }

    private void sound(SoundEvent sound,float volume,float pitch) {
        level().playSound(null,getX(),getY(),getZ(),sound,SoundSource.PLAYERS,volume,pitch);
    }
}
