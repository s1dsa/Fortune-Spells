package io.myf.fortune.mixin;

import io.myf.fortune.registry.FSDamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = {LivingHurtEvent.class, LivingDamageEvent.class}, remap = false)
abstract class FortuneEventAmountMixin {
    @Shadow(remap = false) public abstract DamageSource getSource();
    @Shadow(remap = false) public abstract float getAmount();

    // Each listener sees the last accepted amount, even if a cap ran before its multiplier.
    @ModifyVariable(method = "setAmount(F)V", at = @At("HEAD"), argsOnly = true)
    private float fortuneSpells$keepDamageIncreases(float amount) {
        return getSource() instanceof FSDamageTypes.FortuneStrike ? Math.max(getAmount(), amount) : amount;
    }
}
