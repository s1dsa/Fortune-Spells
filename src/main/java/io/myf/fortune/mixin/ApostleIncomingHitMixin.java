package io.myf.fortune.mixin;

import io.myf.fortune.combat.FortuneCombatHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Goety scales incoming hits in its hurt override before calling LivingEntity.hurt. */
@Pseudo
@Mixin(targets = "com.Polarice3.Goety.common.entities.boss.Apostle", remap = false, priority = 1100)
abstract class ApostleIncomingHitMixin implements FortuneCombatHandler.BossInput {
    @Unique private DamageSource fortuneSpells$source;
    @Unique private float fortuneSpells$incoming;
    @Unique private boolean fortuneSpells$writingHealth;
    @Unique private float fortuneSpells$exactHealth;

    @Inject(method = {"hurt", "m_6469_"}, at = @At("HEAD"), remap = false)
    private void fortuneSpells$remember(DamageSource source, float amount, CallbackInfoReturnable<Boolean> ci) {
        fortuneSpells$source = source;
        fortuneSpells$incoming = amount;
    }

    @Inject(method = {"hurt", "m_6469_"}, at = @At("RETURN"), remap = false)
    private void fortuneSpells$clear(DamageSource source, float amount, CallbackInfoReturnable<Boolean> ci) {
        fortuneSpells$source = null;
    }

    @Override public float fortuneSpells$originalDamage(DamageSource source, float amount) {
        return fortuneSpells$source == source ? Math.max(amount, fortuneSpells$incoming) : amount;
    }

    @Override public void fortuneSpells$setExactHealth(float health) {
        boolean previousWrite = fortuneSpells$writingHealth;
        float previousHealth = fortuneSpells$exactHealth;
        fortuneSpells$writingHealth = true;
        fortuneSpells$exactHealth = health;
        try {
            ((LivingEntity) (Object) this).setHealth(health);
        } finally {
            fortuneSpells$writingHealth = previousWrite;
            fortuneSpells$exactHealth = previousHealth;
        }
    }

    // Both RevelationFix versions keep the requested health in argument 1 and replace it
    // with their capped value. Feed the exact result to their own update/sync path.
    @ModifyVariable(method = {"setHealth", "m_21153_"}, at = @At("LOAD"), index = 1, remap = false, require = 0)
    private float fortuneSpells$uncappedHealth(float value) {
        return fortuneSpells$writingHealth ? fortuneSpells$exactHealth : value;
    }
}
