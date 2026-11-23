package io.myf.fortune.mixin;

import io.myf.fortune.expansion.crafting.ExpansionCrafting;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeMenu;
import io.redspace.ironsspellbooks.item.InkItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = ScrollForgeMenu.class, remap = false)
public abstract class BossScrollForgeMixin {
    @ModifyVariable(method = "setupResultSlot", at = @At("HEAD"), argsOnly = true)
    private AbstractSpell fortune$validateRecipe(AbstractSpell spell) {
        ScrollForgeMenu menu = (ScrollForgeMenu)(Object)this;
        ItemStack ink = menu.getInkSlot().getItem();
        if (!ExpansionCrafting.matchesInkTier(spell, ink)
                || (spell.getSpellResource().getNamespace().equals("fortune_spells")
                    && ink.getItem() instanceof InkItem bottle && spell.getMinLevelForRarity(bottle.getRarity()) < 1)) {
            return SpellRegistry.none();
        }
        return spell;
    }
}
