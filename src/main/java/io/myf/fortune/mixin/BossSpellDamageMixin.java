package io.myf.fortune.mixin;

import io.myf.fortune.expansion.BossSpell;
import io.myf.fortune.expansion.audience.CataclysmSpell;
import io.myf.fortune.expansion.SpellTargets;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = DamageSources.class, remap = false)
abstract class BossSpellDamageMixin {
    @Redirect(method = "applyDamage", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/damage/DamageSources;isFriendlyFireBetween(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)Z"),
            allow = 1)
    private static boolean fortuneSpells$rejectTarget(Entity caster, Entity target,
                                                      Entity hit, float amount, DamageSource source) {
        if (source instanceof SpellDamageSource spell && (spell.spell() instanceof BossSpell || spell.spell() instanceof CataclysmSpell)
                && caster instanceof LivingEntity owner && target instanceof LivingEntity victim) {
            return !SpellTargets.canHit(owner, victim);
        }
        return DamageSources.isFriendlyFireBetween(caster, target);
    }
}
