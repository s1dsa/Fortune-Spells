package io.myf.fortune.client.audience;

import io.myf.fortune.FortuneIds;
import io.myf.fortune.expansion.audience.AudienceStaffItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = FortuneIds.MODID, value = Dist.CLIENT)
public final class StaffNamePrompt {
    private static final Set<UUID> seen = new HashSet<>();

    private StaffNamePrompt() {}

    @SubscribeEvent
    public static void checkInventory(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || mc.player == null || !mc.player.isAlive()
                || mc.player.tickCount % 5 != 0 || mc.screen != null || mc.getOverlay() != null) return;
        var inventory = mc.player.getInventory();
        seen.removeIf(id -> AudienceStaffItem.findUninscribed(inventory, id, -1) < 0);
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            UUID id = unnamedId(inventory.getItem(slot));
            if (id != null && seen.add(id)) {
                mc.setScreen(new StaffNameScreen(id, slot));
                return;
            }
        }
    }

    @SubscribeEvent
    public static void useUnnamedStaff(PlayerInteractEvent.RightClickItem event) {
        Minecraft mc = Minecraft.getInstance();
        if (!event.getLevel().isClientSide || event.getEntity() != mc.player || mc.screen != null) return;
        UUID id = unnamedId(event.getItemStack());
        if (id != null) {
            seen.add(id);
            int slot = AudienceStaffItem.findUninscribed(mc.player.getInventory(), id, mc.player.getInventory().selected);
            if (slot >= 0) mc.setScreen(new StaffNameScreen(id, slot));
        }
    }

    private static UUID unnamedId(ItemStack stack) {
        return stack.getItem() instanceof AudienceStaffItem && !AudienceStaffItem.isInscribed(stack)
                ? AudienceStaffItem.identity(stack) : null;
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        seen.clear();
    }
}
