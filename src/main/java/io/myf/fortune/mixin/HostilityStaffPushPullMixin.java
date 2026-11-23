package io.myf.fortune.mixin;

import io.myf.fortune.expansion.audience.StaffAttributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.xkmc.l2hostility.content.traits.legendary.PushPullTrait", remap = false)
abstract class HostilityStaffPushPullMixin {
    @Inject(method = "lambda$tick$0", at = @At("HEAD"), cancellable = true, require = 0)
    private static void fortune$skipHeldStaff(Player target, CallbackInfoReturnable<Boolean> result) {
        if (StaffAttributes.heldBy(target)) result.setReturnValue(false);
    }

    @Inject(method = "lambda$tick$1", at = @At("HEAD"), cancellable = true, require = 0)
    private static void fortune$skipHeldStaff(LivingEntity source, LivingEntity target, CallbackInfoReturnable<Boolean> result) {
        if (StaffAttributes.heldBy(target)) result.setReturnValue(false);
    }
}
