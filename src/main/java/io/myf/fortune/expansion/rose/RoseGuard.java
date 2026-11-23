package io.myf.fortune.expansion.rose;

import io.myf.fortune.FortuneIds;
import io.myf.fortune.expansion.SpellTargets;
import io.myf.fortune.expansion.offense.OffensiveSpells;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = FortuneIds.MODID)
public final class RoseGuard {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FortuneIds.MODID);
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, FortuneIds.MODID);
    public static final RegistryObject<EntityType<RoseWraith>> WRAITH = ENTITIES.register("rose_wraith", () ->
            EntityType.Builder.<RoseWraith>of(RoseWraith::new, MobCategory.MISC).sized(.75F,1.3125F)
                    .clientTrackingRange(8).updateInterval(1).noSave().noSummon().build("fortune_spells:rose_wraith"));
    private static final RegistryObject<MobEffect> GUARD = EFFECTS.register("parry_stance",
            () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xD64A77) {
                @Override public java.util.List<ItemStack> getCurativeItems() { return java.util.List.of(); }
                @Override public boolean isDurationEffectTick(int duration, int amplifier) { return duration % 3 == 0; }
                @Override public void applyEffectTick(LivingEntity owner, int amplifier) {
                    if (!(owner.level() instanceof ServerLevel world)) return;
                    for (int i = 0; i < 12; i++) {
                        double angle = owner.tickCount * .13 + i * Math.PI / 6;
                        world.sendParticles(THORN, owner.getX()+Math.cos(angle)*.85,
                                owner.getY()+.85+.22*Math.sin(3*angle), owner.getZ()+Math.sin(angle)*.85,
                                1, 0, 0, 0, 0);
                    }
                }
            });
    private static final DustParticleOptions THORN = new DustParticleOptions(new Vector3f(.82F,.1F,.22F),.8F);
    private static final int GUARD_TICKS = 60;
    // A pair shares a single guard, including its start, parry consumption and cancellation.
    private static final Map<LivingEntity, List<RoseWraith>> FORMATIONS = new WeakHashMap<>();

    private RoseGuard() {}
    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        EFFECTS.register(bus);
        bus.addListener(RoseGuard::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(WRAITH.get(), RoseWraith.createAttributes().build());
    }

    public static void cast(LivingEntity caster, float power) {
        if (!(caster.level() instanceof ServerLevel world)) return;
        int count = 1 + world.random.nextInt(2);
        List<RoseWraith> spirits = new ArrayList<>(count);
        for (int i = 0; i < count; ++i) {
            RoseWraith wraith = new RoseWraith(WRAITH.get(), world);
            wraith.bind(caster, power, i == 0 ? 1 : -1);
            if (world.addFreshEntity(wraith)) spirits.add(wraith);
        }
        if (!spirits.isEmpty()) {
            List<RoseWraith> previous = FORMATIONS.put(caster, spirits);
            if (previous != null) {
                caster.removeEffect(GUARD.get());
                previous.forEach(Entity::discard);
            }
        }
    }

    static void appear(LivingEntity caster, RoseWraith spirit) {
        List<RoseWraith> spirits = FORMATIONS.get(caster);
        if (spirits == null || !spirits.contains(spirit)) {
            spirit.discard();
            return;
        }
        long endTick = caster.level().getGameTime() + GUARD_TICKS;
        spirits.forEach(wraith -> wraith.guard(endTick));
        // An owner ticking after its spirits can decrement the display effect on the appearance tick.
        caster.addEffect(new MobEffectInstance(GUARD.get(), GUARD_TICKS + 1));
    }

    static boolean isGuarding(LivingEntity caster) {
        return caster.hasEffect(GUARD.get());
    }

    static void release(LivingEntity caster) {
        List<RoseWraith> spirits = FORMATIONS.remove(caster);
        caster.removeEffect(GUARD.get());
        if (spirits != null) spirits.forEach(RoseWraith::seek);
    }

    static void onDepart(LivingEntity owner, RoseWraith spirit) {
        List<RoseWraith> spirits = FORMATIONS.get(owner);
        if (spirits != null && spirits.remove(spirit) && spirits.isEmpty()) {
            FORMATIONS.remove(owner);
            owner.removeEffect(GUARD.get());
        }
    }

    @SubscribeEvent
    public static void ownerLeaves(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof LivingEntity owner)) return;
        List<RoseWraith> spirits = FORMATIONS.remove(owner);
        if (spirits != null) {
            owner.removeEffect(GUARD.get());
            spirits.forEach(Entity::discard);
        }
    }

    @SubscribeEvent
    public static void parry(LivingHurtEvent event) {
        LivingEntity caster = event.getEntity();
        if (!(caster.level() instanceof ServerLevel world) || !caster.hasEffect(GUARD.get()) || event.getAmount() <= 0) return;
        var source = event.getSource();
        if (!(source.getDirectEntity() instanceof LivingEntity attacker) || source.getEntity() != attacker
                || attacker == caster || !(source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK)
                    || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO) || source.is(DamageTypes.STING))
                || caster.distanceToSqr(attacker) > 36) return;
        var spirits = FORMATIONS.get(caster);
        if (spirits == null) return;
        RoseWraith guard = spirits.stream().filter(w -> w.isAlive() && w.phase() == RoseWraith.Phase.GUARDING)
                .findFirst().orElse(null);
        if (guard == null) return;
        if (guard.guardExpired()) {
            release(caster);
            return;
        }
        release(caster);
        event.setAmount(event.getAmount() * .2F);
        world.playSound(null,caster.blockPosition(),SoundEvents.SHIELD_BLOCK,SoundSource.PLAYERS,1F,1.3F);
        world.sendParticles(ParticleTypes.CHERRY_LEAVES,caster.getX(),caster.getY()+1,caster.getZ(),28,.7,.35,.7,.12);
        if (SpellTargets.canHit(caster, attacker)) {
            attacker.hurt(caster.damageSources().indirectMagic(guard, caster), guard.counterDamage());
            OffensiveSpells.stun(attacker, 20);
        }
    }
}
