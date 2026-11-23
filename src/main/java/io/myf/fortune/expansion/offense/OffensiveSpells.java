package io.myf.fortune.expansion.offense;

import io.myf.fortune.FortuneIds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.joml.Vector3f;

public final class OffensiveSpells {
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FortuneIds.MODID);
    private static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, FortuneIds.MODID);
    public static final RegistryObject<EntityType<SpellFigure>> FIGURE = ENTITIES.register("spell_figure",
            () -> EntityType.Builder.<SpellFigure>of(SpellFigure::new, MobCategory.MISC)
                    .sized(.25f, .25f).clientTrackingRange(12).updateInterval(1).noSave().noSummon()
                    .build(FortuneIds.id("spell_figure").toString()));
    public static final RegistryObject<MobEffect> DECAY = EFFECTS.register("decay", () ->
            new DecayEffect().addAttributeModifier(Attributes.ATTACK_DAMAGE,
                    "ac65a1b6-6482-4efc-a761-500942994f19", -.30, AttributeModifier.Operation.MULTIPLY_TOTAL));
    public static final RegistryObject<MobEffect> STUNNED = EFFECTS.register("stunned", () ->
            new StunnedEffect().addAttributeModifier(Attributes.MOVEMENT_SPEED,
                    "3fa291fe-c107-4fc2-8dc1-5d2c87c11f39", -1, AttributeModifier.Operation.MULTIPLY_TOTAL));
    static final RegistryObject<MobEffect> EXPOSED = EFFECTS.register("exposed", ExposedEffect::new);
    static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(.50f,.89f,.85f), 1.2f);
    private static final java.util.Map<LivingEntity,SpellFigure> RITUALS = new java.util.WeakHashMap<>();
    // Wing circles are visual only. They must never participate in the anchored spells' release checks.
    private static final java.util.Map<LivingEntity,SpellFigure> WING_CHANTS = new java.util.WeakHashMap<>();

    private OffensiveSpells() {}

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        EFFECTS.register(bus);
    }

    public static void releaseWingHalo(ServerPlayer caster) {
        SpellFigure chant = WING_CHANTS.get(caster);
        SpellFigure.spawn(caster.serverLevel(), SpellFigure.Kind.WING_HALO, caster, caster.position(),
                caster.getLookAngle(), 0, 0, 1, chant != null && chant.empowered());
    }

    public static void cast(String path, int level, LivingEntity caster, float power, float maxMana) {
        cast(path,level,caster,power,maxMana,false);
    }

    public static void cast(String path, int level, LivingEntity caster, float power, float maxMana, boolean empowered) {
        if (!(caster.level() instanceof ServerLevel world)) return;
        SpellFigure.Kind kind = switch (path) {
            case "croc_devour" -> SpellFigure.Kind.JAWS;
            case "phantom_belfry" -> SpellFigure.Kind.TOWER;
            case "bell_shockwave" -> SpellFigure.Kind.CHIME;
            case "sunset_blade_dance" -> SpellFigure.Kind.DANCE;
            default -> null;
        };
        if (kind == null) return;
        boolean grandRitual = kind == SpellFigure.Kind.JAWS || kind == SpellFigure.Kind.TOWER;
        SpellFigure ritual = grandRitual ? RITUALS.get(caster) : null;
        if (ritual != null && !canRelease(path,caster,true)) {
            interruptChant(caster);
            return;
        }
        Vec3 origin = kind == SpellFigure.Kind.CHIME ? caster.getEyePosition() : caster.position();
        if (ritual != null) {
            origin = ritual.position();
            empowered = ritual.empowered();
        }
        SpellFigure figure;
        if (ritual != null && kind == SpellFigure.Kind.JAWS) {
            // The gate is already locked. Publish the same yaw in the jaw's first tracking packet.
            float yaw = ritual.getYRot();
            figure = SpellFigure.spawn(world, kind, caster, origin, Vec3.directionFromRotation(0, yaw),
                    yaw, power, maxMana, level, empowered);
        } else {
            figure = SpellFigure.spawn(world, kind, caster, origin, caster.getLookAngle(), power, maxMana, level, empowered);
        }
        if (grandRitual) {
            if (ritual != null) ritual.lightRune();
            else figure.releaseSound();
        } else if (kind == SpellFigure.Kind.DANCE) {
            OffensiveControl.beginDance(caster,figure);
        }
    }

    public static boolean canRelease(String path, LivingEntity caster, boolean chanting) {
        SpellFigure ritual = RITUALS.get(caster);
        // Iron's immediate command casts have no chant. A real long cast must retain its original circle.
        if (ritual == null) return !chanting;
        SpellFigure.Kind expected = path.equals("croc_devour") ? SpellFigure.Kind.SWAMP_RUNE : SpellFigure.Kind.BELL_RUNE;
        return !ritual.isRemoved() && ritual.runePhase()==SpellFigure.RunePhase.DRAWING
                && ritual.kind()==expected && ritual.level()==caster.level() && ritual.distanceToSqr(caster)<=4;
    }

    public static void beginChant(String path, LivingEntity caster) {
        beginChant(path,caster,false);
    }

    public static void beginChant(String path, LivingEntity caster, boolean empowered) {
        if (!(caster.level() instanceof ServerLevel world)) return;
        if (path.equals("bone_wings")) {
            endWingChant(caster);
            SpellFigure wing = SpellFigure.spawn(world,SpellFigure.Kind.WING_RUNE,caster,caster.position(),caster.getLookAngle(),0,0,1,empowered);
            WING_CHANTS.put(caster,wing);
            return;
        }
        interruptChant(caster);
        SpellFigure.Kind kind = path.equals("croc_devour") ? SpellFigure.Kind.SWAMP_RUNE : SpellFigure.Kind.BELL_RUNE;
        SpellFigure ritual = SpellFigure.spawn(world,kind,caster,caster.position(),caster.getLookAngle(),0,0,1,empowered);
        RITUALS.put(caster,ritual);
        ritual.startRune();
    }

    public static void chant(String path, LivingEntity caster, int elapsedTicks) {
        SpellFigure ritual = (path.equals("bone_wings") ? WING_CHANTS : RITUALS).get(caster);
        if (ritual!=null && ritual.level()==caster.level()) ritual.traceRune(elapsedTicks);
    }

    public static void endWingChant(LivingEntity caster) {
        SpellFigure wing = WING_CHANTS.remove(caster);
        if (wing != null && !wing.isRemoved()) wing.discard();
    }

    public static void interruptChant(LivingEntity caster) {
        SpellFigure rune = RITUALS.remove(caster);
        if (rune != null && !rune.isRemoved()) rune.shatterRune();
    }

    static void runePoint(ServerLevel world, Vec3 origin, DustParticleOptions dust, double angle, double radius) {
        world.sendParticles(dust,origin.x + radius*Math.cos(angle),origin.y + .10,
                origin.z + radius*Math.sin(angle),1,0,0,0,0);
    }

    static void forgetRitual(SpellFigure figure) {
        (figure.kind() == SpellFigure.Kind.WING_RUNE ? WING_CHANTS : RITUALS).values().remove(figure);
    }

    public static void stun(LivingEntity target, int ticks) {
        var stun = new MobEffectInstance(STUNNED.get(), ticks);
        if (target instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) {
            // The dragon overrides addEffect to reject every effect. The bell explicitly stuns bosses.
            var current = target.getEffect(STUNNED.get());
            if (current == null || current.getDuration() < ticks) target.forceAddEffect(stun, null);
        } else {
            target.addEffect(stun);
        }
    }

    private static final class DecayEffect extends MobEffect {
        private DecayEffect() { super(MobEffectCategory.HARMFUL, 0x69865B); }
        @Override public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
            // Both versions weaken attacks by 30%. The staff only doubles the periodic damage.
            return super.getAttributeModifierValue(amplifier == 1 ? 0 : amplifier, modifier);
        }
        @Override public boolean isDurationEffectTick(int duration, int amplifier) { return duration % 20 == 1; }
        @Override public void applyEffectTick(LivingEntity entity, int amplifier) {
            if (entity.level().isClientSide) return;
            if (amplifier == 1) {
                io.myf.fortune.combat.StaffSpellDamage.apply(entity, entity.damageSources().magic(), 2);
                return;
            }
            // Rot respects an existing hurt cooldown. Its own tick must not shield the
            // creature from the next bite of the same devour.
            int previousCooldown = entity.invulnerableTime;
            if (entity.hurt(entity.damageSources().magic(), 1f))
                entity.invulnerableTime = Math.min(entity.invulnerableTime, previousCooldown);
        }
    }

    private static final class StunnedEffect extends MobEffect {
        private StunnedEffect() { super(MobEffectCategory.HARMFUL, 0xE3C989); }
    }
}
