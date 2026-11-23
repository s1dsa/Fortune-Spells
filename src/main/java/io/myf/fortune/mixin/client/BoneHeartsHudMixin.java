package io.myf.fortune.mixin.client;

import io.myf.fortune.client.movement.MovementOverlay;
import io.myf.fortune.expansion.movement.MovementSpells;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
abstract class BoneHeartsHudMixin {
    @Unique
    private static int fortuneSpells$redHealth(int total) {
        var player = Minecraft.getInstance().player;
        return player == null ? total : Math.max(0, total - Mth.ceil(MovementSpells.boneHealth(player)));
    }

    @ModifyVariable(method = {"renderHearts", "m_168688_"}, at = @At("HEAD"), argsOnly = true, index = 8, remap = false)
    private int fortuneSpells$currentRed(int total) { return fortuneSpells$redHealth(total); }

    @ModifyVariable(method = {"renderHearts", "m_168688_"}, at = @At("HEAD"), argsOnly = true, index = 9, remap = false)
    private int fortuneSpells$blinkingRed(int total) { return fortuneSpells$redHealth(total); }

    @Inject(method = {"renderHearts", "m_168688_"}, at = @At("RETURN"), remap = false)
    private void fortuneSpells$greyHearts(GuiGraphics gfx, Player player, int left, int bottom, int rowSpacing,
                                          int regenerationIndex, float max, int hp, int shownHp, int absorption,
                                          boolean blink, CallbackInfo info) {
        MovementOverlay.paintBoneHearts(gfx, player, left, bottom, rowSpacing);
    }
}
