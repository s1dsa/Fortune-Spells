package io.myf.fortune.mixin;

import io.myf.fortune.combat.FortuneCombatHandler;
import io.myf.fortune.expansion.offense.ExposedEffect;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    // forceAddEffect still checks immunity. Permit only the sword dance's mark.
    @Redirect(method = "forceAddEffect", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;canBeAffected(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private boolean fortuneSpells$allowExposed(LivingEntity entity, MobEffectInstance effect) {
        return effect.getEffect() instanceof ExposedEffect || entity.canBeAffected(effect);
    }

    // Both player and mob damage pass here after armor and enchantments, before absorption.
    @Inject(method = {"getDamageAfterMagicAbsorb", "m_6515_"}, at = @At("RETURN"), cancellable = true, remap = false)
    private void fortuneSpells$exposeWounds(DamageSource source, float amount, CallbackInfoReturnable<Float> result) {
        float multiplier = ExposedEffect.damageMultiplier((LivingEntity) (Object) this);
        if (multiplier > 1) result.setReturnValue(result.getReturnValueF() * multiplier);
    }

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
    private DamageSource fortuneSpells$selectSource(DamageSource source, DamageSource original, float amount) {
        LivingEntity target = (LivingEntity) (Object) this;
        // Player.hurt posts the attack event before calling super.hurt.
        return target instanceof Player ? source : FortuneCombatHandler.selectSource(target, source, amount);
    }

    @Invoker("actuallyHurt")
    protected abstract void fortuneSpells$applyNativeDamage(DamageSource source, float amount);

    @Redirect(method = "hurt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V"),
            require = 2, allow = 2)
    private void fortuneSpells$applyDamage(LivingEntity target, DamageSource source, float amount) {
        FortuneCombatHandler.apply(target, source, amount, this::fortuneSpells$applyNativeDamage);
    }
}
