package io.myf.fortune.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.myf.fortune.FortuneIds;
import io.myf.fortune.FortuneIds.Anim;
import io.myf.fortune.FortuneIds.Face;
import io.myf.fortune.FortuneIds.Result;
import io.myf.fortune.client.cardgame.ClientCardGame;
import io.myf.fortune.client.fx.FortunePalette;
import io.myf.fortune.entity.FateCardEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * 命运之牌渲染器（DESIGN 8.3 / 9.3 / 9.5）。
 * 牌面、牌背、背面目标标记在运行时引用迎战的贴图；Joker 牌面和「?」标记用我们自己的贴图。
 * 只读实体的同步数据和客户端动画计时，不改任何状态。
 */
public class FateCardRenderer extends EntityRenderer<FateCardEntity> {
    private static final String MYF = "meetyourfight";

    private static final ResourceLocation[] FACES = {
            new ResourceLocation(MYF, "textures/entity/fortuna_card_club.png"),
            new ResourceLocation(MYF, "textures/entity/fortuna_card_heart.png"),
            new ResourceLocation(MYF, "textures/entity/fortuna_card_diamond.png"),
            new ResourceLocation(MYF, "textures/entity/fortuna_card_spade.png"),
            FortuneIds.id("textures/entity/fate_card/joker.png"),
    };
    private static final ResourceLocation BACK = new ResourceLocation(MYF, "textures/entity/fortuna_card_hidden.png");
    /** 背面目标标记，下标 = ASK 取值（0..4 牌面，5 = 未知「?」）。 */
    private static final ResourceLocation[] ASK = {
            new ResourceLocation(MYF, "textures/entity/fortuna_card_ask_club.png"),
            new ResourceLocation(MYF, "textures/entity/fortuna_card_ask_heart.png"),
            new ResourceLocation(MYF, "textures/entity/fortuna_card_ask_diamond.png"),
            new ResourceLocation(MYF, "textures/entity/fortuna_card_ask_spade.png"),
            FortuneIds.id("textures/entity/fate_card/ask_joker.png"),
            FortuneIds.id("textures/entity/fate_card/ask_unknown.png"),
    };

    /** 整张牌的渲染缩放（DESIGN 9.3）：28×40 像素 → 约 1.23×1.75 格。 */
    private static final float CARD_SCALE = 0.7f;
    private static final float HOVER_LIFT = 0.15f;
    private static final float HOVER_SCALE = 1.05f;
    /** 发光边框宽度（部件坐标，缩放前）。 */
    private static final float FRAME = 0.1f;
    private static final float FRAME_Z = FateCardModel.HALF_DEPTH + 0.01f;
    private static final float[] FRAME_DEPTHS = {-FRAME_Z, FRAME_Z};
    // Untextured, alpha blended and back-face culled. The result color stays legible against the sky.
    private static final RenderType FRAME_TYPE = RenderType.gui();

    private final FateCardModel model;

