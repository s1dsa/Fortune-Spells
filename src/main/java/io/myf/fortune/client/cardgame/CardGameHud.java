package io.myf.fortune.client.cardgame;

import io.myf.fortune.FortuneIds;
import io.myf.fortune.FortuneIds.Face;
import io.myf.fortune.FortuneIds.Phase;
import io.myf.fortune.client.fx.FortunePalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * 猜牌 HUD（DESIGN 9.6）：屏幕上方 25% 处居中的提示文字和倒计时条。
 * 倒计时条用 fill 直接画像素块，不依赖贴图。
 */
public final class CardGameHud implements IGuiOverlay {
    public static final CardGameHud INSTANCE = new CardGameHud();

    private static final int BAR_WIDTH = 120;
    private static final int BAR_HEIGHT = 5;
    private static final int LINE_GAP = 2;
    /** 剩这么多 tick 时倒计时条变红并闪烁。 */
    private static final int WARN_TICKS = 60;

    private static final Component REMEMBER = hud("remember");
    private static final Component[] REMEMBER_SUITS = {
            rememberSuit("♣", FortunePalette.IVORY),
            rememberSuit("♥", FortunePalette.CHIP_RED),
            rememberSuit("♦", FortunePalette.CHIP_RED),
            rememberSuit("♠", FortunePalette.IVORY),
            rememberSuit("★", FortunePalette.GOLD)
    };
    private static final Component SHUFFLING = hud("shuffling");
    private static final Component PICK = hud("pick");
    private static final Component PICK_HINT = hud("pick_hint");
    private static final Component TIMEOUT = hud("timeout");
    private static final Component WIN = hud("win");
    private static final Component LOSE = hud("lose");
    private static final Component CANCELLED = hud("cancelled");

    private CardGameHud() {}

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) {
            return;
        }
        Font font = mc.font;
        int centerX = width / 2;
        int y = height / 4;

        if (!ClientCardGame.isActive()) {
            byte linger = ClientCardGame.lingerMessage();
            if (linger == ClientCardGame.LINGER_RESULT) {
                drawResult(graphics, font, centerX, y);
            } else if (linger == ClientCardGame.LINGER_CANCELLED) {
                drawCentered(graphics, font, CANCELLED, centerX, y, 0xAAAAAA);
            }
            return;
        }

        switch (ClientCardGame.phase()) {
            case Phase.SHOW_TARGET -> {
                byte face = ClientCardGame.targetFace();
                Component line = face >= 0 && face < Face.COUNT ? REMEMBER_SUITS[face] : REMEMBER;
                drawCentered(graphics, font, line, centerX, y, FortunePalette.GOLD);
            }
            case Phase.SHUFFLING -> drawCentered(graphics, font, SHUFFLING, centerX, y, FortunePalette.IVORY);
            case Phase.AWAIT_PICK -> {
                drawCentered(graphics, font, PICK, centerX, y, FortunePalette.GOLD);
                int hintY = y + font.lineHeight + LINE_GAP;
                if (!ClientCardGame.hasPicked()) {
                    drawCentered(graphics, font, PICK_HINT, centerX, hintY, 0xAAAAAA);
                }
                drawBar(graphics, centerX, hintY + font.lineHeight + 4, partialTick);
            }
            case Phase.REVEAL -> {
                if (ClientCardGame.hasResult()) {
                    drawResult(graphics, font, centerX, y);
                }
            }
            default -> { }
        }
    }

    private static void drawResult(GuiGraphics graphics, Font font, int centerX, int y) {
        if (ClientCardGame.timedOut()) {
            drawCentered(graphics, font, TIMEOUT, centerX, y, 0xAAAAAA);
            y += font.lineHeight + LINE_GAP;
        }
        if (ClientCardGame.won()) {
            drawCentered(graphics, font, WIN, centerX, y, FortunePalette.GOLD);
        } else {
            drawCentered(graphics, font, LOSE, centerX, y, FortunePalette.FORSAKEN_LIGHT);
        }
    }

    private static void drawBar(GuiGraphics graphics, int centerX, int y, float partialTick) {
        float remaining = ClientCardGame.phaseRemaining(partialTick);
        int ticksLeft = ClientCardGame.phaseTicksLeft();
        boolean warn = ticksLeft <= WARN_TICKS;
        int left = centerX - BAR_WIDTH / 2;
        // 1px 天鹅绒描边 + 牌背墨底
        graphics.fill(left - 1, y - 1, left + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, 0xFF000000 | FortunePalette.VELVET);
        graphics.fill(left, y, left + BAR_WIDTH, y + BAR_HEIGHT, 0xFF000000 | FortunePalette.CARD_INK);
        int filled = Math.round(BAR_WIDTH * remaining);
        if (filled <= 0) {
            return;
        }
        if (warn && (ClientCardGame.clientTicks() / 4) % 2 == 1) {
            return; // 闪烁
        }
        int color = warn ? FortunePalette.CHIP_RED : lerpColor(FortunePalette.CHIP_RED, FortunePalette.GOLD, remaining);
        graphics.fill(left, y, left + filled, y + BAR_HEIGHT, 0xFF000000 | color);
        // 顶部 1px 高光
        graphics.fill(left, y, left + filled, y + 1, 0x60FFFFFF);
    }

    private static int lerpColor(int from, int to, float t) {
        int r = (int) (((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = (int) (((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (r << 16) | (g << 8) | b;
    }

    private static void drawCentered(GuiGraphics graphics, Font font, Component text, int centerX, int y, int color) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, 0xFF000000 | color, true);
    }

    private static MutableComponent hud(String key) {
        return Component.translatable(FortuneIds.LANG_HUD + key);
    }

    private static Component rememberSuit(String suit, int color) {
        return hud("remember").append(Component.literal("  " + suit).withStyle(Style.EMPTY.withColor(color)));
    }
}
