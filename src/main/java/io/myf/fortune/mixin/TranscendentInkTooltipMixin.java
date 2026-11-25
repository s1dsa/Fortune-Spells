package io.myf.fortune.mixin;

import io.myf.fortune.expansion.crafting.ExpansionCrafting;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.item.InkItem;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = InkItem.class, remap = false)
public abstract class TranscendentInkTooltipMixin {
    @Redirect(method = {"appendHoverText", "m_7373_"}, at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/api/spells/SpellRarity;getDisplayName()Lnet/minecraft/network/chat/MutableComponent;"))
    private MutableComponent fortune$inkTierName(SpellRarity rarity) {
        if (ExpansionCrafting.INK.isPresent() && (Object) this == ExpansionCrafting.INK.get()) {
            return ExpansionCrafting.tierName();
        }
        return rarity.getDisplayName();
    }
}
