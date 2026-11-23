package io.myf.fortune.mixin.client;

import io.myf.fortune.expansion.movement.MovementSpells;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
abstract class PhasePushMixin {
    @Inject(method = {"moveTowardsClosestSpace", "m_108704_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void fortuneSpells$allowWallTraversal(double x, double z, CallbackInfo ci) {
        if (MovementSpells.phasing((LocalPlayer)(Object)this)) ci.cancel();
    }
}
