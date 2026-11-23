package io.myf.fortune.mixin;

import io.myf.fortune.expansion.rose.RoseWraith;
import lykrast.meetyourfight.entity.RosalyneEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RosalyneEntity.class, remap = false)
abstract class RosalyneSpiritCountMixin {
    @Shadow @Final private TargetingConditions spiritCountTargeting;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void fortuneSpells$excludePlayerSummons(CallbackInfo ci) {
        // Rosalyne uses this query both to claim nearby spirits and to decide her next phase.
        spiritCountTargeting.selector(spirit -> !(spirit instanceof RoseWraith));
    }
}
