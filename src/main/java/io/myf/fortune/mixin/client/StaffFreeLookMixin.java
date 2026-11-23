package io.myf.fortune.mixin.client;

import io.myf.fortune.client.audience.StaffCinematic;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
abstract class StaffFreeLookMixin {
    @Inject(method = {"turn", "m_19884_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void fortune$turnDetachedCamera(double dx, double dy, CallbackInfo ci) {
        if ((Object)this == Minecraft.getInstance().player && StaffCinematic.turnFreeCamera(dx, dy)) ci.cancel();
    }
}
