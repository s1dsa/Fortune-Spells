package io.myf.fortune.mixin.client;

import io.myf.fortune.client.audience.StaffCinematic;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
abstract class StaffProjectionMixin {
    @Inject(method = {"getDepthFar", "m_172790_"}, at = @At("RETURN"), cancellable = true, remap = false)
    private void fortune$keepFarRings(CallbackInfoReturnable<Float> result) {
        if (StaffCinematic.wideView()) result.setReturnValue(Math.max(6000, result.getReturnValueF()));
    }
}
