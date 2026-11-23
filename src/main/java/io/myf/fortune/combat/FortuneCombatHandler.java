package io.myf.fortune.combat;

import io.myf.fortune.FortuneIds;
import io.myf.fortune.FortuneTuning;
import io.myf.fortune.expansion.offense.ExposedEffect;
import io.myf.fortune.mixin.BossProjectileAccessor;
import io.myf.fortune.network.FSNetwork;
import io.myf.fortune.network.S2CFortuneFx;
import io.myf.fortune.registry.FSDamageTypes;
import io.myf.fortune.registry.FSEffects;
import io.myf.fortune.registry.FSSounds;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FortuneIds.MODID)
public final class FortuneCombatHandler {
    private static final ThreadLocal<Hit> ACTIVE = new ThreadLocal<>();
    private static final Map<Impact, Boolean> PROJECTILE_ROLLS = new HashMap<>();

    private FortuneCombatHandler() {}

    private record Impact(Entity projectile, LivingEntity target) {}

    private static final class Hit {
        final Hit parent;
        final LivingEntity target;
        final DamageSource source;
        final LivingEntity attacker;
        final MobEffectInstance allIn;
        final boolean fortune;
        final float intended;
        final float absorption;
        float healthDamage;
        float healthBeforeDamage;

        Hit(Hit parent, LivingEntity target, DamageSource source, LivingEntity attacker,
            MobEffectInstance allIn, boolean fortune, float intended) {
            this.parent = parent;
            this.target = target;
            this.source = source;
            this.attacker = attacker;
            this.allIn = allIn;
            this.fortune = fortune;
            this.intended = intended;
            this.absorption = target.getAbsorptionAmount();
        }
    }

    public interface BossInput {
        float fortuneSpells$originalDamage(DamageSource source, float amount);
        void fortuneSpells$setExactHealth(float health);
    }

    @FunctionalInterface
    public interface ApplyDamage {
        void apply(DamageSource source, float amount);
    }

    // Iron's checks Evasion at projectile impact, before the projectile even calls hurt().
    public static boolean bypassImpactEvasion(LivingEntity target, DamageSource impactSource) {
        Entity projectile = impactSource.getDirectEntity();
        if (target.level().isClientSide || !(impactSource.getEntity() instanceof LivingEntity attacker)
                || attacker == target || !attacker.hasEffect(FSEffects.BLESSED_BY_FORTUNE.get())
                || !hasImpactDamage(projectile, target)) return false;
        Impact impact = new Impact(projectile, target);
        return PROJECTILE_ROLLS.computeIfAbsent(impact, ignored -> roll(attacker));
    }

    // Iron's constructs an indirectMagic source even for harmless projectiles. Only roll early
    // for damaging impacts; ordinary hurt() still handles damage from later effects and areas.
    private static boolean hasImpactDamage(Entity projectile, LivingEntity target) {
        if (projectile instanceof AbstractArrow arrow) {
            return arrow.getBaseDamage() > 0 && arrow.getDeltaMovement().lengthSqr() > 0;
        }
        if (projectile instanceof AbstractMagicProjectile spell) {
            // Stationary spell controllers deal damage in their own tick. Iron's snowball uses
            // its damage field as a duration for Chilled, rather than dealing impact damage.
            return spell.getDamage() > 0 && spell.getDeltaMovement().lengthSqr() > 0
                    && !(spell instanceof io.redspace.ironsspellbooks.entity.spells.snowball.Snowball);
        }
        if (projectile instanceof ThrownPotion potion) {
            var item = potion.getItem();
            var effects = PotionUtils.getMobEffects(item);
            if (effects.isEmpty()) return PotionUtils.getPotion(item) == Potions.WATER && target.isSensitiveToWater();
            var damagingEffect = target.isInvertedHealAndHarm() ? MobEffects.HEAL : MobEffects.HARM;
            return item.is(Items.SPLASH_POTION) && target.isAffectedByPotions()
                    && effects.stream().anyMatch(effect -> effect.getEffect() == damagingEffect);
        }
        if (projectile instanceof FireworkRocketEntity rocket) {
            var fireworks = rocket.getItem().getTagElement(FireworkRocketItem.TAG_FIREWORKS);
            return fireworks != null && !fireworks.getList(FireworkRocketItem.TAG_EXPLOSIONS, Tag.TAG_COMPOUND).isEmpty();
        }
        // Meet Your Fight also posts impact events while its boss projectiles are positioning.
        if (projectile instanceof BossProjectileAccessor bossProjectile) return bossProjectile.fortuneSpells$hasFired();
        return (projectile instanceof AbstractHurtingProjectile && !(projectile instanceof DragonFireball))
                || projectile instanceof ShulkerBullet || projectile instanceof LlamaSpit
                || (projectile instanceof Snowball && target instanceof Blaze);
    }

