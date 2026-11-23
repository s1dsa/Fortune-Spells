package io.myf.fortune.mixin.client;

import io.myf.fortune.expansion.audience.StaffTitles;
import net.minecraft.Util;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** The staff title shares the player's selected default font. Its silver ink moves across the glyphs. */
@Mixin(targets = "net.minecraft.client.gui.Font$StringRenderOutput")
abstract class CreatorTitleFontMixin {
    @ModifyVariable(method = {"accept", "m_6411_"}, at = @At("HEAD"), argsOnly = true, remap = false)
    private Style fortuneSpells$silverInk(Style style, int index, Style original, int codePoint) {
        if (!StaffTitles.CREATOR_FONT.equals(style.getFont())) return style;
        double wave = Math.cos(Util.getMillis() % 2400 / 2400.0 * 2 * Math.PI - index * .42);
        int gray = 166 + (int)(61 * wave);
        return style.withColor(gray * 0x010101);
    }
}
