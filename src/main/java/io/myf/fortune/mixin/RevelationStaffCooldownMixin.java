package io.myf.fortune.mixin;

import io.myf.fortune.expansion.audience.AudienceStaffItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.mega.revelationfix.common.apollyon.common.CooldownsManager", remap = false)
abstract class RevelationStaffCooldownMixin {
    @Inject(method = "setItemCooldowns", at = @At("HEAD"), cancellable = true)
    private static void fortune$skipStaff(Player player, Item item, int ticks, CallbackInfo ci) {
        if (item instanceof AudienceStaffItem) ci.cancel();
    }
}