    public static DamageSource selectSource(LivingEntity target, DamageSource original, float amount) {
        if (target.level().isClientSide || amount <= 0 || original.is(FSDamageTypes.FORTUNE)
                || original.is(DamageTypes.THORNS) || !(original.getEntity() instanceof LivingEntity attacker)
                || attacker == target || !attacker.hasEffect(FSEffects.BLESSED_BY_FORTUNE.get())) return original;
        Boolean impactRoll = PROJECTILE_ROLLS.isEmpty() ? null
                : PROJECTILE_ROLLS.remove(new Impact(original.getDirectEntity(), target));
        if (!(impactRoll != null ? impactRoll : roll(attacker))) return original;
        // For a stronger hit during hurt resistance vanilla applies only the difference.
        // Retain the unmodified amount exclusively when compensating a boss override.
        float incoming = 0;
        if (target instanceof BossInput boss
                && (target.invulnerableTime <= 10 || original.is(DamageTypeTags.BYPASSES_COOLDOWN))) {
            incoming = boss.fortuneSpells$originalDamage(original, amount);
        }
        return FSDamageTypes.fortune(target.level(), attacker, original, incoming);
    }

    private static boolean roll(LivingEntity attacker) {
        return attacker.getRandom().nextFloat() < FortuneTuning.FATE_CHANCE;
    }

    @SubscribeEvent
    public static void clearUnusedProjectileRolls(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) PROJECTILE_ROLLS.clear();
    }

    // Wrap just the native actuallyHurt invocation. Original return, death, and totem paths run once.
    public static void apply(LivingEntity target, DamageSource source, float amount, ApplyDamage nativeDamage) {
        Hit parent = ACTIVE.get();
        Hit hit = null;
        if (!target.level().isClientSide && amount > 0 && source.getEntity() instanceof LivingEntity attacker
                && attacker != target) {
            boolean fortune = source instanceof FSDamageTypes.FortuneStrike;
            MobEffectInstance allIn = attacker.getEffect(FSEffects.ALL_IN.get());
            if ((!fortune && source.is(FSDamageTypes.FORTUNE)) || reserved(parent, allIn)) allIn = null;
            if (fortune || allIn != null) {
                float intended = source instanceof FSDamageTypes.FortuneStrike strike ? Math.max(amount, strike.incoming) : amount;
                if (allIn != null) intended *= FortuneTuning.DOUBLE_DAMAGE;
                if (fortune) intended *= FortuneTuning.DOUBLE_DAMAGE;
                hit = new Hit(parent, target, source, attacker, allIn, fortune, intended);
            }
        }
        // A nested vanilla hit may occur inside an event and reuse the same DamageSource object.
        if (hit == null && parent == null) {
            nativeDamage.apply(source, amount);
            return;
        }
        ACTIVE.set(hit == null ? new Hit(parent, target, source, null, null, false, amount) : hit);
        try {
            nativeDamage.apply(source, hit == null ? amount : hit.intended);
            if (hit != null && hit.healthDamage > 0 && target.getHealth() < hit.healthBeforeDamage) {
                // Some bosses cap the amount after Forge's final damage callback, or in setHealth.
                // Only repair a landed hit, before vanilla handles totems and death.
                float uncapped = Math.max(0, hit.healthBeforeDamage - hit.healthDamage);
                if (target.getHealth() > uncapped) StaffSpellDamage.setHealth(target, uncapped);
            }
        } finally {
            if (parent == null) ACTIVE.remove();
            else ACTIVE.set(parent);
        }
    }

    private static boolean reserved(Hit hit, MobEffectInstance effect) {
        if (effect == null) return false;
        for (; hit != null; hit = hit.parent) {
            if (hit.allIn == effect) return true;
        }
        return false;
    }

    /** Goety's actuallyHurt overrides have adjusted the hit by the time it reaches Forge. */
    public static float restoreChosenBossHit(LivingEntity target, DamageSource source, float reduced) {
        Hit hit = ACTIVE.get();
        if (reduced <= 0 || hit == null || !hit.fortune || !(target instanceof BossInput)
                || hit.target != target || hit.source != source) return reduced;
        return hit.intended;
    }

    public static void afterDamage(LivingDamageEvent event) {
        Hit hit = ACTIVE.get();
        if (hit == null || hit.target != event.getEntity() || hit.source != event.getSource()) return;
        float finalAmount = event.getAmount();
        if (finalAmount <= 0) return;
        if (hit.fortune) {
            // This is reached only if the attack cleared the boss's own invulnerability gates.
            // Absorption is consumed before onLivingDamage; a cap can occur on either side of it.
            float floor = hit.intended * ExposedEffect.damageMultiplier(hit.target);
            hit.healthDamage = Math.max(finalAmount, floor - hit.absorption);
            hit.healthBeforeDamage = hit.target.getHealth();
        }
        if (hit.allIn != null && hit.attacker.getEffect(FSEffects.ALL_IN.get()) == hit.allIn) {
            hit.attacker.removeEffect(FSEffects.ALL_IN.get());
            strike(hit.target, FortuneIds.FxKind.ALL_IN_STRIKE, finalAmount, FSSounds.ALL_IN_PROC.get(),
                    FortuneTuning.ALL_IN_STRIKE_PITCH);
        }
        if (hit.fortune) {
            strike(hit.target, FortuneIds.FxKind.FATE_STRIKE, finalAmount, FSSounds.FATE_STRIKE.get(),
                    FortuneTuning.FATE_STRIKE_PITCH);
        }
    }

    private static void strike(LivingEntity target, byte kind, float amount, SoundEvent sound, float pitch) {
        FSNetwork.sendTracking(target, new S2CFortuneFx(kind, target.getId(), amount));
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), sound, target.getSoundSource(), 1.0f, pitch);
    }
}
