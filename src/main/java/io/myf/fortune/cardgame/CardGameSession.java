package io.myf.fortune.cardgame;

import io.myf.fortune.FortuneIds.Anim;
import io.myf.fortune.FortuneIds.Ask;
import io.myf.fortune.FortuneIds.EndReason;
import io.myf.fortune.FortuneIds.Face;
import io.myf.fortune.FortuneIds.FxKind;
import io.myf.fortune.FortuneIds.Mode;
import io.myf.fortune.FortuneIds.Phase;
import io.myf.fortune.FortuneIds.Result;
import io.myf.fortune.FortuneIds.ResultEffect;
import io.myf.fortune.FortuneTuning;
import io.myf.fortune.entity.FateCardEntity;
import io.myf.fortune.network.FSNetwork;
import io.myf.fortune.network.S2CCardGamePhase;
import io.myf.fortune.network.S2CCardGameResult;
import io.myf.fortune.network.S2CCardGameStart;
import io.myf.fortune.network.S2CFortuneFx;
import io.myf.fortune.registry.FSEffects;
import io.myf.fortune.registry.FSEntities;
import io.myf.fortune.registry.FSSounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec3;

public final class CardGameSession {
    private static final int HIDE_AT = FortuneTuning.DEAL_TICKS + FortuneTuning.HINT_PHASE_TICKS;
    private static final int SHUFFLE_AT = HIDE_AT + Anim.DURATION[Anim.HIDE];
    private static final int SHUFFLE_TICKS = FortuneTuning.SHUFFLE_APPROACH_TICKS
            + 2 * FortuneTuning.SHUFFLE_ACCEL_TICKS + FortuneTuning.SHUFFLE_LAND_TICKS;

    private final int id, spellLevel, startedAt, correct;
    private final ServerPlayer owner;
    private final ServerLevel level;
    private final byte mode;
    private final Direction direction;
    private final Vec3 center;
    private final byte[] faces;
    private final List<FateCardEntity> cards;
    private byte phase = Phase.DEALING;
    private int phaseStartedAt;
    private int lastTick;
    private int picked = -1;
    private byte pendingEffect = ResultEffect.NONE;
    private int effectTicks;
    private boolean timeout, settled;

    CardGameSession(ServerPlayer owner, int id, byte mode, int spellLevel) {
        this.owner = owner;
        this.id = id;
        this.mode = mode;
        this.spellLevel = spellLevel;
        level = owner.serverLevel();
        startedAt = level.getServer().getTickCount();
        lastTick = startedAt;
        direction = Direction.fromYRot(owner.getYRot());
        center = owner.position().add(direction.getStepX() * FortuneTuning.TABLE_FORWARD,
                FortuneTuning.TABLE_HEIGHT, direction.getStepZ() * FortuneTuning.TABLE_FORWARD);
        int count = FortuneTuning.cardCount(mode, spellLevel);
        correct = level.random.nextInt(count);
        cards = new ArrayList<>(count);
        faces = new byte[count];
        if (isBlind()) {
            int[] suits = shuffledIndices(Face.COUNT - 1, level.random);
            int nextSuit = 0;
            for (int i = 0; i < count; i++) {
                faces[i] = i == correct ? Face.JOKER : (byte) suits[nextSuit++];
            }
        } else {
            int[] order = shuffledIndices(count, level.random);
            for (int i = 0; i < count; i++) faces[i] = (byte) order[i];
        }
    }

    public int id() { return id; }
    public byte phase() { return phase; }
    ServerPlayer owner() { return owner; }
    private boolean isBlind() { return mode == Mode.BLIND_GAMBLE; }

    private static int[] shuffledIndices(int size, RandomSource random) {
        int[] result = new int[size];
        for (int i = 0; i < size; i++) result[i] = i;
        for (int i = size - 1; i > 0; --i) {
            int j = random.nextInt(i + 1);
            int previous = result[i];
            result[i] = result[j];
            result[j] = previous;
        }
        return result;
    }

    private Vec3 slot(int index) {
        Direction side = direction.getClockWise();
        double offset = (index - (faces.length - 1) / 2.0) * FortuneTuning.SLOT_SPACING;
        return center.add(side.getStepX() * offset, 0, side.getStepZ() * offset);
    }

