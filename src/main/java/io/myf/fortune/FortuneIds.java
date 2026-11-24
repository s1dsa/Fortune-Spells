package io.myf.fortune;

import net.minecraft.resources.ResourceLocation;

/**
 * 契约常量（docs/DESIGN.md 第 3、8 节）。opus 维护，后端只读。
 * 改这里之前先改 DESIGN.md 并在频道里说一声。
 * 本类只依赖 ResourceLocation（两端通用），客户端和服务端都可以引用。
 */
public final class FortuneIds {
    private FortuneIds() {}

    public static final String MODID = "fortune_spells";

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }

    // ---- 学派 / 法术 ----
    public static final ResourceLocation SCHOOL_FORTUNE = id("fortune");
    public static final ResourceLocation SPELL_CARD_SELECTION = id("card_selection");
    public static final ResourceLocation SPELL_BLIND_GAMBLE = id("blind_gamble");
    public static final ResourceLocation SPELL_ALL_IN = id("all_in");

    // ---- 状态效果 ----
    public static final ResourceLocation EFFECT_BLESSED_BY_FORTUNE = id("blessed_by_fortune");
    public static final ResourceLocation EFFECT_CURSED_BY_FATE = id("cursed_by_fate");
    public static final ResourceLocation EFFECT_ALL_IN = id("all_in");

    // ---- 伤害类型（ResourceKey 在 registry/FSDamageTypes 里用这两个 id 构造）----
    public static final ResourceLocation DAMAGE_FORTUNE = id("fortune");
    public static final ResourceLocation DAMAGE_FORTUNE_MAGIC = id("fortune_magic");

    // ---- 属性 ----
    public static final ResourceLocation ATTR_FORTUNE_SPELL_POWER = id("fortune_spell_power");
    public static final ResourceLocation ATTR_FORTUNE_MAGIC_RESIST = id("fortune_magic_resist");

    // ---- 实体 / 粒子 ----
    public static final ResourceLocation ENTITY_FATE_CARD = id("fate_card");
    public static final ResourceLocation PARTICLE_FATE_SPARK = id("fate_spark");
    public static final ResourceLocation PARTICLE_CARD_SHARD = id("card_shard");
    public static final ResourceLocation PARTICLE_FORSAKEN_MOTE = id("forsaken_mote");

    // ---- 标签 ----
    /** 物品标签 #fortune_spells:fortune_focus（学派物品 = meetyourfight:fortunes_favor） */
    public static final ResourceLocation TAG_FORTUNE_FOCUS = id("fortune_focus");
    public static final ResourceLocation ITEM_FORTUNES_FAVOR = new ResourceLocation("meetyourfight", "fortunes_favor");

    // ---- 声音事件（sounds.json 里用 "type":"event" 指向迎战/原版）----
    public static final ResourceLocation SOUND_SPELL_FORTUNE_CAST = id("spell.fortune.cast");
    public static final ResourceLocation SOUND_SPELL_FORTUNE_CHARGE = id("spell.fortune.charge");
    public static final ResourceLocation SOUND_CARD_APPEAR = id("card.appear");
    public static final ResourceLocation SOUND_CARD_SHUFFLE = id("card.shuffle");
    public static final ResourceLocation SOUND_CARD_FLIP = id("card.flip");
    public static final ResourceLocation SOUND_CARD_RIGHT = id("card.right");
    public static final ResourceLocation SOUND_CARD_WRONG = id("card.wrong");
    public static final ResourceLocation SOUND_FATE_LAUGH = id("fate.laugh");
    public static final ResourceLocation SOUND_FATE_STRIKE = id("fate.strike");
    public static final ResourceLocation SOUND_ALL_IN_CAST = id("all_in.cast");
    public static final ResourceLocation SOUND_ALL_IN_PROC = id("all_in.proc");

    // ---- 网络 ----
    public static final ResourceLocation CHANNEL_MAIN = id("main");
    public static final String PROTOCOL_VERSION = "1";

    public static final int PKT_S2C_CARD_GAME_START = 0;
    public static final int PKT_S2C_CARD_GAME_PHASE = 1;
    public static final int PKT_S2C_CARD_GAME_RESULT = 2;
    public static final int PKT_C2S_CARD_PICK = 3;
    public static final int PKT_S2C_FORTUNE_FX = 4;

    // ---- 卡牌同步数据取值（DESIGN 8.3）----
    /** FACE：牌面。0..3 和迎战女爵的牌面顺序一致，4 是我们自己画的 Joker。 */
    public static final class Face {
        private Face() {}
        public static final byte HIDDEN = -1;
        public static final byte CLUB = 0;
        public static final byte HEART = 1;
        public static final byte DIAMOND = 2;
        public static final byte SPADE = 3;
        public static final byte JOKER = 4;
        public static final int COUNT = 5;
    }

    /** ASK：背面浮现的目标标记。 */
    public static final class Ask {
        private Ask() {}
        public static final byte NONE = -1;
        // 0..4 = 目标牌面（同 Face）
        public static final byte UNKNOWN = 5;
    }

    /** ANIM：动画编号，0..7 与女爵 FortunaCardEntity 一致。 */
    public static final class Anim {
        private Anim() {}
        public static final byte NOT_HERE = 0;
        public static final byte APPEAR = 1;
        public static final byte IDLE_SHOW = 2;
        public static final byte HIDE = 3;
        public static final byte IDLE_HIDDEN = 4;
        public static final byte IDLE_QUESTION = 5;
        public static final byte REVEAL = 6;
        public static final byte HINT = 7;
        public static final byte FADE_OUT = 8;

        /** 各动画时长（tick），0 = 循环 / 静止。下标即动画编号。 */
        public static final int[] DURATION = {0, 10, 0, 10, 0, 0, 20, 30, 10};
    }

    /** RESULT：揭晓结果描边。 */
    public static final class Result {
        private Result() {}
        public static final byte NONE = 0;
        public static final byte PICKED_WIN = 1;
        public static final byte PICKED_LOSE = 2;
        public static final byte TRUTH = 3;
    }

    // ---- 对局（DESIGN 8.2）----
    public static final class Phase {
        private Phase() {}
        public static final byte DEALING = 0;
        public static final byte SHOW_TARGET = 1;
        public static final byte SHUFFLING = 2;
        public static final byte AWAIT_PICK = 3;
        public static final byte REVEAL = 4;
        public static final byte ENDED = 5;
    }

    public static final class Mode {
        private Mode() {}
        public static final byte FATES_CHOICE = 0;
        public static final byte BLIND_GAMBLE = 1;
    }

    /** S2CCardGamePhase.reason，仅 ENDED 有意义。 */
    public static final class EndReason {
        private EndReason() {}
        public static final byte FINISHED = 0;
        public static final byte CANCELLED = 1;
    }

    /** S2CCardGameResult.effect */
    public static final class ResultEffect {
        private ResultEffect() {}
        public static final byte NONE = 0;
        public static final byte BLESSED = 1;
        public static final byte FORSAKEN = 2;
    }

    /** S2CFortuneFx.kind（DESIGN 8.6） */
    public static final class FxKind {
        private FxKind() {}
        public static final byte FATE_STRIKE = 0;
        public static final byte ALL_IN_STRIKE = 1;
        public static final byte ALL_IN_PRIMED = 2;
        public static final byte BLESSED_GAINED = 3;
        public static final byte FORSAKEN_GAINED = 4;
        public static final byte WINGS_UNFOLD = 5;
        public static final byte WINGS_SHATTER = 6;
        public static final byte SUNSET_SWING = 7;
        public static final byte STAFF_ORBIT = 8;
    }

    // ---- 语言键前缀 ----
    public static final String LANG_UI = "ui." + MODID + ".";
    public static final String LANG_HUD = "hud." + MODID + ".card_game.";
}
