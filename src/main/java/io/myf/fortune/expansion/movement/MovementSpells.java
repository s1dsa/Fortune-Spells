package io.myf.fortune.expansion.movement;

import io.myf.fortune.FortuneIds;
import io.myf.fortune.expansion.SpellTargets;
import io.myf.fortune.expansion.offense.OffensiveSpells;
import io.myf.fortune.network.FSNetwork;
import io.myf.fortune.network.S2CFortuneFx;
import io.redspace.ironsspellbooks.effect.ISyncedMobEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.joml.Vector3f;

import java.util.List;

@Mod.EventBusSubscriber(modid = FortuneIds.MODID)
public final class MovementSpells {
    private static final String ANCHOR = "fortune_spells:phase_anchor";
    private static final String BURROW_POWER = "fortune_spells:burrow_impact";
    private static final String GRANTED_FLIGHT = "fortune_spells:wing_flight";
    private static final String HAS_WINGS = "fortune_spells:wing_pool_active";
    private static final String LANDED_WINGS = "fortune_spells:wing_descent";
    private static final int BONUS = 20;
    private static final DustParticleOptions AFTERIMAGE = new DustParticleOptions(new Vector3f(0.35f, 0.85f, 0.85f), 0.7f);
    private static final BlockParticleOption MUD = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MUD.defaultBlockState());
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, FortuneIds.MODID);
    private static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, FortuneIds.MODID);
    private static final RegistryObject<Attribute> BONE_HEALTH = ATTRIBUTES.register("bone_health",
            () -> new RangedAttribute("attribute.fortune_spells.bone_health", 0, 0, BONUS).setSyncable(true));
    public static final RegistryObject<MobEffect> PHASING = EFFECTS.register("phased", () -> new MovementEffect(0x80D6D4));
    public static final RegistryObject<MobEffect> BOG_BURROW = EFFECTS.register("burrowed", () -> new MovementEffect(0x687C43)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, "942df536-d157-4275-b011-ab6916d0b536", 0.5, AttributeModifier.Operation.MULTIPLY_TOTAL));
    public static final RegistryObject<MobEffect> BONE_WINGS = EFFECTS.register("bone_wings", () -> new WingEffect()
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, "9ecee07b-f537-4798-a34a-931ac3dd7538", 0.5, AttributeModifier.Operation.MULTIPLY_TOTAL)
            .addAttributeModifier(Attributes.MAX_HEALTH, "0b0ff86a-e5be-4809-8d05-bdb251b86d6e", BONUS, AttributeModifier.Operation.ADDITION));

    private MovementSpells() {}

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
        ATTRIBUTES.register(bus);
        bus.addListener(MovementSpells::addAttributes);
    }

    private static void addAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, BONE_HEALTH.get());
    }

    public static void cast(String path, int level, LivingEntity caster, float power) {
        if (!(caster instanceof ServerPlayer player)) return;
        switch (path) {
            case "phase_shift" -> player.addEffect(new MobEffectInstance(PHASING.get(), 100 * level, 0));
            case "burrow" -> {
                rememberSafePosition(player);
                if (!player.addEffect(new MobEffectInstance(BOG_BURROW.get(), 80, level - 1))) return;
                player.getPersistentData().putFloat(BURROW_POWER, 20 * power * (0.5f + 0.05f * (level - 1)));
                player.teleportTo(player.getX(), Math.max(player.level().getMinBuildHeight() + 1, player.getY() - 1.5), player.getZ());
                player.setDeltaMovement(Vec3.ZERO);
                player.fallDistance = 0;
                for (Mob mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(64),
                        mob -> mob.getTarget() == player)) mob.setTarget(null);
            }
            case "bone_wings" -> {
                if (!player.hasEffect(BONE_WINGS.get()) && player.addEffect(new MobEffectInstance(BONE_WINGS.get(), -1, 0)))
                    OffensiveSpells.releaseWingHalo(player);
            }
        }
    }

    public static boolean phasing(Entity entity) {
        return entity instanceof Player player && PHASING.isPresent()
                && (player.hasEffect(PHASING.get()) || player.hasEffect(BOG_BURROW.get()));
    }

    public static Vec3 burrowMovement(Player player, Vec3 requested, Vec3 normal) {
        if (requested.lengthSqr() > 100) return normal;
        Vec3 movement = new Vec3(requested.x, 0, requested.z);
        var sweep = player.getBoundingBox().expandTowards(movement);
        if (!withinBorder(player.level().getWorldBorder(), sweep)) return Vec3.ZERO;
        if (sweep.minY < player.level().getMinBuildHeight() || sweep.maxY > player.level().getMaxBuildHeight()) return normal;
        return protectedBlocks(player, sweep) ? normal : movement;
    }

    public static boolean protectedBlocks(Player player, AABB bounds) {
        var sweep = bounds.deflate(1.0e-6);
        for (BlockPos pos : BlockPos.betweenClosed(Mth.floor(sweep.minX), Mth.floor(sweep.minY), Mth.floor(sweep.minZ),
                Mth.floor(sweep.maxX), Mth.floor(sweep.maxY), Mth.floor(sweep.maxZ))) {
            var state = player.level().getBlockState(pos);
            if (state.is(Blocks.BEDROCK) || state.is(Blocks.BARRIER)) return true;
        }
        return false;
    }

    public static boolean withinBorder(WorldBorder border, AABB bounds) {
        return bounds.minX >= border.getMinX() && bounds.maxX <= border.getMaxX()
                && bounds.minZ >= border.getMinZ() && bounds.maxZ <= border.getMaxZ();
    }

    private static boolean clearAt(Player player, Vec3 at) {
        var level = player.level();
        var bounds = player.getBoundingBox().move(at.subtract(player.position()));
        if (at.y < level.getMinBuildHeight() || bounds.maxY > level.getMaxBuildHeight()
                || !withinBorder(level.getWorldBorder(), bounds) || !level.noCollision(player, bounds)) return false;
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(bounds.minX, bounds.minY - 0.05, bounds.minZ),
                BlockPos.containing(bounds.maxX, bounds.maxY - 1.0e-6, bounds.maxZ))) {
            if (hazard(level.getBlockState(pos))) return false;
        }
        return true;
    }

    private static boolean hazard(BlockState state) {
        return state.getFluidState().is(FluidTags.LAVA) || state.is(BlockTags.FIRE) || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.CACTUS) || state.is(Blocks.POWDER_SNOW)
                || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(BlockTags.CAMPFIRES);
    }

    private static void rememberSafePosition(ServerPlayer player) {
        if (!clearAt(player, player.position()) && player.getPersistentData().contains(ANCHOR)) return;
        CompoundTag anchor = new CompoundTag();
        anchor.putString("dimension", player.level().dimension().location().toString());
        anchor.putDouble("x", player.getX());
        anchor.putDouble("y", player.getY());
        anchor.putDouble("z", player.getZ());
        anchor.putBoolean("burrow", true);
        player.getPersistentData().put(ANCHOR, anchor);
    }

    private static Vec3 findBurrowExit(ServerPlayer player, boolean burrow) {
        if (!burrow && clearAt(player, player.position())) return player.position();
        // Surface near the end of the path, including uneven ground above the tunnel.
        Vec3 origin = player.position().add(0, burrow ? 1.5 : 0, 0);
        if (clearAt(player, origin)) return origin;
        Vec3 safe = nearbyBurrowExit(player, origin);
        if (safe == null) {
            CompoundTag anchor = player.getPersistentData().getCompound(ANCHOR);
            if (anchor.getString("dimension").equals(player.level().dimension().location().toString())) {
                origin = new Vec3(anchor.getDouble("x"), anchor.getDouble("y"), anchor.getDouble("z"));
                safe = clearAt(player, origin) ? origin : nearbyBurrowExit(player, origin);
            }
        }
        if (safe == null) {
            safe = surfaceExit(player, BlockPos.containing(origin));
        }
        if (safe == null) safe = surfaceExit(player, player.level().getSharedSpawnPos());
        return safe;
    }

    private static Vec3 surfaceExit(ServerPlayer player, BlockPos origin) {
        ServerLevel level = player.serverLevel();
        for (int radius = 0; radius <= 8; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.max(Math.abs(x), Math.abs(z)) != radius) continue;
                    BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin.offset(x, 0, z));
                    Vec3 at = Vec3.atBottomCenterOf(surface);
                    if (surface.getY() > level.getMinBuildHeight() && clearAt(player, at)) return at;
                }
            }
        }
        return null;
    }

    private static Vec3 nearbyBurrowExit(ServerPlayer player, Vec3 origin) {
        double nearest = 65;
        Vec3 best = null;
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(origin).offset(-8,-8,-8),
                BlockPos.containing(origin).offset(8,8,8))) {
            Vec3 candidate = Vec3.atBottomCenterOf(pos);
            double distance = candidate.distanceToSqr(origin);
            if (distance < nearest && distance <= 64 && clearAt(player, candidate)) {
                best = candidate;
                nearest = distance;
            }
        }
        return best;
    }

    private static void impact(ServerPlayer player, float damage) {
        ServerLevel level = player.serverLevel();
        for (int i = 0; i < 32; i++) {
            double angle = 2 * Math.PI * i / 32;
            level.sendParticles(MUD, player.getX() + 3.8 * Math.cos(angle), player.getY() + 0.2,
                    player.getZ() + 3.8 * Math.sin(angle), 2, 0.13, 0.12, 0.13, 0.018);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, player.getSoundSource(), 1.2f, 0.65f);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(4),
                target -> target.distanceToSqr(player) <= 16 && SpellTargets.canHit(player, target))) {
            target.hurt(player.damageSources().indirectMagic(player, player), damage);
        }
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || !player.isAlive()) return;
        CompoundTag data = player.getPersistentData();
        if (data.getBoolean(LANDED_WINGS)) {
            data.remove(LANDED_WINGS);
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0));
        }
        boolean burrow = player.hasEffect(BOG_BURROW.get());
        if (burrow) {
            player.fallDistance = 0;
            if (player.tickCount % 3 == 0)
                player.serverLevel().sendParticles(MUD, player.getX(), player.getY() + 1.65, player.getZ(), 4, 0.3, 0.08, 0.3, 0.025);
        }
        if (!burrow && player.hasEffect(PHASING.get()) && player.tickCount % 4 == 0) {
            player.serverLevel().sendParticles(AFTERIMAGE, player.getX(), player.getY() + 1, player.getZ(), 4, 0.15, 0.45, 0.15, 0);
        }
        if (player.hasEffect(BONE_WINGS.get())) {
            if (boneHealth(player) <= 0) player.removeEffect(BONE_WINGS.get());
            else {
                grantFlight(player);
                float cap = player.getMaxHealth() - BONUS + boneHealth(player);
                if (player.getHealth() > cap) player.setHealth(cap);
            }
        }
    }

    @SubscribeEvent
    public static void finishMovement(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        // The connection restores its firstGood position after PlayerTick.END. Teleport after that restore.
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            CompoundTag data = player.getPersistentData();
            if (!player.isAlive() || player.hasEffect(BOG_BURROW.get())) continue;
            boolean exitingBurrow = data.contains(BURROW_POWER);
            // Older saves used a shared anchor for the two spells.
            if (data.contains(ANCHOR) && !exitingBurrow && !data.getCompound(ANCHOR).getBoolean("burrow")) {
                data.remove(ANCHOR);
            }
            if (exitingBurrow && !data.contains(ANCHOR)) rememberSafePosition(player);
            if (data.contains(ANCHOR)) settleBurrowExit(player, data);
        }
    }

    private static void settleBurrowExit(ServerPlayer player, CompoundTag data) {
        CompoundTag anchor = data.getCompound(ANCHOR);
        anchor.putBoolean("burrow", true);
        long now = player.level().getGameTime();
        if (!anchor.contains("exitStarted") || anchor.getLong("exitStarted") > now) anchor.putLong("exitStarted", now);
        boolean delayed = now - anchor.getLong("exitStarted") >= 20;
        // If the terrain offers no exit yet, retain the anchor and periodically look again.
        if (delayed && now - anchor.getLong("exitAttempt") < 20) return;
        anchor.putLong("exitAttempt", now);
        Vec3 safe = null;
        if (anchor.contains("exitX")) {
            // Confirm the server position on a subsequent tick before discarding the anchor.
            if (clearAt(player, player.position())) {
                data.remove(ANCHOR);
                return;
            }
            if (now - anchor.getLong("exitSent") < 3) return;
            var lastExit = new Vec3(anchor.getDouble("exitX"), anchor.getDouble("exitY"), anchor.getDouble("exitZ"));
            if (clearAt(player, lastExit)) safe = lastExit;
        }
        boolean burrow = data.contains(BURROW_POWER);
        if (safe == null) safe = findBurrowExit(player, burrow);
        if (safe == null) return;
        if (safe.distanceToSqr(player.position()) > 1.0e-8) {
            anchor.putDouble("exitX", safe.x);
            anchor.putDouble("exitY", safe.y);
            anchor.putDouble("exitZ", safe.z);
            anchor.putLong("exitSent", now);
            player.connection.teleport(safe.x, safe.y, safe.z, player.getYRot(), player.getXRot());
        } else {
            data.remove(ANCHOR);
        }
        if (burrow) {
            float damage = data.getFloat(BURROW_POWER);
            data.remove(BURROW_POWER);
            impact(player, damage);
        }
    }

    @SubscribeEvent
    public static void preventSuffocation(LivingAttackEvent event) {
        if ((event.getSource().is(DamageTypes.IN_WALL) && (phasing(event.getEntity())
                || event.getEntity() instanceof Player recovering && settling(recovering)))
                || (event.getSource().getEntity() instanceof Player player && player.hasEffect(BOG_BURROW.get())))
            event.setCanceled(true);
    }

    private static boolean settling(Player player) {
        if (!player.getPersistentData().contains(ANCHOR)) return false;
        CompoundTag anchor = player.getPersistentData().getCompound(ANCHOR);
        return !anchor.contains("exitStarted") || player.level().getGameTime() - anchor.getLong("exitStarted") < 20;
    }

    @SubscribeEvent
    public static void preventBuriedAttack(AttackEntityEvent event) {
        if (event.getEntity().hasEffect(BOG_BURROW.get())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void hideBurrowingTarget(LivingChangeTargetEvent event) {
        if (event.getNewTarget() != null && event.getNewTarget().hasEffect(BOG_BURROW.get()))
            event.setNewTarget(null);
    }

    public static float boneHealth(Player player) {
        if (!BONE_HEALTH.isPresent()) return 0;
        AttributeInstance attr = player.getAttribute(BONE_HEALTH.get());
        return attr == null ? 0 : (float)attr.getBaseValue();
    }

    public static void spendBoneHealth(Player player, float damage) {
        float remaining = boneHealth(player);
        if (damage > 0 && remaining > 0) {
            player.getAttribute(BONE_HEALTH.get()).setBaseValue(Math.max(0, remaining - damage));
            if (damage >= remaining && player instanceof ServerPlayer) {
                FSNetwork.sendTracking(player, new S2CFortuneFx(FortuneIds.FxKind.WINGS_SHATTER, player.getId(), 0));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void healRedHearts(LivingHealEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.hasEffect(BONE_WINGS.get())) {
            float missing = player.getMaxHealth() - BONUS + boneHealth(player) - player.getHealth();
            event.setAmount(Math.min(event.getAmount(), Math.max(0, missing)));
        }
    }

    @SubscribeEvent
    public static void keepWingsOnEndReturn(PlayerEvent.Clone event) {
        if (event.isWasDeath() || !(event.getOriginal() instanceof ServerPlayer original)
                || !(event.getEntity() instanceof ServerPlayer next)) return;
        MobEffectInstance wings = original.getEffect(BONE_WINGS.get());
        float remaining = boneHealth(original);
        if (wings == null || remaining <= 0) return;
        next.getPersistentData().putBoolean(HAS_WINGS, true);
        next.getAttribute(BONE_HEALTH.get()).setBaseValue(remaining);
        next.addEffect(new MobEffectInstance(wings));
        next.getPersistentData().putBoolean(GRANTED_FLIGHT, original.getPersistentData().getBoolean(GRANTED_FLIGHT));
        next.getAbilities().mayfly = original.getAbilities().mayfly;
        next.getAbilities().flying = original.getAbilities().flying;
        next.setHealth(original.getHealth());
    }

    @SubscribeEvent
    public static void syncWingsAfterEndReturn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.isEndConquered() && event.getEntity() instanceof ServerPlayer player) {
            MobEffectInstance wings = player.getEffect(BONE_WINGS.get());
            if (wings != null) {
                player.connection.send(new ClientboundUpdateMobEffectPacket(player.getId(), wings));
                player.onUpdateAbilities();
            }
        }
    }

    private static void grantFlight(Player player) {
        if (!player.getAbilities().mayfly) {
            player.getPersistentData().putBoolean(GRANTED_FLIGHT, true);
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }
    }

    private static class MovementEffect extends MobEffect implements ISyncedMobEffect {
        MovementEffect(int color) { super(MobEffectCategory.BENEFICIAL, color); }

        @Override
        public List<ItemStack> getCurativeItems() { return List.of(); }

        @Override
        public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
            // The burrow speed bonus stays at 50% across all six levels.
            return modifier.getAmount();
        }
    }

    private static final class WingEffect extends MovementEffect {
        WingEffect() { super(0xAEB5BB); }

        @Override
        public void addAttributeModifiers(LivingEntity entity, AttributeMap map, int amplifier) {
            super.addAttributeModifiers(entity, map, amplifier);
            if (!(entity instanceof ServerPlayer player)) return;
            if (!player.getPersistentData().getBoolean(HAS_WINGS)) {
                player.getAttribute(BONE_HEALTH.get()).setBaseValue(BONUS);
                player.getPersistentData().putBoolean(HAS_WINGS, true);
                player.setHealth(player.getHealth() + BONUS);
                if (player.connection != null)
                    FSNetwork.sendTracking(player, new S2CFortuneFx(FortuneIds.FxKind.WINGS_UNFOLD, player.getId(), 0));
            }
            grantFlight(player);
        }

        @Override
        public void removeAttributeModifiers(LivingEntity entity, AttributeMap map, int amplifier) {
            if (entity instanceof ServerPlayer player) {
                float ordinary = Math.max(0, player.getHealth() - boneHealth(player));
                player.getPersistentData().remove(HAS_WINGS);
                player.getAttribute(BONE_HEALTH.get()).setBaseValue(0);
                player.setHealth(ordinary);
                if (player.isAlive()) player.getPersistentData().putBoolean(LANDED_WINGS, true);
                if (player.getPersistentData().getBoolean(GRANTED_FLIGHT)) {
                    player.getPersistentData().remove(GRANTED_FLIGHT);
                    if (!player.isCreative() && !player.isSpectator()) {
                        player.getAbilities().mayfly = false;
                        player.getAbilities().flying = false;
                        player.onUpdateAbilities();
                    }
                }
            }
            super.removeAttributeModifiers(entity, map, amplifier);
        }
    }
}
