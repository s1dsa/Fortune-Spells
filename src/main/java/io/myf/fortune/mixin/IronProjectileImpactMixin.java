package io.myf.fortune.mixin;

import io.myf.fortune.combat.FortuneCombatHandler;
import io.redspace.ironsspellbooks.effect.AbyssalShroudEffect;
import io.redspace.ironsspellbooks.effect.EvasionEffect;
import io.redspace.ironsspellbooks.player.ServerPlayerEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ServerPlayerEvents.class, remap = false)
abstract class IronProjectileImpactMixin {
    @Redirect(method = "onProjectileImpact", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/effect/EvasionEffect;doEffect(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;)Z"))
    private static boolean fortuneSpells$checkImpactEvasion(LivingEntity target, DamageSource source) {
        return !FortuneCombatHandler.bypassImpactEvasion(target, source) && EvasionEffect.doEffect(target, source);
    }

    @Redirect(method = "onProjectileImpact", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/effect/AbyssalShroudEffect;doEffect(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;)Z"))
    private static boolean fortuneSpells$checkImpactShroud(LivingEntity target, DamageSource source) {
        return !FortuneCombatHandler.bypassImpactEvasion(target, source) && AbyssalShroudEffect.doEffect(target, source);
    }
}
