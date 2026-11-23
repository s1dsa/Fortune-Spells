package io.myf.fortune.mixin;

import io.myf.fortune.combat.FortuneCombatHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = ForgeHooks.class, remap = false)
abstract class ForgeDamageEventsMixin {
    @ModifyVariable(method = "onLivingHurt", at = @At("HEAD"), argsOnly = true)
    private static float fortuneSpells$bossFormula(float amount, LivingEntity target, DamageSource source, float original) {
        return FortuneCombatHandler.restoreChosenBossHit(target, source, amount);
    }

    // Only an uncanceled final damage event reaches this read.
    @Inject(method = "onLivingDamage", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/event/entity/living/LivingDamageEvent;getAmount()F"),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private static void fortuneSpells$afterDamage(LivingEntity target, DamageSource source, float amount,
                                                 CallbackInfoReturnable<Float> callback, LivingDamageEvent event) {
        FortuneCombatHandler.afterDamage(event);
    }
}
