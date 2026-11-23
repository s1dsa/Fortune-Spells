package io.myf.fortune.mixin.client;

import io.myf.fortune.expansion.crafting.ExpansionCrafting;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.item.InkItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;

@Pseudo
@Mixin(targets = "io.redspace.ironsspellbooks.jei.ScrollForgeRecipeMaker", remap = false)
public abstract class BossJeiRecipesMixin {
    @Redirect(method = "lambda$getRecipes$0", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;getMinLevelForRarity(Lio/redspace/ironsspellbooks/api/spells/SpellRarity;)I"))
    private static int fortune$recipeLevel(AbstractSpell spell, SpellRarity nativeRarity, List<AbstractSpell> spells,
                                           ArrayList<ItemStack> inks, ArrayList<ItemStack> scrolls, InkItem ink) {
        // The recipe maker omits pairs with level zero, keeping its input and output lists aligned.
        return ExpansionCrafting.matchesInkTier(spell, ink) ? spell.getMinLevelForRarity(nativeRarity) : 0;
    }
}
