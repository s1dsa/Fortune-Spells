package io.myf.fortune.client.cardgame;

import io.myf.fortune.FortuneIds.Anim;
import io.myf.fortune.FortuneIds.EndReason;
import io.myf.fortune.FortuneIds.Face;
import io.myf.fortune.FortuneIds.Phase;
import io.myf.fortune.client.fx.FortuneFx;
import io.myf.fortune.entity.FateCardEntity;
import io.myf.fortune.network.C2SCardPick;
import io.myf.fortune.network.FSNetwork;
import io.myf.fortune.network.S2CCardGamePhase;
import io.myf.fortune.network.S2CCardGameResult;
import io.myf.fortune.network.S2CCardGameStart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.Optional;

/**
 * 本地玩家的猜牌对局（DESIGN 8.5 / 9.5 / 9.6）。只保存服务端发来的信息，
 * 答案永远不在客户端：盲眼赌局里客户端不知道哪张是对的，直到 S2CCardGameResult 到达。
 */
public final class ClientCardGame {
    private ClientCardGame() {}

    /** 准星射线长度（格）。服务端允许 24 格内点选，这里取 16 格，和设计一致。 */
    private static final double PICK_RANGE = 16.0;
    private static final double PICK_INFLATE = 0.1;
    /** 对局结束后结果文字再留多久（tick）。 */
    private static final int RESULT_HOLD_TICKS = 30;
    private static final int CANCEL_SHOW_TICKS = 30;
    private static final int[] NO_CARDS = {};

    private static boolean active;
    private static int sessionId = -1;
    private static byte mode;
    private static int[] cardIds = NO_CARDS;
    private static byte targetFace = -1;
    private static int pickWindowTicks;

    private static byte phase = Phase.ENDED;
    private static int phaseStartTick;
    private static int phaseDuration;

    private static int hoveredId = -1;
    private static int pickedId = -1;

    private static boolean hasResult;
    private static boolean won;
    private static boolean timedOut;

    /** 结果 / 作废提示在对局清掉以后还显示到这个 tick。 */
    private static int messageUntil;
    public static final byte LINGER_NONE = 0, LINGER_RESULT = 1, LINGER_CANCELLED = 2;
    private static byte lingerMessage = LINGER_NONE;

    /** 本地 tick 计数，客户端 tick 结束时 +1；不受世界时间、暂停影响以外的东西干扰。 */
    private static int clientTicks;
    private static final byte[] lastAnim = new byte[Face.COUNT];

    // ---------------------------------------------------------------- 网络入口

    public static void onStart(S2CCardGameStart msg) {
        active = true;
        sessionId = msg.sessionId();
        mode = msg.mode();
        cardIds = msg.cardEntityIds();
        targetFace = msg.targetFace();
        pickWindowTicks = msg.pickWindowTicks();
        phase = Phase.DEALING;
        phaseStartTick = clientTicks;
        phaseDuration = 0;
        hoveredId = -1;
        pickedId = -1;
        hasResult = false;
        won = false;
        timedOut = false;
        lingerMessage = LINGER_NONE;
        Arrays.fill(lastAnim, Anim.NOT_HERE);
    }

    public static void onPhase(S2CCardGamePhase msg) {
        if (!active || msg.sessionId() != sessionId) {
            return;
        }
        phase = msg.phase();
        phaseStartTick = clientTicks;
        phaseDuration = msg.durationTicks();
        if (phase != Phase.AWAIT_PICK) {
            hoveredId = -1;
        }
        if (phase == Phase.ENDED) {
            if (msg.reason() == EndReason.CANCELLED) {
                lingerMessage = LINGER_CANCELLED;
                messageUntil = clientTicks + CANCEL_SHOW_TICKS;
            } else if (hasResult) {
                lingerMessage = LINGER_RESULT;
                messageUntil = clientTicks + RESULT_HOLD_TICKS;
            }
            active = false;
            cardIds = NO_CARDS;
            pickedId = -1;
        }
    }

