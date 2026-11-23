package io.myf.fortune.client.audience;

import com.mojang.datafixers.util.Either;
import io.myf.fortune.FortuneIds;
import io.myf.fortune.expansion.audience.AudienceStaffItem;
import io.myf.fortune.expansion.audience.StaffTitles;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = FortuneIds.MODID, value = Dist.CLIENT)
public record StaffLore(List<FormattedCharSequence> lines, int width) implements TooltipComponent, ClientTooltipComponent {
    @Mod.EventBusSubscriber(modid = FortuneIds.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Factory {
        @SubscribeEvent public static void register(RegisterClientTooltipComponentFactoriesEvent event) {
            event.register(StaffLore.class, component -> component);
        }
    }

    @SubscribeEvent public static void gather(RenderTooltipEvent.GatherComponents event) {
        var stack = event.getItemStack();
        if (!(stack.getItem() instanceof AudienceStaffItem)) return;
        String base = "lore.fortune_spells.staff.";
        List<Component> text = new ArrayList<>();
        if (Screen.hasShiftDown()) {
            text.add(Component.translatable(base + "master", stack.getHoverName().getString()));
            for (String key : List.of("cooldown", "mana", "destiny", "unseen", "malice", "doom"))
                text.add(Component.translatable(base + key));
        }
        if (Screen.hasAltDown()) text.add(Component.translatable(base + "cosmos"));
        if (text.isEmpty()) {
            event.getTooltipElements().add(Math.min(1, event.getTooltipElements().size()),
                    Either.left(Component.translatable(base + "keys").withStyle(style -> style.withColor(0xA7A7AF))));
            return;
        }
        Font font = Minecraft.getInstance().font;
        int wrap = Math.max(30, Math.min(280, event.getScreenWidth() - 40));
        List<FormattedCharSequence> lines = new ArrayList<>();
        int width = 0;
        for (Component line : text) {
            var styled = line.copy().withStyle(style -> style.withFont(StaffTitles.CREATOR_FONT).withColor(0xC4C4C4));
            for (var row : font.split(styled, wrap)) {
                lines.add(row);
                width = Math.max(width, font.width(row));
            }
        }
        event.getTooltipElements().add(Math.min(1, event.getTooltipElements().size()), Either.right(new StaffLore(lines, width + 12)));
    }

    @Override public int getWidth(Font font) { return width; }
    @Override public int getHeight() { return lines.size() * 11 + 5; }

    @Override public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        int row = y + 1;
        for (var line : lines) {
            graphics.drawString(font, line, x + 6, row, 0xFFFFFFFF, true);
            row += 11;
        }
        // Drift motes along the margins so even the smallest glyphs stay legible.
        double t = (Util.getMillis() % 12000L) / 12000.0;
        for (int i = 0; i < 10; i++) {
            double phase = (t * (1 + i % 3) + i * .618034) % 1;
            int dx = (i % 2 == 0 ? x : x + width - 5) + 2 + (int)(2 * Math.sin(phase * Math.PI * 2 + i));
            int dy = y + (int)((1 - phase) * (getHeight() - 3));
            int alpha = 40 + (int)(160 * Math.sin(Math.PI * phase));
            graphics.fill(dx, dy, dx + 1, dy + 2, (alpha << 24) | 0xDDDDE2);
        }
    }
}
