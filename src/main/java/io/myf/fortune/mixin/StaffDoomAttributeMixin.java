package io.myf.fortune.mixin;

import io.myf.fortune.expansion.audience.DoomAttributeState;
import io.myf.fortune.expansion.audience.StaffAttributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AttributeInstance.class)
abstract class StaffDoomAttributeMixin implements DoomAttributeState {
    @Inject(method = {"getValue", "m_22135_"}, at = @At("RETURN"), cancellable = true, remap = false)
    private void fortune$staffInDoom(CallbackInfoReturnable<Double> result) {
        if (!revelationfix$alwaysBase()) return;
        double value = result.getReturnValueD();
        double restored = StaffAttributes.getDoomValue((AttributeInstance)(Object)this, value);
        if (restored != value) result.setReturnValue(restored);
    }
}
