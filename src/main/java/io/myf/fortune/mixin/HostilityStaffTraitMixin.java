package io.myf.fortune.mixin;

import io.myf.fortune.expansion.audience.StaffAttributes;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "dev.xkmc.l2hostility.content.traits.base.MobTrait", remap = false)
abstract class HostilityStaffTraitMixin {
    @Shadow public abstract void postHurtImpl(int level, LivingEntity attacker, LivingEntity target);

    @Redirect(method = "postHurtPlayer", at = @At(value = "INVOKE",
            target = "Ldev/xkmc/l2hostility/content/traits/base/MobTrait;postHurtImpl(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void fortune$applyUnwarded(@Coerce Object trait, int level, LivingEntity attacker, LivingEntity target) {
        if (!StaffAttributes.heldBy(target)) postHurtImpl(level, attacker, target);
    }
}
