package io.myf.fortune.mixin;

import io.myf.fortune.expansion.audience.StaffAttributes;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.xkmc.l2hostility.content.traits.legendary.RagnarokTrait", remap = false)
abstract class HostilityStaffSealMixin {
    @Inject(method = "sealItems", at = @At("HEAD"), cancellable = true)
    private void fortune$wardEquipment(int level, LivingEntity target, CallbackInfo ci) {
        if (StaffAttributes.heldBy(target)) ci.cancel();
    }
}
