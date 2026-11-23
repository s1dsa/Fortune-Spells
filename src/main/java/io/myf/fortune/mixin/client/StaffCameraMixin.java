package io.myf.fortune.mixin.client;

import io.myf.fortune.client.audience.StaffCinematic;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
abstract class StaffCameraMixin {
    @Inject(method = {"setup", "m_90575_"}, at = @At("RETURN"), remap = false)
    private void fortune$placeCamera(BlockGetter world, Entity entity, boolean thirdPerson, boolean reverse, float partial, CallbackInfo ci) {
        StaffCinematic.place((Camera)(Object)this, partial);
    }

    @Inject(method = {"isDetached", "m_90594_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void fortune$showCaster(CallbackInfoReturnable<Boolean> ci) {
        if (StaffCinematic.active()) ci.setReturnValue(true);
    }
}
