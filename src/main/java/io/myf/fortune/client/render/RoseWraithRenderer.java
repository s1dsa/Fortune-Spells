package io.myf.fortune.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.myf.fortune.expansion.rose.RoseWraith;
import lykrast.meetyourfight.renderer.RoseSpiritRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class RoseWraithRenderer extends EntityRenderer<RoseWraith> {
    private final RoseSpiritRenderer spirit;

    public RoseWraithRenderer(EntityRendererProvider.Context context) {
        super(context);
        spirit = new RoseSpiritRenderer(context);
    }

    @Override
    public ResourceLocation getTextureLocation(RoseWraith wraith) {
        return spirit.getTextureLocation(wraith);
    }

    @Override
    public boolean shouldRender(RoseWraith wraith, Frustum frustum, double x, double y, double z) {
        if (super.shouldRender(wraith, frustum, x, y, z)) return true;
        if (!wraith.shouldRender(x, y, z)) return false;
        Entity caster = wraith.link();
        if (wraith.phase() == RoseWraith.Phase.GUARDING && caster != null) {
            return frustum.isVisible(wraith.getBoundingBox().minmax(caster.getBoundingBox()));
        }
        return wraith.phase() == RoseWraith.Phase.SUMMONING
                && frustum.isVisible(wraith.getBoundingBox().inflate(1.3));
    }

    @Override
    public void render(RoseWraith wraith, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        if (wraith.phase() == RoseWraith.Phase.SUMMONING) {
            shadowRadius = 0;
            renderSeal(wraith, partial, pose, buffers);
            return;
        }
        shadowRadius = .5f;
        spirit.render(wraith, yaw, partial, pose, buffers, light);
        Entity caster = wraith.link();
        if (wraith.phase() == RoseWraith.Phase.GUARDING && caster != null) {
            Vec3 offset = caster.getPosition(partial).add(0, caster.getBbHeight() * .6, 0)
                    .subtract(wraith.getPosition(partial).add(0, 1, 0));
            RoseSpiritRenderer.renderCrystalBeams((float) offset.x, (float) offset.y, (float) offset.z,
                    partial, wraith.tickCount, pose, buffers, light);
        }
    }

    private static void renderSeal(RoseWraith wraith, float partial, PoseStack pose, MultiBufferSource buffers) {
        float t = Mth.clamp(wraith.summonProgress(partial), 0, 1);
        float eased = t * t * (3 - 2 * t);
        float radius = .25f + .9f * eased;
        pose.pushPose();
        // Tilt the seal toward the summoner so its star is visible from eye level.
        Entity caster = wraith.link();
        if (caster != null) {
            Vec3 toCaster = caster.getPosition(partial).subtract(wraith.getPosition(partial));
            pose.mulPose(Axis.YP.rotation((float) Mth.atan2(toCaster.x, toCaster.z)));
        }
        pose.mulPose(Axis.XP.rotationDegrees(35));
        pose.scale(radius, radius, radius);
        pose.mulPose(Axis.YP.rotationDegrees(25 * eased));
        VertexConsumer ink = buffers.getBuffer(RenderType.debugQuads());
        Matrix4f matrix = pose.last().pose();
        int rose = 0xCB426E, light = 0xFFDEDB;
        for (int i = 0; i < 48; i++) {
            float a = i * Mth.TWO_PI / 48, b = (i + 1) * Mth.TWO_PI / 48;
            stroke(ink, matrix, a, b, 1, .024f, rose, 210, 1);
            stroke(ink, matrix, a, b, .94f, .007f, light, 220, 1);
        }
        pose.mulPose(Axis.YP.rotationDegrees(-52 * eased));
        float drawn = Mth.clamp(t / .8f, 0, 1) * 5;
        for (int i = 0; i < 5 && i < drawn; i++) {
            float a = i * 2 * Mth.TWO_PI / 5, b = (i + 1) * 2 * Mth.TWO_PI / 5;
            float fraction = Math.min(1, drawn - i);
            stroke(ink, matrix, a, b, .88f, .034f, rose, 235, fraction);
            stroke(ink, matrix, a, b, .88f, .012f, light, 250, fraction);
        }
        pose.popPose();
    }

    private static void stroke(VertexConsumer out, Matrix4f matrix, float a, float b, float radius,
                               float halfWidth, int rgb, int alpha, float fraction) {
        float x = Mth.sin(a) * radius, z = Mth.cos(a) * radius;
        float dx = Mth.sin(b) * radius - x, dz = Mth.cos(b) * radius - z;
        float width = halfWidth / Mth.sqrt(dx * dx + dz * dz);
        float nx = dz * width, nz = -dx * width;
        float endX = x + dx * fraction, endZ = z + dz * fraction;
        vertex(out, matrix, x - nx, z - nz, rgb, alpha);
        vertex(out, matrix, endX - nx, endZ - nz, rgb, alpha);
        vertex(out, matrix, endX + nx, endZ + nz, rgb, alpha);
        vertex(out, matrix, x + nx, z + nz, rgb, alpha);
    }

    private static void vertex(VertexConsumer out, Matrix4f matrix, float x, float z, int rgb, int alpha) {
        out.vertex(matrix, x, 0, z).color(rgb >> 16, (rgb >> 8) & 255, rgb & 255, alpha).endVertex();
    }
}
