package io.myf.fortune.mixin;

import io.myf.fortune.expansion.audience.AudienceStaffItem;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.mega.revelationfix.common.apollyon.common.PlayerTickrateExecutor", remap = false)
abstract class RevelationStaffUseMixin {
    // The optional JAR is SRG-named in production, and mapped to Mojang names in development.
    @Redirect(method = "playerTick", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;m_5810_()V"))
    private static void fortune$keepUsingStaff(Player player) {
        if (!(player.getUseItem().getItem() instanceof AudienceStaffItem)) player.stopUsingItem();
    }

    @Redirect(method = "playerTick", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;stopUsingItem()V"))
    private static void fortune$keepUsingStaffDev(Player player) {
        fortune$keepUsingStaff(player);
    }
}
