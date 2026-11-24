package io.myf.fortune.mixin.client;

import io.myf.fortune.expansion.audience.StaffAttributes;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
abstract class StaffOrbitCullingMixin {
    @Inject(method = {"shouldRender", "m_5523_"}, at = @At("RETURN"), cancellable = true, remap = false)
    private void fortune$includeStaffSeals(Entity entity, Frustum frustum, double x, double y, double z,
                                          CallbackInfoReturnable<Boolean> visible) {
        if (visible.getReturnValueZ() || !(entity instanceof AbstractClientPlayer player)
                || !player.isAlive() || player.isSpectator() || player.isInvisible()
                || !StaffAttributes.heldBy(player) || !player.shouldRender(x, y, z)) return;

        // Include the outer dial and its back offset even as the wearer turns, crouches or swims.
        if (frustum.isVisible(player.getBoundingBoxForCulling().inflate(2.4))) visible.setReturnValue(true);
    }
}
