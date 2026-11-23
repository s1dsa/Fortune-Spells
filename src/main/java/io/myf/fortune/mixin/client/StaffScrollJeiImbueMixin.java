package io.myf.fortune.mixin.client;

import io.myf.fortune.expansion.crafting.ExpansionCrafting;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.myf.fortune.expansion.audience.AudienceSpell;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Pseudo
@Mixin(targets = "io.redspace.ironsspellbooks.jei.ArcaneAnvilJeiRecipe", remap = false)
abstract class StaffScrollJeiImbueMixin {
    @Redirect(method = "getRecipeItems", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lio/redspace/ironsspellbooks/api/registry/SpellRegistry;getEnabledSpells()Ljava/util/List;"))
    private List<AbstractSpell> fortune$listImbueableSpells() {
        return ExpansionCrafting.spellsWithScrolls().stream().filter(spell -> !(spell instanceof AudienceSpell)).toList();
    }
}
