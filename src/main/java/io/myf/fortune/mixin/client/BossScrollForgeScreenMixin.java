package io.myf.fortune.mixin.client;

import io.myf.fortune.expansion.crafting.ExpansionCrafting;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeScreen;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.stream.Stream;

@Mixin(value = ScrollForgeScreen.class, remap = false)
public abstract class BossScrollForgeScreenMixin {
    @Redirect(method = "generateSpellList", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;toList()Ljava/util/List;"))
    private List<AbstractSpell> fortune$filterInkTier(Stream<AbstractSpell> spells) {
        var menu = ((ScrollForgeScreen)(Object)this).getMenu();
        ItemStack ink = menu.getInkSlot().getItem();
        return spells.filter(spell -> ExpansionCrafting.matchesInkTier(spell, ink)).toList();
    }
}
