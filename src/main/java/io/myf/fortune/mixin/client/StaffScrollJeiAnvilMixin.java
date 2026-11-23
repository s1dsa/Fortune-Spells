package io.myf.fortune.mixin.client;

import io.myf.fortune.expansion.crafting.ExpansionCrafting;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Pseudo
@Mixin(targets = "io.redspace.ironsspellbooks.jei.ArcaneAnvilRecipeMaker", remap = false)
abstract class StaffScrollJeiAnvilMixin {
    @Redirect(method = {"getAffinityAttuneRecipes", "getScrollRecipes"},
            at = @At(value = "INVOKE", target = "Lio/redspace/ironsspellbooks/api/registry/SpellRegistry;getEnabledSpells()Ljava/util/List;"))
    private static List<AbstractSpell> fortune$listScrollRecipes() {
        return ExpansionCrafting.spellsWithScrolls();
    }
}
