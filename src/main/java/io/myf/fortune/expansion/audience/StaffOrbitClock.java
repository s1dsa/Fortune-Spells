package io.myf.fortune.expansion.audience;

import com.google.common.collect.MapMaker;
import io.myf.fortune.FortuneIds;
import io.myf.fortune.network.FSNetwork;
import io.myf.fortune.network.S2CFortuneFx;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

@Mod.EventBusSubscriber(modid = FortuneIds.MODID)
public final class StaffOrbitClock {
    // Both the orbit (320 ticks) and the rotating dial (1600 ticks) repeat in this interval.
    public static final int CYCLE = 1600;
    private static final Map<ServerPlayer, Long> STARTS = new MapMaker().weakKeys().makeMap();

    private StaffOrbitClock() {}

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || player.connection == null) return;
        if (player.isAlive() && StaffAttributes.heldBy(player)) {
            if (!STARTS.containsKey(player)) {
                STARTS.put(player, player.level().getGameTime());
                FSNetwork.sendTracking(player, packet(player, 0));
            }
        } else if (STARTS.remove(player) != null) {
            FSNetwork.sendTracking(player, packet(player, -1));
        }
    }

    @SubscribeEvent
    public static void track(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer wearer && event.getEntity() instanceof ServerPlayer viewer)
            syncTo(wearer, viewer);
    }

    @SubscribeEvent
    public static void changeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) syncTo(player, player);
    }

    private static void syncTo(ServerPlayer wearer, ServerPlayer viewer) {
        Long start = STARTS.get(wearer);
        if (start == null || !StaffAttributes.heldBy(wearer)) return;
        long elapsed = Math.max(0, wearer.level().getGameTime() - start);
        // Keep one full revolution before wrapping, beyond every engraving and acceleration stage.
        int age = elapsed < CYCLE ? (int) elapsed : CYCLE + (int) (elapsed % CYCLE);
        FSNetwork.sendTo(viewer, packet(wearer, age));
    }

    private static S2CFortuneFx packet(ServerPlayer player, int age) {
        return new S2CFortuneFx(FortuneIds.FxKind.STAFF_ORBIT, player.getId(), age);
    }
}