    boolean deal() {
        int[] ids = new int[faces.length];
        for (int i = 0; i < faces.length; i++) {
            FateCardEntity card = new FateCardEntity(FSEntities.FATE_CARD.get(), level);
            Vec3 slot = slot(i);
            card.moveTo(slot.x, slot.y, slot.z, direction.getOpposite().toYRot(), 0);
            card.bindTo(owner.getId(), id);
            card.setFace(isBlind() ? Face.HIDDEN : faces[i]);
            if (!level.addFreshEntity(card)) return false;
            ids[i] = card.getId();
            cards.add(card);
        }
        FSNetwork.sendTo(owner, new S2CCardGameStart(id, mode, (byte) spellLevel, (byte) cards.size(),
                ids, isBlind() ? Face.HIDDEN : faces[correct], FortuneTuning.PICK_WINDOW_TICKS));
        beginPhase(Phase.DEALING, FortuneTuning.DEAL_TICKS);
        animateDeal(0);
        return true;
    }

    boolean isValid() {
        if (!owner.isAlive() || owner.hasDisconnected() || owner.level() != level
                || owner.position().distanceToSqr(center) > FortuneTuning.MAX_TABLE_DISTANCE * FortuneTuning.MAX_TABLE_DISTANCE) {
            return false;
        }
        return cards.stream().allMatch(card -> card.isAlive() && card.level() == level
                && level.getEntity(card.getId()) == card);
    }

    void tick() {
        int now = level.getServer().getTickCount();
        if (now == lastTick) return;
        lastTick = now;
        int elapsed = now - startedAt;
        int phaseAge = now - phaseStartedAt;
        switch (phase) {
            case Phase.DEALING -> {
                animateDeal(elapsed);
                if (elapsed >= FortuneTuning.DEAL_TICKS) {
                    if (isBlind()) awaitPick();
                    else showTarget();
                }
            }
            case Phase.SHOW_TARGET -> tickHint(elapsed);
            case Phase.SHUFFLING -> {
                if (phaseAge >= SHUFFLE_TICKS) awaitPick();
            }
            case Phase.AWAIT_PICK -> {
                if (phaseAge >= FortuneTuning.PICK_WINDOW_TICKS) {
                    choose(level.random.nextInt(cards.size()), true);
                }
            }
            case Phase.REVEAL -> tickReveal(phaseAge);
            default -> { }
        }
    }

    private void animateDeal(int elapsed) {
        for (int i = 0; i < cards.size(); i++) {
            FateCardEntity card = cards.get(i);
            int age = elapsed - i * FortuneTuning.DEAL_INTERVAL;
            if (age == 0) {
                card.setAnim(Anim.APPEAR);
                play(card, FSSounds.CARD_APPEAR.get());
            } else if (age == Anim.DURATION[Anim.APPEAR]) {
                card.setAnim(isBlind() ? Anim.IDLE_HIDDEN : Anim.IDLE_SHOW);
            }
        }
    }

    private void showTarget() {
        FateCardEntity target = cards.get(correct);
        target.setAnim(Anim.HINT);
        target.setAsk(faces[correct]);
        beginPhase(Phase.SHOW_TARGET, FortuneTuning.HINT_PHASE_TICKS);
    }

    private void tickHint(int elapsed) {
        if (elapsed == FortuneTuning.DEAL_TICKS + Anim.DURATION[Anim.HINT]) {
            cards.get(correct).setAnim(Anim.IDLE_SHOW);
        } else if (elapsed == HIDE_AT) {
            cards.forEach(card -> card.setAnim(Anim.HIDE));
            play(center, FSSounds.CARD_FLIP.get());
        } else if (elapsed == HIDE_AT + Anim.DURATION[Anim.HIDE] / 2) {
            for (FateCardEntity card : cards) {
                card.setFace(Face.HIDDEN);
                card.setAsk(Ask.NONE);
            }
        } else if (elapsed == SHUFFLE_AT) {
            startShuffle();
        }
    }

    private void startShuffle() {
        int[] order;
        boolean changed;
        do {
            order = shuffledIndices(cards.size(), level.random);
            changed = false;
            for (int i = 0; i < order.length; i++) changed |= order[i] != i;
        } while (!changed);
        Vec3 orbitCenter = center.add(0, FortuneTuning.ORBIT_HEIGHT, 0);
        for (int i = 0; i < order.length; i++) {
            FateCardEntity card = cards.get(i);
            card.setAnim(Anim.IDLE_HIDDEN);
            int degrees = Math.floorMod(i * 360 / order.length - (int) direction.toYRot(), 360);
            card.startShuffle(orbitCenter, degrees, slot(order[i]));
        }
        play(center, FSSounds.CARD_SHUFFLE.get());
        beginPhase(Phase.SHUFFLING, SHUFFLE_TICKS);
    }

    private void awaitPick() {
        for (FateCardEntity card : cards) {
            card.setAnim(Anim.IDLE_QUESTION);
            card.setAsk(isBlind() ? Ask.UNKNOWN : faces[correct]);
        }
        beginPhase(Phase.AWAIT_PICK, FortuneTuning.PICK_WINDOW_TICKS);
    }