    public static void onResult(S2CCardGameResult msg) {
        if (!active || msg.sessionId() != sessionId) {
            return;
        }
        hasResult = true;
        won = msg.won();
        timedOut = msg.timedOut();
        pickedId = msg.pickedCardId();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Entity picked = mc.level.getEntity(msg.pickedCardId());
            if (picked != null) {
                FortuneFx.cardResult(picked, won);
            }
        }
    }

    public static void reset() {
        active = false;
        sessionId = -1;
        cardIds = NO_CARDS;
        phase = Phase.ENDED;
        hoveredId = -1;
        pickedId = -1;
        hasResult = false;
        lingerMessage = LINGER_NONE;
        targetFace = Face.HIDDEN;
        timedOut = false;
        won = false;
    }

    // ---------------------------------------------------------------- 每 tick

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused()) {
            return;
        }
        clientTicks++;
        if (lingerMessage != LINGER_NONE && clientTicks - messageUntil >= 0) {
            lingerMessage = LINGER_NONE;
        }
        if (!active) {
            return;
        }
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            reset();
            return;
        }
        hoveredId = canPick() ? raycastCard(mc, player) : -1;

        for (int i = 0; i < cardIds.length && i < lastAnim.length; i++) {
            if (!(mc.level.getEntity(cardIds[i]) instanceof FateCardEntity card) || !belongsToGame(card, player)) {
                continue;
            }
            byte anim = card.getAnim();
            if (anim == Anim.APPEAR && lastAnim[i] != Anim.APPEAR) {
                FortuneFx.cardAppear(card);
            }
            lastAnim[i] = anim;
            if (phase == Phase.SHUFFLING && (clientTicks & 1) == 0) {
                FortuneFx.shuffleTrail(card);
            }
        }
    }

    private static boolean belongsToGame(FateCardEntity card, LocalPlayer player) {
        return card.isAlive() && card.getSessionId() == sessionId && card.getOwnerId() == player.getId();
    }

    private static int raycastCard(Minecraft mc, LocalPlayer player) {
        Entity camera = mc.getCameraEntity();
        if (camera == null) camera = player;
        float partialTick = mc.getFrameTime();
        Vec3 from = camera.getEyePosition(partialTick);
        Vec3 to = from.add(camera.getViewVector(partialTick).scale(PICK_RANGE));
        int best = -1;
        double bestDist = Double.MAX_VALUE;
        for (int id : cardIds) {
            if (!(player.level().getEntity(id) instanceof FateCardEntity card) || !belongsToGame(card, player)) {
                continue;
            }
            AABB box = card.getBoundingBox().inflate(PICK_INFLATE);
            Optional<Vec3> hit = box.contains(from) ? Optional.of(from) : box.clip(from, to);
            if (hit.isPresent()) {
                double dist = hit.get().distanceToSqr(from);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = id;
                }
            }
        }
        return best;
    }

    // ---------------------------------------------------------------- 点选

    private static boolean canPick() {
        return active && phase == Phase.AWAIT_PICK && pickedId == -1;
    }

    /**
     * 攻击键 / 使用键按下时调用。返回 true 表示这次按键被牌局吃掉了（调用方要取消原版行为）。
     * 准星没对着牌时返回 false，不影响正常操作。
     */
    public static boolean tryPick() {
        if (!canPick() || hoveredId == -1) {
            return false;
        }
        pickedId = hoveredId;
        hoveredId = -1;
        FSNetwork.sendToServer(new C2SCardPick(sessionId, pickedId));
        return true;
    }

    // ---------------------------------------------------------------- 给渲染 / HUD 读

    /** 本地需要高亮（白描边、上浮）的牌：悬停中，或已经选了还没揭晓。 */
    public static boolean isHighlighted(int entityId) {
        if (!active || entityId < 0) {
            return false;
        }
        if (entityId == hoveredId) {
            return true;
        }
        return entityId == pickedId && !hasResult && phase == Phase.AWAIT_PICK;
    }

    public static boolean isActive() { return active; }
    public static byte phase() { return phase; }
    public static byte mode() { return mode; }
    public static byte targetFace() { return targetFace; }
    public static boolean hasPicked() { return pickedId != -1; }
    public static boolean isHovering() { return hoveredId != -1; }
    public static boolean hasResult() { return hasResult; }
    public static boolean won() { return won; }
    public static boolean timedOut() { return timedOut; }

    /** 当前阶段剩余比例 0..1；阶段没有时长时返回 1。 */
    public static float phaseRemaining(float partialTick) {
        int duration = phase == Phase.AWAIT_PICK && phaseDuration <= 0 ? pickWindowTicks : phaseDuration;
        if (duration <= 0) {
            return 1f;
        }
        float elapsed = clientTicks - phaseStartTick + partialTick;
        return Math.max(0f, Math.min(1f, 1f - elapsed / duration));
    }

    /** 当前阶段剩余 tick。 */
    public static int phaseTicksLeft() {
        int duration = phase == Phase.AWAIT_PICK && phaseDuration <= 0 ? pickWindowTicks : phaseDuration;
        return Math.max(0, duration - (clientTicks - phaseStartTick));
    }

    /** 对局结束后还要显示的提示类型（LINGER_*）。 */
    public static byte lingerMessage() {
        return lingerMessage;
    }

    public static int clientTicks() { return clientTicks; }
}