    public FateCardRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new FateCardModel(context.bakeLayer(FateCardModel.LAYER));
        this.shadowRadius = 0f;
    }

    @Override
    protected int getBlockLightLevel(FateCardEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    public ResourceLocation getTextureLocation(FateCardEntity entity) {
        return faceTexture(entity.getFace());
    }

    public static ResourceLocation faceTexture(int face) {
        return face >= 0 && face < FACES.length ? FACES[face] : BACK;
    }

    @Override
    public void render(FateCardEntity card, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        int anim = card.clientAnim;
        if (anim == Anim.NOT_HERE || anim < 0 || anim >= Anim.DURATION.length) {
            return;
        }

        float progress = progress(card, partialTick);
        float yaw = Mth.rotLerp(partialTick, card.yRotO, card.getYRot());
        float alpha = 1f;
        float size = 1f;
        float appear = 1f;
        switch (anim) {
            case Anim.APPEAR -> {
                float p = progress * progress;
                appear = p * p;
            }
            case Anim.HIDE, Anim.REVEAL -> yaw = Mth.wrapDegrees(yaw + progress * 360f);
            case Anim.HINT -> yaw = Mth.wrapDegrees(yaw + Mth.sin(progress * 3f * Mth.PI) * 30f);
            case Anim.FADE_OUT -> {
                alpha = Mth.clamp(1f - progress, 0f, 1f);
                size = 1f - 0.2f * progress;
            }
            default -> { }
        }
        if (alpha <= 0.01f || appear <= 0f) {
            return;
        }

        boolean hovered = ClientCardGame.isHighlighted(card.getId());

        poseStack.pushPose();
        if (hovered) {
            poseStack.translate(0f, HOVER_LIFT, 0f);
        }
        float scale = CARD_SCALE * size * (hovered ? HOVER_SCALE : 1f);
        poseStack.scale(scale, scale, scale);
        // 模型坐标 y 朝下，翻过来再上移，和原版实体模型的约定一致
        poseStack.scale(-1f, -1f, 1f);
        poseStack.translate(0f, -1.5f, 0f);
        if (appear < 1f) {
            poseStack.scale(appear, 1f, appear);
        }
        model.setYaw(yaw);

        boolean translucent = alpha < 1f;
        byte face = card.getFace();
        ResourceLocation baseTexture = face == Face.JOKER ? BACK : getTextureLocation(card);
        model.renderToBuffer(poseStack, buffers.getBuffer(cardType(baseTexture, translucent)),
                packedLight, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, alpha);

        model.translateToCard(poseStack);
        if (face == Face.JOKER) {
            model.renderOverlay(poseStack, buffers.getBuffer(cardType(faceTexture(face), translucent)),
                    packedLight, OverlayTexture.NO_OVERLAY, alpha, false);
        }

        byte ask = card.getAsk();
        if (anim == Anim.IDLE_QUESTION && face == Face.HIDDEN && ask >= 0 && ask < ASK.length) {
            // Upstream suit markers occupy the front UV island. Our Joker and question mark use the back island.
            model.renderOverlay(poseStack, buffers.getBuffer(RenderType.entityTranslucentEmissive(ASK[ask], false)),
                    packedLight, OverlayTexture.NO_OVERLAY, alpha, ask >= Face.JOKER);
        }

        int glow = glowColor(card, anim, hovered, partialTick);
        float glowAlpha = ((glow >>> 24) & 0xFF) / 255f * alpha;
        if (glowAlpha > 0.01f) {
            // Unlit vertex colors with alpha blending retain the result color even against a bright sky.
            renderFrame(poseStack, buffers.getBuffer(FRAME_TYPE), glow, glowAlpha);
        }

        poseStack.popPose();
    }

    private static RenderType cardType(ResourceLocation texture, boolean translucent) {
        return translucent ? RenderType.entityTranslucent(texture) : RenderType.entityCutoutNoCull(texture);
    }

    /** 当前动画进度 0..1；静止动画返回 1。 */
    private static float progress(FateCardEntity card, float partialTick) {
        int duration = Anim.DURATION[card.clientAnim];
        if (duration <= 0) {
            return 1f;
        }
        return Mth.clamp((duration - card.animTimer + partialTick) / duration, 0f, 1f);
    }

    /**
     * 发光边框颜色（ARGB，A 是强度）。0 = 不画。
     * 优先级：揭晓结果 > 本地悬停/已选 > 提示目标。
     */
    private static int glowColor(FateCardEntity card, int anim, boolean highlighted, float partialTick) {
        float t = card.tickCount + partialTick;
        float breathe = 0.5f + 0.5f * Mth.sin(t * Mth.TWO_PI / 20f);
        byte result = card.getResult();
        if (result == Result.PICKED_WIN) {
            return withAlpha(FortunePalette.GOLD, 0.75f + 0.25f * breathe);
        }
        if (result == Result.PICKED_LOSE) {
            return withAlpha(FortunePalette.FORSAKEN_LIGHT, 0.8f);
        }
        if (result == Result.TRUTH) {
            return withAlpha(FortunePalette.GOLD, 0.4f + 0.4f * breathe);
        }
        if (highlighted) {
            return withAlpha(FortunePalette.IVORY, 0.85f);
        }
        // 命运之选的提示阶段：牌面朝上、带目标标记、还没有结果
        if ((anim == Anim.HINT || anim == Anim.IDLE_SHOW) && card.getFace() != Face.HIDDEN && card.getAsk() >= 0) {
            return withAlpha(FortunePalette.GOLD, 0.4f + 0.6f * breathe);
        }
        return 0;
    }

    private static int withAlpha(int rgb, float alpha) {
        int a = Mth.clamp(Math.round(alpha * 255f), 0, 255);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    /** Wind each outline towards its own side so alpha is applied once. */
    private static void renderFrame(PoseStack poseStack, VertexConsumer buffer, int argb, float alpha) {
        Matrix4f pose = poseStack.last().pose();
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        float x0 = -FateCardModel.HALF_WIDTH, x1 = FateCardModel.HALF_WIDTH;
        float y0 = -FateCardModel.HEIGHT, y1 = 0f;
        for (float z : FRAME_DEPTHS) {
            strip(buffer, pose, x0 - FRAME, y1, x1 + FRAME, y1 + FRAME, z, r, g, b, alpha);
            strip(buffer, pose, x0 - FRAME, y0 - FRAME, x1 + FRAME, y0, z, r, g, b, alpha);
            strip(buffer, pose, x0 - FRAME, y0, x0, y1, z, r, g, b, alpha);
            strip(buffer, pose, x1, y0, x1 + FRAME, y1, z, r, g, b, alpha);
        }
    }

    private static void strip(VertexConsumer buffer, Matrix4f pose, float xa, float ya, float xb, float yb, float z,
                              float r, float g, float b, float a) {
        float firstX = z < 0 ? xa : xb;
        float lastX = z < 0 ? xb : xa;
        buffer.vertex(pose, firstX, ya, z).color(r, g, b, a).endVertex();
        buffer.vertex(pose, firstX, yb, z).color(r, g, b, a).endVertex();
        buffer.vertex(pose, lastX, yb, z).color(r, g, b, a).endVertex();
        buffer.vertex(pose, lastX, ya, z).color(r, g, b, a).endVertex();
    }
}
