package io.myf.fortune.mixin;

import io.myf.fortune.expansion.audience.AudienceSpell;
import io.myf.fortune.expansion.audience.AudienceStaffItem;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.gui.inscription_table.InscriptionTableMenu;
import io.redspace.ironsspellbooks.item.Scroll;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InscriptionTableMenu.class, remap = false)
public abstract class AudienceInscriptionMixin {
    @Shadow @Final private Player player;
    @Shadow public abstract Slot getScrollSlot();

    @Inject(method = "doInscription", at = @At("HEAD"), cancellable = true)
    private void fortune$keepAudienceOnScroll(int selectedIndex, CallbackInfo callback) {
        ItemStack scroll = getScrollSlot().getItem();
        if (!(scroll.getItem() instanceof Scroll) || !ISpellContainer.isSpellContainer(scroll)) return;
        var spell = ISpellContainer.get(scroll).getSpellAtIndex(0).getSpell();
        if (spell instanceof AudienceSpell || AudienceStaffItem.exclusive(spell)) {
            if (spell instanceof AudienceSpell && player instanceof ServerPlayer owner) {
                owner.displayClientMessage(Component.translatable("message.fortune_spells.audience.scroll_only"), true);
            }
            callback.cancel();
        }
    }
}
