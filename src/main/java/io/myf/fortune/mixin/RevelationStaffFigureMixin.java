package io.myf.fortune.mixin;

import io.myf.fortune.expansion.audience.CataclysmField;
import io.myf.fortune.expansion.offense.SpellFigure;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.mega.revelationfix.common.config.CommonConfig", remap = false)
abstract class RevelationStaffFigureMixin {
    @Inject(method = "inWhitelist(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private static void fortune$protectStaffSpellEntities(Entity entity, CallbackInfoReturnable<Boolean> result) {
        if (entity instanceof CataclysmField || entity instanceof SpellFigure figure && figure.empowered())
            result.setReturnValue(true);
    }
}
