package io.myf.fortune.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;

/** Atlas of the finished dial strokes and crown enamel. Animated strokes remain live geometry. */
final class RitualDialCache {
    // Each render invocation submits at most two floor pieces and twenty teeth before returning.
    private static final Stamp[] STAMPS = new Stamp[22];
    private static final float[] SIN = new float[49], COS = new float[49];
    static {
        for (int i = 0; i < STAMPS.length; i++) STAMPS[i] = new Stamp();
        for (int i = 0; i <= 48; i++) {
            SIN[i] = (float)Math.sin(i * Math.PI / 24);
            COS[i] = (float)Math.cos(i * Math.PI / 24);
        }
    }

    private static final class Stamp {
        final Matrix4f pose = new Matrix4f();
        float u, v, width, height, tileWidth, tileHeight;
        boolean annulus;
    }

    private final RenderType material;
    private int used;

    RitualDialCache(RenderType material) {
        this.material = material;
    }

    void floor(PoseStack pose, boolean dial) {
        Stamp entry = STAMPS[used++];
        entry.pose.set(pose.last().pose()).translate(0, dial ? .038f : .07f, 0);
        entry.width = entry.height = 12.25f;
        entry.u = dial ? .5f : 0;
        entry.v = 0;
        entry.tileWidth = .5f;
        entry.tileHeight = 1;
        entry.annulus = dial;
    }

    void tooth(Matrix4f pose, float radius, int hue, boolean upper) {
        Stamp entry = STAMPS[used++];
        entry.pose.set(pose).translate(0, .22f, radius).rotateX((float)(-Math.PI / 2));
        entry.width = upper ? 1.24f : 1.42f;
        entry.height = 1.02f;
        entry.u = (1152 + 256 * hue) / 2048f;
        entry.v = (upper ? 512 : 352) / 1024f;
        entry.tileWidth = 256 / 2048f;
        entry.tileHeight = 160 / 1024f;
        entry.annulus = false;
    }

    void flush(MultiBufferSource buffers, float intensity) {
        if (used == 0) return;
        VertexConsumer out = buffers.getBuffer(material);
        int alpha = (int)(255 * intensity);
        for (int i = 0; i < used; i++) {
            Stamp s = STAMPS[i];
            if (s.annulus) {
                // Keep the transparent middle out of the rasterizer. The six crown sprites
                // occupy that otherwise unused part of the atlas.
                for (int j = 0; j < 48; j += 2) {
                    vertex(out, s, SIN[j], COS[j], alpha);
                    vertex(out, s, SIN[j+2], COS[j+2], alpha);
                    vertex(out, s, .90f*SIN[j+2], .90f*COS[j+2], alpha);
                    vertex(out, s, .90f*SIN[j], .90f*COS[j], alpha);
                }
            } else {
                vertex(out, s, -1, -1, alpha); vertex(out, s, -1, 1, alpha);
                vertex(out, s, 1, 1, alpha); vertex(out, s, 1, -1, alpha);
            }
        }
    }

    private static void vertex(VertexConsumer out, Stamp s, float x, float z, int alpha) {
        out.vertex(s.pose, x*s.width, 0, z*s.height).color(255,255,255,alpha)
                .uv(s.u + (.5f + .5f*x)*s.tileWidth, s.v + (.5f + .5f*z)*s.tileHeight)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0,1,0).endVertex();
    }
}
