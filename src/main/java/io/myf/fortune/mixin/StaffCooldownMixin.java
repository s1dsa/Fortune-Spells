package io.myf.fortune.mixin;

import io.myf.fortune.expansion.audience.StaffAttributes;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MagicManager.class, remap = false)
abstract class StaffCooldownMixin {
    @Inject(method = "getEffectiveSpellCooldown", at = @At("HEAD"), cancellable = true)
    private static void fortune$zeroCooldown(AbstractSpell spell, Player player, CastSource source, CallbackInfoReturnable<Integer> cir) {
        if (player != null && StaffAttributes.heldBy(player)) cir.setReturnValue(0);
    }
}
