package io.myf.fortune.mixin;

import io.myf.fortune.expansion.audience.StaffAttributes;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "dev.xkmc.l2hostility.content.traits.common.FieryTrait", remap = false)
abstract class HostilityStaffFireMixin {
    @Redirect(method = {"onHurtTarget", "onAttackedByOthers"}, require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;m_20254_(I)V"))
    private void fortune$skipIgnition(LivingEntity target, int seconds) {
        if (!StaffAttributes.heldBy(target)) target.setSecondsOnFire(seconds);
    }

    @Redirect(method = {"onHurtTarget", "onAttackedByOthers"}, require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;setSecondsOnFire(I)V"))
    private void fortune$skipIgnitionDev(LivingEntity target, int seconds) {
        fortune$skipIgnition(target, seconds);
    }
}
