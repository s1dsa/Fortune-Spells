package io.myf.fortune.client.movement;

import io.myf.fortune.FortuneIds;
import io.myf.fortune.expansion.movement.MovementSpells;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderBlockScreenEffectEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FortuneIds.MODID, value = Dist.CLIENT)
public final class MovementOverlay {
    private MovementOverlay() {}

    @SubscribeEvent
    public static void renderWings(RenderPlayerEvent.Post event) {
        BoneWingRenderer.draw(event);
    }

    @SubscribeEvent
    public static void allowVision(RenderBlockScreenEffectEvent event) {
        if (event.getOverlayType() == RenderBlockScreenEffectEvent.OverlayType.BLOCK
                && MovementSpells.phasing(event.getPlayer())) event.setCanceled(true);
    }

    // Vanilla already painted the containers and any regular absorption hearts.
    public static void paintBoneHearts(GuiGraphics gfx, Player player, int left, int bottom, int rowSpacing) {
        int hp = Mth.ceil(MovementSpells.boneHealth(player));
        int first = Mth.ceil((player.getMaxHealth() - 20) / 2.0f);
        for (int point = 0; point < hp; point += 2) {
            int slot = first + point / 2;
            int x = left + slot % 10 * 8;
            int y = bottom - slot / 10 * rowSpacing;
            boolean full = point + 1 < hp;
            gfx.fill(x + 1, y + 2, x + 4, y + 5, 0xffa5a8b3);
            gfx.fill(x + 2, y + 1, x + 4, y + 3, 0xffe1e3e6);
            gfx.fill(x + 2, y + 5, x + 4, y + 6, 0xffa5a8b3);
            gfx.fill(x + 3, y + 6, x + 4, y + 7, 0xff777d88);
            if (full) {
                gfx.fill(x + 4, y + 2, x + 8, y + 5, 0xffa5a8b3);
                gfx.fill(x + 5, y + 1, x + 7, y + 3, 0xffc7cbd1);
                gfx.fill(x + 4, y + 5, x + 7, y + 6, 0xff9499a3);
                gfx.fill(x + 4, y + 6, x + 6, y + 7, 0xff777d88);
                gfx.fill(x + 4, y + 7, x + 5, y + 8, 0xff777d88);
            }
        }
    }
}
