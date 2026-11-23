package io.myf.fortune.client.fx;

/** 美术规范调色板（DESIGN 9.2），RGB。 */
public final class FortunePalette {
    private FortunePalette() {}

    public static final int CHIP_RED = 0xF0506E;
    public static final int DEEP_RED = 0xA51E3A;
    public static final int VELVET = 0x5A0F1E;
    public static final int GOLD = 0xF5C542;
    public static final int DARK_GOLD = 0xB8862B;
    public static final int IVORY = 0xFFF4E0;
    public static final int CARD_INK = 0x2A1A2E;
    public static final int FORSAKEN = 0x6B5B7B;
    public static final int FORSAKEN_DARK = 0x3E3349;
    public static final int FORSAKEN_LIGHT = 0xA99BB8;

    public static float r(int rgb) { return ((rgb >> 16) & 0xFF) / 255f; }
    public static float g(int rgb) { return ((rgb >> 8) & 0xFF) / 255f; }
    public static float b(int rgb) { return (rgb & 0xFF) / 255f; }
}
