package io.myf.fortune.mixin;

import io.myf.fortune.expansion.crafting.ExpansionCrafting;
import io.myf.fortune.expansion.audience.AudienceSpell;
import io.myf.fortune.expansion.audience.AudienceStaffItem;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.item.Scroll;
import net.minecraft.world.item.ItemStack;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.gui.arcane_anvil.ArcaneAnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ArcaneAnvilMenu.class, remap = false)
public abstract class TranscendentAnvilMixin {
    @Redirect(method = {"createResult", "m_6640_"}, at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/api/util/Utils;canImbue(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean fortune$allowImbuement(ItemStack equipment) {
        ItemStack scroll = ((ArcaneAnvilMenu)(Object)this).getSlot(1).getItem();
        if (!Utils.canImbue(equipment)) return false;
        if (!(scroll.getItem() instanceof Scroll) || !ISpellContainer.isSpellContainer(scroll)) return true;
        var spell = ISpellContainer.get(scroll).getSpellAtIndex(0).getSpell();
        return !(spell instanceof AudienceSpell) && !AudienceStaffItem.exclusive(spell);
    }

    @Redirect(method = {"createResult", "m_6640_"}, at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/api/spells/SpellRarity;equals(Ljava/lang/Object;)Z"))
    private boolean fortune$matchUpgradeInk(SpellRarity next, Object bottleTier) {
        var menu = (ArcaneAnvilMenu)(Object)this;
        return !menu.getSlot(1).getItem().is(ExpansionCrafting.INK.get()) && next == bottleTier;
    }
}
