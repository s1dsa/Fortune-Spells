package io.myf.fortune.mixin.client;

import io.myf.fortune.client.audience.StaffCinematic;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientMagicData.class, remap = false)
abstract class StaffCastViewMixin {
    @Inject(method = "setClientCastState", at = @At("RETURN"))
    private static void fortune$staffCast(String spell, int level, int ticks, CastSource source, String slot, CallbackInfo ci) {
        StaffCinematic.begin(spell, source, slot, ticks);
    }
}
