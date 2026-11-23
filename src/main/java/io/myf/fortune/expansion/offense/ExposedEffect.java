package io.myf.fortune.expansion.offense;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

public final class ExposedEffect extends MobEffect {
    private static final int MAX_LEVEL = 10;
    private static final int DURATION = 8 * 20;

    ExposedEffect() {
        super(MobEffectCategory.HARMFUL, 0xCD4859);
    }

    static void applySlash(LivingEntity target, LivingEntity caster) {
        MobEffect effect = OffensiveSpells.EXPOSED.get();
        int nextLevel = Math.min(MAX_LEVEL, level(target.getEffect(effect)) + 1);
        var instance = new MobEffectInstance(effect, DURATION, nextLevel - 1);
        if (target instanceof EnderDragon) target.forceAddEffect(instance, caster);
        else target.addEffect(instance, caster);
    }

    public static float damageMultiplier(LivingEntity target) {
        return 1 + 0.1F * level(target.getEffect(OffensiveSpells.EXPOSED.get()));
    }

    private static int level(MobEffectInstance effect) {
        return effect == null ? 0 : Math.min(MAX_LEVEL, effect.getAmplifier() + 1);
    }
}
