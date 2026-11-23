package io.myf.fortune.mixin.client;

import io.myf.fortune.expansion.crafting.ExpansionCrafting;
import io.myf.fortune.expansion.audience.AudienceStaffItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.util.TooltipsUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TooltipsUtils.class, remap = false)
public abstract class TranscendentTooltipMixin {
    @Inject(method = "getLevelComponenet", at = @At("HEAD"), cancellable = true)
    private static void fortune$displayStaffLevels(SpellData data, LivingEntity caster, CallbackInfoReturnable<MutableComponent> cir) {
        if (AudienceStaffItem.exclusive(data.getSpell())) {
            cir.setReturnValue(Component.translatable("ui.fortune_spells.world_limit"));
        } else if (ExpansionCrafting.transcendent(data.getSpell()) && data.getLevel() == AudienceStaffItem.TRANSCENDENT_LEVEL) {
            cir.setReturnValue(Component.literal(Integer.toString(AudienceStaffItem.TRANSCENDENT_LEVEL)));
        }
    }

    @Inject(method = "getTitleComponent", at = @At("RETURN"), cancellable = true)
    private static void fortune$bookTier(SpellData data, LocalPlayer reader, CallbackInfoReturnable<MutableComponent> cir) {
        if (ExpansionCrafting.transcendent(data.getSpell())) {
            cir.setReturnValue(cir.getReturnValue().append(" · ").append(ExpansionCrafting.tierName()));
        }
    }

    @Redirect(method = "formatScrollTooltip", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/api/spells/SpellRarity;getDisplayName()Lnet/minecraft/network/chat/MutableComponent;"))
    private static MutableComponent fortune$displayTier(SpellRarity rarity, ItemStack scroll, Player player) {
        var spell = ISpellContainer.get(scroll).getSpellAtIndex(0).getSpell();
        return ExpansionCrafting.transcendent(spell) ? ExpansionCrafting.tierName() : rarity.getDisplayName();
    }
}
