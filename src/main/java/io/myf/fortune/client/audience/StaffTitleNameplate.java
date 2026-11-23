package io.myf.fortune.client.audience;

import io.myf.fortune.FortuneIds;
import io.myf.fortune.expansion.audience.StaffTitles;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FortuneIds.MODID, value = Dist.CLIENT)
public final class StaffTitleNameplate {
    private StaffTitleNameplate() {}

    @SubscribeEvent
    public static void nameplate(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        var connection = Minecraft.getInstance().getConnection();
        var info = connection == null ? null : connection.getPlayerInfo(player.getUUID());
        Component roster = info == null ? null : info.getTabListDisplayName();
        if (roster != null && roster.getContents() instanceof TranslatableContents named
                && named.getKey().equals(StaffTitles.DISPLAY) && named.getArgs().length == 2) {
            event.setContent(Component.translatable(StaffTitles.DISPLAY, named.getArgs()[0], event.getContent()));
        }
    }
}
