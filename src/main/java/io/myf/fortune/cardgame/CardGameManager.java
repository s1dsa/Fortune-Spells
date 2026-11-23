package io.myf.fortune.cardgame;

import com.mojang.logging.LogUtils;
import io.myf.fortune.FortuneIds;
import io.myf.fortune.FortuneIds.EndReason;
import io.myf.fortune.FortuneIds.Mode;
import io.myf.fortune.FortuneIds.Phase;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = FortuneIds.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CardGameManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicInteger NEXT_ID = new AtomicInteger();
    private static final Map<UUID, CardGameSession> SESSIONS = new HashMap<>();

    private CardGameManager() {}

    public static boolean hasSession(UUID ownerId) {
        return SESSIONS.containsKey(ownerId);
    }

    public static CardGameSession get(UUID ownerId) {
        return SESSIONS.get(ownerId);
    }

    public static void start(ServerPlayer owner, byte mode, int spellLevel) {
        if ((mode != Mode.FATES_CHOICE && mode != Mode.BLIND_GAMBLE)
                || !owner.isAlive() || hasSession(owner.getUUID())) return;
        CardGameSession session = new CardGameSession(owner, NEXT_ID.incrementAndGet(), mode, spellLevel);
        SESSIONS.put(owner.getUUID(), session);
        if (!session.deal()) cancel(owner);
    }

    /** Called from the channel's server-thread work item; each accepted pick immediately leaves AWAIT_PICK. */
    public static void onPick(ServerPlayer sender, int sessionId, int cardEntityId) {
        if (sender == null) {
            LOGGER.debug("Dropped card pick for session {} without a sender", sessionId);
            return;
        }
        CardGameSession session = get(sender.getUUID());
        if (session == null || session.id() != sessionId || session.owner() != sender || !session.canPick()) {
            LOGGER.debug("Dropped stale or out-of-phase card pick from {} for session {}", sender.getUUID(), sessionId);
            return;
        }
        if (!session.isValid()) {
            LOGGER.debug("Cancelled invalid card session {} before picking", sessionId);
            cancel(sender);
            return;
        }
        if (!session.tryPick(cardEntityId)) {
            LOGGER.debug("Dropped card {} outside the valid choices or reach for session {}", cardEntityId, sessionId);
        }
    }

    public static void cancel(ServerPlayer owner) {
        CardGameSession session = get(owner.getUUID());
        if (session != null && session.owner() == owner) {
            SESSIONS.remove(owner.getUUID());
            session.finish(EndReason.CANCELLED);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || SESSIONS.isEmpty()) return;
        // Applying effects and removing entities can synchronously invoke other Forge listeners.
        for (CardGameSession session : List.copyOf(SESSIONS.values())) {
            if (session.phase() == Phase.ENDED) continue;
            if (!session.isValid()) {
                cancel(session.owner());
                continue;
            }
            session.tick();
            if (session.phase() == Phase.ENDED) SESSIONS.remove(session.owner().getUUID(), session);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) cancel(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) cancel(player);
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) cancel(player);
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent event) {
        for (CardGameSession session : List.copyOf(SESSIONS.values())) {
            cancel(session.owner());
        }
    }
}
