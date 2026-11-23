package io.myf.fortune.expansion.audience;

import io.myf.fortune.FortuneIds;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FortuneIds.MODID)
public final class StaffTitles {
    private static final String STAFF_NAME = "fortune_spells_first_named_staff";
    public static final String DISPLAY = "title.fortune_spells.display";
    public static final ResourceLocation CREATOR_FONT = FortuneIds.id("creator");

    private StaffTitles() {}

    static void recognize(ServerPlayer owner, ItemStack stack) {
        if (!AudienceStaffItem.isInscribed(stack) || !stack.hasCustomHoverName()) return;
        CompoundTag data = owner.getPersistentData();
        CompoundTag persistent = data.getCompound(Player.PERSISTED_NBT_TAG);
        if (persistent.contains(STAFF_NAME)) return;
        persistent.putString(STAFF_NAME, stack.getHoverName().getString());
        data.put(Player.PERSISTED_NBT_TAG, persistent);
        refresh(owner);
    }

    private static void refresh(ServerPlayer owner) {
        owner.refreshDisplayName();
        owner.refreshTabListName();
    }

    private static MutableComponent title(ServerPlayer owner) {
        String name = owner.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getString(STAFF_NAME);
        if (name.isEmpty()) return null;
        if (owner.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS)) > 0)
            return Component.translatable("title.fortune_spells.transcender", name).withStyle(ChatFormatting.GOLD);
        return Component.translatable("title.fortune_spells.creator", name)
                .withStyle(style -> style.withFont(CREATOR_FONT).withColor(0xBDBDBD).withItalic(true));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void formatName(PlayerEvent.NameFormat event) {
        if (!(event.getEntity() instanceof ServerPlayer owner)) return;
        Component prefix = title(owner);
        if (prefix != null) event.setDisplayname(Component.translatable(DISPLAY, prefix, event.getDisplayname()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void formatRoster(PlayerEvent.TabListNameFormat event) {
        if (!(event.getEntity() instanceof ServerPlayer owner)) return;
        Component prefix = title(owner);
        if (prefix == null) return;
        Component base = event.getDisplayName();
        if (base == null) base = PlayerTeam.formatNameForTeam(owner.getTeam(), owner.getName());
        event.setDisplayName(Component.translatable(DISPLAY, prefix, base));
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer owner) refresh(owner);
    }
}
