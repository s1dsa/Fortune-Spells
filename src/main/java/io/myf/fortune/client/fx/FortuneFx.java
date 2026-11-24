package io.myf.fortune.client.fx;

import io.myf.fortune.FortuneIds;
import io.myf.fortune.FortuneIds.FxKind;
import io.myf.fortune.client.movement.BoneWingRenderer;
import io.myf.fortune.client.audience.StaffOrbitLayer;
import io.myf.fortune.network.S2CFortuneFx;
import io.myf.fortune.registry.FSEffects;
import io.myf.fortune.registry.FSParticles;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.render.animation.AnimationHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 全部客户端特效（DESIGN 9.5）。声音由服务端播，这里只管粒子。
 * 单次爆发不超过 60 个粒子，并按「粒子」画质设置缩减数量。
 */
public final class FortuneFx {
    private FortuneFx() {}

    private static final RandomSource RANDOM = RandomSource.create();
    private static final List<Ring> RINGS = new ArrayList<>();
    private static final String CARD_SELECTION_ID = FortuneIds.SPELL_CARD_SELECTION.toString();
    private static final String BLIND_GAMBLE_ID = FortuneIds.SPELL_BLIND_GAMBLE.toString();
    private static final ResourceLocation SUNSET_FOREHAND = FortuneIds.id("sunset_forehand");
    private static final ResourceLocation SUNSET_BACKHAND = FortuneIds.id("sunset_backhand");

    private static int ticks;
    private static int victoryRingAt = -1;

    // ---------------------------------------------------------------- 服务端触发