    boolean canPick() {
        return phase == Phase.AWAIT_PICK && picked == -1
                && level.getServer().getTickCount() - phaseStartedAt < FortuneTuning.PICK_WINDOW_TICKS;
    }

    boolean tryPick(int entityId) {
        for (int i = 0; i < cards.size(); i++) {
            FateCardEntity card = cards.get(i);
            if (card.getId() != entityId) continue;
            if (!card.isAlive() || card.level() != owner.level() || level.getEntity(entityId) != card
                    || owner.distanceToSqr(card) > FortuneTuning.MAX_PICK_DISTANCE * FortuneTuning.MAX_PICK_DISTANCE) {
                return false;
            }
            choose(i, false);
            return true;
        }
        return false;
    }

    private void choose(int index, boolean timedOut) {
        picked = index;
        timeout = timedOut;
        if (picked == correct) {
            pendingEffect = ResultEffect.BLESSED;
            effectTicks = isBlind() ? FortuneTuning.BLIND_WIN_TICKS
                    : FortuneTuning.chosenDurationSeconds(spellLevel) * FortuneTuning.TICKS_PER_SECOND;
        } else if (isBlind()) {
            pendingEffect = ResultEffect.FORSAKEN;
            effectTicks = FortuneTuning.BLIND_LOSE_TICKS;
        }
        for (int i = 0; i < cards.size(); i++) {
            if (i != picked) cards.get(i).setAsk(Ask.NONE);
        }
        flip(cards.get(picked));
        beginPhase(Phase.REVEAL, FortuneTuning.REVEAL_PHASE_TICKS);
    }

    private void tickReveal(int elapsed) {
        FateCardEntity selected = cards.get(picked);
        FateCardEntity truth = cards.get(correct);
        boolean won = picked == correct;
        int turn = Anim.DURATION[Anim.REVEAL];
        if (elapsed == turn / 2) {
            selected.setFace(faces[picked]);
        } else if (elapsed == turn) {
            selected.setAsk(Ask.NONE);
            selected.setAnim(Anim.IDLE_SHOW);
            selected.setResult(won ? Result.PICKED_WIN : Result.PICKED_LOSE);
            settle();
            play(selected, won ? FSSounds.CARD_RIGHT.get() : FSSounds.CARD_WRONG.get());
            if (isBlind() && !won) play(center, FSSounds.FATE_LAUGH.get());
            FSNetwork.sendTo(owner, new S2CCardGameResult(id, selected.getId(), truth.getId(), won,
                    timeout, pendingEffect, effectTicks));
            if (!won) flip(truth);
        } else if (elapsed == turn + turn / 2 && !won) {
            truth.setFace(faces[correct]);
        } else if (elapsed == 2 * turn) {
            if (!won) {
                truth.setAnim(Anim.IDLE_SHOW);
                truth.setResult(Result.TRUTH);
            }
            for (int i = 0; i < cards.size(); i++) {
                if (i != picked && i != correct) cards.get(i).setAnim(Anim.FADE_OUT);
            }
        } else if (elapsed >= FortuneTuning.REVEAL_PHASE_TICKS) {
            finish(EndReason.FINISHED);
        }
    }

    private void flip(FateCardEntity card) {
        card.setAnim(Anim.REVEAL);
        play(card, FSSounds.CARD_FLIP.get());
    }

    private void settle() {
        if (picked < 0 || settled) return;
        settled = true;
        if (pendingEffect == ResultEffect.NONE) return;
        boolean blessed = pendingEffect == ResultEffect.BLESSED;
        owner.addEffect(new MobEffectInstance(blessed ? FSEffects.BLESSED_BY_FORTUNE.get()
                : FSEffects.CURSED_BY_FATE.get(), effectTicks));
        FSNetwork.sendTracking(owner, new S2CFortuneFx(blessed ? FxKind.BLESSED_GAINED
                : FxKind.FORSAKEN_GAINED, owner.getId(), 0));
    }

    private void beginPhase(byte next, int duration) {
        phase = next;
        phaseStartedAt = level.getServer().getTickCount();
        FSNetwork.sendTo(owner, new S2CCardGamePhase(id, next, duration, EndReason.FINISHED));
    }

    void finish(byte reason) {
        if (phase == Phase.ENDED) return;
        phase = Phase.ENDED;
        settle();
        cards.forEach(FateCardEntity::discard);
        if (!owner.hasDisconnected()) {
            FSNetwork.sendTo(owner, new S2CCardGamePhase(id, Phase.ENDED, 0, reason));
        }
    }

    private void play(FateCardEntity card, SoundEvent sound) {
        play(card.position(), sound);
    }

    private void play(Vec3 position, SoundEvent sound) {
        level.playSound(null, position.x, position.y, position.z, sound, SoundSource.PLAYERS, 1, 1);
    }
}
