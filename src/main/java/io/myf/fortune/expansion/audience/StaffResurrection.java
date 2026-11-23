package io.myf.fortune.expansion.audience;

import io.myf.fortune.FortuneIds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = FortuneIds.MODID)
public final class StaffResurrection {
    public static final int DELAY = 60;
    private static final String PENDING = "fortune_spells:resurrection";

    private StaffResurrection() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.server.isHardcore() || player.isSpectator()) return;
        if (!player.getInventory().hasAnyMatching(stack -> stack.is(AudienceContent.STAFF.get()))) return;
        CompoundTag pending = new CompoundTag();
        pending.putLong("Due", player.server.overworld().getGameTime() + DELAY);
        pending.putInt("Deaths", deaths(player) + 1);
        pending.putInt("Mode", player.gameMode.getGameModeForPlayer().getId());
        CompoundTag persistent = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persistent.put(PENDING, pending);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistent);
    }

    public static boolean awaitingRespawn(ServerPlayer player) {
        return player.server.isHardcore() && player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).contains(PENDING)
                && player.isDeadOrDying();
    }

    private static int deaths(ServerPlayer player) {
        return player.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS));
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !event.getServer().isHardcore()) return;
        long now = event.getServer().overworld().getGameTime();
        // Vanilla respawn replaces the player in the connected-player list.
        for (ServerPlayer player : List.copyOf(event.getServer().getPlayerList().getPlayers())) {
            CompoundTag persistent = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
            if (!persistent.contains(PENDING)) continue;
            CompoundTag pending = persistent.getCompound(PENDING);
            if (deaths(player) < pending.getInt("Deaths") || (!player.isDeadOrDying() && !player.isSpectator())) {
                // A later Forge listener saved the player, or an external revival completed.
                persistent.remove(PENDING);
                continue;
            }
            long due = pending.getLong("Due");
            if (now < due) {
                if (!pending.getBoolean("Drawing")) {
                    ResurrectionSeal.appear(player, false, (int)(due - now));
                    pending.putBoolean("Drawing", true);
                }
                continue;
            }
            persistent.remove(PENDING);
            ServerPlayer survivor = player;
            if (player.isDeadOrDying()) {
                survivor = player.server.getPlayerList().respawn(player, false);
                survivor.connection.player = survivor;
            }
            if (survivor.isSpectator()) survivor.setGameMode(GameType.byId(pending.getInt("Mode")));
            ResurrectionSeal.appear(survivor, true, ResurrectionSeal.REVEAL);
        }
    }
}
