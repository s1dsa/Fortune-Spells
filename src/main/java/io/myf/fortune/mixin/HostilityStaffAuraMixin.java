package io.myf.fortune.mixin;

import io.myf.fortune.expansion.audience.StaffAttributes;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.xkmc.l2hostility.content.traits.common.AuraEffectTrait", remap = false)
abstract class HostilityStaffAuraMixin {
    @Inject(method = "canApply", at = @At("HEAD"), cancellable = true)
    private void fortune$wardAura(LivingEntity target, CallbackInfoReturnable<Boolean> result) {
        if (StaffAttributes.heldBy(target)) result.setReturnValue(false);
    }
}