    public static void onFx(S2CFortuneFx msg) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        Entity entity = level.getEntity(msg.entityId());
        if (entity == null) {
            return;
        }
        switch (msg.kind()) {
            case FxKind.STAFF_ORBIT -> {
                if (entity instanceof Player player) StaffOrbitLayer.sync(player, (int) msg.amount());
            }
            case FxKind.FATE_STRIKE -> fateStrike(level, entity);
            case FxKind.ALL_IN_STRIKE -> allInStrike(level, entity);
            case FxKind.ALL_IN_PRIMED -> RINGS.add(new Ring(entity.getId()));
            case FxKind.BLESSED_GAINED -> blessedHalo(level, entity);
            case FxKind.FORSAKEN_GAINED -> forsakenFall(level, entity, 20);
            case FxKind.WINGS_UNFOLD -> {
                if (entity instanceof Player player) BoneWingRenderer.unfold(player);
            }
            case FxKind.WINGS_SHATTER -> {
                if (entity instanceof Player player) BoneWingRenderer.shatter(player);
            }
            case FxKind.SUNSET_SWING -> {
                if (entity instanceof AbstractClientPlayer player) {
                    int stroke = (int) msg.amount();
                    boolean offhand = (stroke & 2) != 0;
                    boolean leftArm = (player.getMainArm() == HumanoidArm.LEFT) ^ offhand;
                    // Iron mirrors the animation onto the casting arm. Reverse the mirrored
                    // stroke as well so the hand still follows the world-space crescent.
                    boolean reverse = ((stroke & 1) != 0) ^ leftArm;
                    AnimationHelper.animatePlayerStart(player, reverse ? SUNSET_BACKHAND : SUNSET_FOREHAND);
                }
            }
            default -> { }
        }
    }

    /** 被命运选中触发：胸口一道金色斜斩 + 散开的金光 + 少量纸片。 */
    private static void fateStrike(ClientLevel level, Entity target) {
        Vec3 chest = target.position().add(0, target.getBbHeight() * 0.6, 0);
        Vec3 right = horizontalRight(target);
        int slash = count(8);
        for (int i = 0; i < slash; i++) {
            double t = slash == 1 ? 0 : i / (double) (slash - 1) - 0.5;
            Vec3 p = chest.add(right.scale(t * 1.2)).add(0, -t * 1.0, 0);
            spawn(level, FSParticles.FATE_SPARK.get(), p, Vec3.ZERO, FortunePalette.GOLD);
        }
        burst(level, FSParticles.FATE_SPARK.get(), chest, count(12), 0.25, FortunePalette.GOLD);
        burst(level, FSParticles.CARD_SHARD.get(), chest, count(6), 0.2, -1);
    }

    /** 孤注一掷命中：红色纸片 / 筹码炸开。 */
    private static void allInStrike(ClientLevel level, Entity target) {
        Vec3 chest = target.position().add(0, target.getBbHeight() * 0.6, 0);
        burst(level, FSParticles.CARD_SHARD.get(), chest, count(20), 0.3, -1);
        burst(level, FSParticles.FATE_SPARK.get(), chest, count(10), 0.2, FortunePalette.CHIP_RED);
    }

    /** 获得被命运选中：头顶金色星环。 */
    private static void blessedHalo(ClientLevel level, Entity entity) {
        Vec3 head = entity.position().add(0, entity.getBbHeight() + 0.35, 0);
        sparkRing(level, head, 0.5);
    }

    private static void sparkRing(ClientLevel level, Vec3 center, double radius) {
        int n = count(16);
        for (int i = 0; i < n; i++) {
            double a = i * Mth.TWO_PI / n;
            Vec3 dir = new Vec3(Math.cos(a), 0, Math.sin(a));
            spawn(level, FSParticles.FATE_SPARK.get(), center.add(dir.scale(radius)), dir.scale(0.04).add(0, 0.02, 0),
                    FortunePalette.GOLD);
        }
    }

    /** 被命运背弃：灰紫微尘从头顶落下。 */
    private static void forsakenFall(ClientLevel level, Entity entity, int amount) {
        double w = entity.getBbWidth() * 0.6;
        Vec3 top = entity.position().add(0, entity.getBbHeight() + 0.4, 0);
        int n = count(amount);
        for (int i = 0; i < n; i++) {
            Vec3 p = top.add((RANDOM.nextDouble() - 0.5) * 2 * w, RANDOM.nextDouble() * 0.3, (RANDOM.nextDouble() - 0.5) * 2 * w);
            spawn(level, FSParticles.FORSAKEN_MOTE.get(), p, new Vec3(0, -0.02, 0), -1);
        }
    }

    // ---------------------------------------------------------------- 牌局

    /** 一张牌出现：6 个纸片从牌中心弹出。 */
    public static void cardAppear(Entity card) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        burst(level, FSParticles.CARD_SHARD.get(), cardCenter(card), count(6), 0.12, -1);
    }

    /** 洗牌拖尾：牌身后 1 个金光。 */
    public static void shuffleTrail(Entity card) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || particleStatus() == ParticleStatus.MINIMAL) {
            return;
        }
        spawn(level, FSParticles.FATE_SPARK.get(), cardCenter(card), Vec3.ZERO, FortunePalette.GOLD);
    }

    /** 揭晓：猜中金光爆开 + 纸片上抛 + 玩家脚下金环；猜错灰紫微尘下坠。 */
    public static void cardResult(Entity picked, boolean won) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        Vec3 c = cardCenter(picked);
        if (won) {
            burst(level, FSParticles.FATE_SPARK.get(), c, count(30), 0.3, FortunePalette.GOLD);
            int shards = count(20);
            for (int i = 0; i < shards; i++) {
                Vec3 v = new Vec3((RANDOM.nextDouble() - 0.5) * 0.25, 0.2 + RANDOM.nextDouble() * 0.2, (RANDOM.nextDouble() - 0.5) * 0.25);
                spawn(level, FSParticles.CARD_SHARD.get(), c, v, -1);
            }
            // Keep the 16 foot sparks separate from the 50-particle card burst.
            victoryRingAt = ticks + 2;
        } else {
            int n = count(15);
            for (int i = 0; i < n; i++) {
                Vec3 p = c.add((RANDOM.nextDouble() - 0.5) * 1.1, (RANDOM.nextDouble() - 0.3) * 1.2, (RANDOM.nextDouble() - 0.5) * 0.3);
                spawn(level, FSParticles.FORSAKEN_MOTE.get(), p, new Vec3(0, -0.03, 0), -1);
            }
        }
    }

    // ---------------------------------------------------------------- 持续特效

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) {
            if (level == null) {
                reset();
            }
            return;
        }
        ticks++;
        if (victoryRingAt == ticks && mc.player != null) {
            sparkRing(level, mc.player.position().add(0, 0.1, 0), 0.8);
            victoryRingAt = -1;
        }

        // 孤注一掷蓄力：红色光点绕施法者转一圈
        Iterator<Ring> it = RINGS.iterator();
        while (it.hasNext()) {
            Ring ring = it.next();
            Entity e = level.getEntity(ring.entityId);
            if (e == null || ring.age >= 10) {
                it.remove();
                continue;
            }
            int age = ring.age++;
            int sparkCount = count(20);
            int first = age * sparkCount / 10;
            int end = (age + 1) * sparkCount / 10;
            for (int i = first; i < end; i++) {
                double angle = age * Mth.TWO_PI / 10 + (i - first) * Mth.PI;
                Vec3 at = e.position().add(Math.cos(angle) * 0.9, e.getBbHeight() * 0.5, Math.sin(angle) * 0.9);
                spawn(level, FSParticles.FATE_SPARK.get(), at, Vec3.ZERO, FortunePalette.CHIP_RED);
            }
        }

        // 吟唱：命运之选 / 盲眼赌局的施法者双手间绕圈金光（自己和附近其他玩家）
        for (AbstractClientPlayer p : level.players()) {
            if (p.distanceToSqr(mc.cameraEntity != null ? mc.cameraEntity : p) > 48 * 48) {
                continue;
            }
            var data = ClientMagicData.getSyncedSpellData(p);
            if (!data.isCasting()) {
                continue;
            }
            String spell = data.getCastingSpellId();
            if (!CARD_SELECTION_ID.equals(spell) && !BLIND_GAMBLE_ID.equals(spell)) {
                continue;
            }
            Vec3 look = p.getViewVector(1f);
            Vec3 hands = p.position().add(0, p.getBbHeight() * 0.62, 0).add(look.x * 0.5, 0, look.z * 0.5);
            double a = (ticks + p.getId()) * 0.45;
            Vec3 side = horizontalRight(p);
            Vec3 offset = side.scale(Math.cos(a) * 0.8).add(0, Math.sin(a) * 0.8, 0);
            spawn(level, FSParticles.FATE_SPARK.get(), hands.add(offset), Vec3.ZERO, FortunePalette.GOLD);
        }

        LocalPlayer self = mc.player;
        if (self == null) {
            return;
        }
        // 孤注一掷已就绪：主手每 10 tick 冒一个红色光点
        if (ticks % 10 == 0 && self.hasEffect(FSEffects.ALL_IN.get())) {
            spawn(level, FSParticles.FATE_SPARK.get(), mainHand(self), new Vec3(0, 0.03, 0), FortunePalette.CHIP_RED);
        }
        // 被命运背弃期间：每 20 tick 一粒微尘
        if (ticks % 20 == 0 && self.hasEffect(FSEffects.CURSED_BY_FATE.get())) {
            forsakenFall(level, self, 1);
        }
    }

    public static void reset() {
        RINGS.clear();
        victoryRingAt = -1;
        ticks = 0;
    }

    // ---------------------------------------------------------------- 工具

    private static Vec3 cardCenter(Entity card) {
        return card.position().add(0, card.getBbHeight() * 0.5, 0);
    }

    private static Vec3 horizontalRight(Entity e) {
        float yaw = e.getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
    }

    private static Vec3 mainHand(LocalPlayer p) {
        Vec3 look = p.getViewVector(1f);
        Vec3 right = horizontalRight(p).scale(p.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1 : -1);
        return p.getEyePosition(1f).add(look.scale(0.5)).add(right.scale(0.35)).add(0, -0.45, 0);
    }

    private static void burst(ClientLevel level, ParticleOptions type, Vec3 at, int n, double speed, int color) {
        for (int i = 0; i < n; i++) {
            Vec3 v = new Vec3(RANDOM.nextGaussian(), RANDOM.nextGaussian(), RANDOM.nextGaussian()).normalize().scale(speed * (0.5 + RANDOM.nextDouble() * 0.5));
            spawn(level, type, at, v, color);
        }
    }

    /** color = -1 表示不染色（用贴图原色）。 */
    private static void spawn(ClientLevel level, ParticleOptions type, Vec3 at, Vec3 velocity, int color) {
        Particle particle = Minecraft.getInstance().particleEngine.createParticle(type, at.x, at.y, at.z, velocity.x, velocity.y, velocity.z);
        if (particle != null && color >= 0) {
            particle.setColor(FortunePalette.r(color), FortunePalette.g(color), FortunePalette.b(color));
        }
    }

    private static ParticleStatus particleStatus() {
        return Minecraft.getInstance().options.particles().get();
    }

    /** 按粒子画质缩减：全部 ×1、减少 ×0.5、最少 ×0.25（至少 1 个）。 */
    private static int count(int full) {
        return switch (particleStatus()) {
            case ALL -> full;
            case DECREASED -> Math.max(1, full / 2);
            case MINIMAL -> Math.max(1, full / 4);
        };
    }

    private static final class Ring {
        final int entityId;
        int age;

        Ring(int entityId) {
            this.entityId = entityId;
        }
    }
}
