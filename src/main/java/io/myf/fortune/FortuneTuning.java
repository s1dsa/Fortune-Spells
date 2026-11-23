package io.myf.fortune;

/** Server-side defaults. Spell config overrides and cast modifiers are applied by Iron's Spells. */
public final class FortuneTuning {
    private FortuneTuning() {}

    public static final int TICKS_PER_SECOND = 20;
    public static final int CARD_CAST_TICKS = 3 * TICKS_PER_SECOND;
    public static final int CHOICE_MANA = 60;
    public static final int CHOICE_MANA_PER_LEVEL = 10;
    public static final int CHOICE_MAX_LEVEL = 3;
    public static final int CHOICE_COOLDOWN_SECONDS = 180;
    public static final int BLIND_MANA = 80;
    public static final int BLIND_MAX_LEVEL = 2;
    public static final int BLIND_COOLDOWN_SECONDS = 3600;
    public static final int ALL_IN_MANA = 20;
    public static final int ALL_IN_MAX_LEVEL = 1;
    public static final int ALL_IN_COOLDOWN_SECONDS = 5;

    public static final int BLIND_WIN_TICKS = 15 * 60 * TICKS_PER_SECOND;
    public static final int BLIND_LOSE_TICKS = 30 * 60 * TICKS_PER_SECOND;
    public static final float DOUBLE_DAMAGE = 2.0f;
    public static final float FATE_CHANCE = 0.5f;
    public static final float FATE_STRIKE_PITCH = 1.2f;
    public static final float ALL_IN_STRIKE_PITCH = 0.8f;

    public static final int DEAL_INTERVAL = 6;
    public static final int DEAL_TICKS = 40;
    public static final int HINT_PHASE_TICKS = 40;
    public static final int SHUFFLE_APPROACH_TICKS = 20;
    public static final int SHUFFLE_ACCEL_TICKS = 30;
    public static final int SHUFFLE_LAND_TICKS = 35;
    public static final int SHUFFLE_SETTLE_TICKS = 10;
    public static final int PICK_WINDOW_TICKS = 10 * TICKS_PER_SECOND;
    public static final int REVEAL_PHASE_TICKS = 60;

    public static final double TABLE_HEIGHT = 0.25;
    public static final double TABLE_FORWARD = 5.0;
    public static final double SLOT_SPACING = 1.75;
    public static final double ORBIT_HEIGHT = 2.0;
    public static final double ORBIT_RADIUS = 2.4;
    public static final double ORBIT_APPROACH_ACCEL = 0.05;
    public static final double MAX_TABLE_DISTANCE = 32.0;
    public static final double MAX_PICK_DISTANCE = 24.0;

    public static int cardCount(byte mode, int level) {
        return mode == FortuneIds.Mode.BLIND_GAMBLE && level >= 2 ? 4 : 5;
    }

    public static int chosenDurationSeconds(int level) {
        return 10 + 20 * level;
    }
}
