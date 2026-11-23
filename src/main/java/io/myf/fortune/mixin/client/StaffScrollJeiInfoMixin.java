package io.myf.fortune.mixin.client;

import io.myf.fortune.registry.FSSpells;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "io.redspace.ironsspellbooks.jei.JeiPlugin", remap = false)
abstract class StaffScrollJeiInfoMixin {
    @Redirect(method = "lambda$registerRecipes$1", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;isEnabled()Z"))
    private static boolean fortune$includeScrollInformation(AbstractSpell spell) {
        return spell != FSSpells.CATACLYSM.get() && spell.isEnabled();
    }
}
