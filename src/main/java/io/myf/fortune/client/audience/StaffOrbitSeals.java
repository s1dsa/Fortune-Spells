package io.myf.fortune.client.audience;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

import static io.myf.fortune.client.audience.AudienceGlyphs.Pen;
import static io.myf.fortune.client.audience.AudienceGlyphs.Seal;

/** Eight celestial seals surrounding an armillary sphere. Each pen path is built once. */
final class StaffOrbitSeals {
    private static final int BRASS = 0xB17842;
    private static final int GOLD = 0xF0BA65;
    private static final int IVORY = 0xFFF1C8;
    private static final int GEM = -1;
    static final int AXIS = 8;
    private static final Seal BEZEL = bezel();
    private static final Seal DIAL = dial();
    private record Face(Seal drawing, int enamel) {}
    private static final Face[] FACES = {
            new Face(compass(), 0x8DE8E6), new Face(hourglass(), 0xFFBB83),
            new Face(moon(), 0xC3A5FA), new Face(rosette(), 0x8BE3B5),
            new Face(sun(), 0xFF927C), new Face(scales(), 0xFFADCB),
            new Face(knot(), 0x9BD6FF), new Face(eclipses(), 0xAEB9FF),
            new Face(armillary(), 0xFFE19B)
    };

    private StaffOrbitSeals() {}

    /** Local XY face centered on (0,0), nominal radius 1. Age is the interpolated held time in ticks. */
    static void render(PoseStack pose, MultiBufferSource buffers, int index, float progress, float age) {
        if (progress <= 0) return;
        VertexConsumer lines = buffers.getBuffer(RenderType.debugQuads());
        Face face = FACES[index];
        int accent = face.enamel;
        pose.pushPose();
        // AudienceGlyphs' pen draws on its local XZ sheet; turn that sheet toward the back viewer.
        pose.mulPose(Axis.XP.rotationDegrees(-90));
        BEZEL.draw(pose.last().pose(), lines, accent, 255, progress / .46f);
        float rotation = (index % 2 == 0 ? 1 : -1) * age * (360f / 1600);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        DIAL.draw(pose.last().pose(), lines, accent, 246, (progress - .20f) / .60f);
        pose.popPose();
        face.drawing.draw(pose.last().pose(), lines, accent, 255, (progress - .44f) / .56f);
        pose.popPose();
    }

    private static Seal bezel() {
        Pen p = new Pen();
        p.arc(.25f, 1.25f, .977f, 48, .046f, GOLD);
        // Four broad ivory inlays give the brass rim a readable silhouette at inventory-screen scale.
        for (int i = 0; i < 4; i++) {
            float a = i / 4f;
            p.arc(a + .027f, a + .18f, .92f, 8, .025f, BRASS);
            p.arc(a - .018f, a + .018f, .975f, 3, .057f, IVORY);
        }
        return p.finish();
    }

    private static Seal dial() {
        Pen p = new Pen();
        for (int i = 0; i < 12; i++) {
            float a = i / 12f;
            if (i % 3 == 0) {
                p.lozenge(a, .69f, .90f, .021f, .033f, GEM);
            } else {
                p.chord(a, .76f, a, .87f, .039f, IVORY);
            }
            p.arc(a + .018f, a + .067f, .738f, 3, .024f, GOLD);
        }
        return p.finish();
    }

    private static Seal compass() {
        Pen p = new Pen();
        for (int i = 0; i < 4; i++) {
            float a = i / 4f;
            p.lozenge(a, .10f, .665f, .052f, .047f, IVORY);
            p.chord(a, .14f, a, .49f, .031f, GEM);
            p.chord(a + .125f, .28f, a + .125f, .45f, .047f, GOLD);
        }
        return p.finish();
    }

    private static Seal moon() {
        Pen p = new Pen();
        // The two bows meet at sharp corners. A suspended pupil hangs between them.
        for (int side : new int[]{1, -1}) {
            for (int i = 0; i < 16; i++) {
                float t = i / 16f, u = (i + 1) / 16f;
                p.stroke(1.30f * t - .65f, side * .29f * Mth.sin(Mth.PI * t),
                        1.30f * u - .65f, side * .29f * Mth.sin(Mth.PI * u), .052f, IVORY);
            }
        }
        p.arc(0, 1, .175f, 20, .052f, GEM);
        p.stroke(0, -.11f, 0, .11f, .055f, GOLD);
        p.lozenge(.25f, .32f, .62f, .032f, .035f, GEM);
        p.lozenge(.75f, .32f, .62f, .032f, .035f, GEM);
        return p.finish();
    }

    private static Seal sun() {
        Pen p = new Pen();
        p.arc(0, 1, .236f, 24, .064f, IVORY);
        for (int i = 0; i < 8; i++) {
            float a = i / 8f;
            p.chord(a - .045f, .31f, a, .64f, .043f, GOLD);
            p.chord(a, .64f, a + .031f, .36f, .039f, GEM);
        }
        p.arc(0, 1, .11f, 12, .068f, GEM);
        return p.finish();
    }

    private static Seal knot() {
        Pen p = new Pen();
        // A single continuous pentagram encloses a second, counter-facing jewel.
        for (int i = 0; i < 5; i++) {
            float a = .25f + i * .4f;
            p.chord(a, .65f, a + .4f, .65f, .052f, IVORY);
        }
        for (int i = 0; i < 5; i++) {
            float a = .15f + i / 5f;
            p.chord(a, .155f, a + .2f, .155f, .052f, GEM);
        }
        return p.finish();
    }

    private static Seal hourglass() {
        Pen p = new Pen();
        for (int sign : new int[]{1, -1}) {
            float rim = sign * .46f;
            p.stroke(-.39f, rim, .39f, rim, .064f, IVORY);
            p.stroke(-.34f, rim, 0, 0, .048f, GOLD);
            p.stroke(0, 0, .34f, rim, .048f, GOLD);
            p.stroke(-.14f, rim * .70f, .14f, rim * .70f, .060f, GEM);
        }
        return p.finish();
    }

    private static Seal rosette() {
        Pen p = new Pen();
        for (int i = 0; i < 6; i++) {
            float a = i / 6f;
            p.lozenge(a, .14f, .63f, .067f, .050f, i % 2 == 0 ? GEM : IVORY);
        }
        p.arc(0, 1, .12f, 12, .043f, GOLD);
        return p.finish();
    }

    private static Seal scales() {
        Pen p = new Pen();
        p.stroke(0, -.48f, 0, .44f, .055f, IVORY);
        p.stroke(-.22f, -.48f, .22f, -.48f, .050f, GOLD);
        p.stroke(-.43f, .22f, .43f, .30f, .055f, GEM);
        for (int sign : new int[]{1, -1}) {
            float x = sign * .40f, y = .26f + sign * .04f;
            p.stroke(x, y, x - .15f, -.12f, .037f, GOLD);
            p.stroke(x, y, x + .15f, -.12f, .037f, GOLD);
            p.stroke(x - .15f, -.12f, x + .15f, -.12f, .057f, IVORY);
        }
        return p.finish();
    }

    private static Seal eclipses() {
        Pen p = new Pen();
        p.arc(0, 1, .29f, 24, .056f, IVORY);
        for (int i = 0; i < 3; i++) {
            float a = i / 3f;
            p.arc(a, a + .23f, .48f, 8, .057f, GEM);
            p.lozenge(a + .27f, .38f, .63f, .030f, .043f, GOLD);
        }
        return p.finish();
    }

    private static Seal armillary() {
        Pen p = new Pen();
        for (int ring = 0; ring < 3; ring++) {
            float angle = ring * Mth.PI / 3;
            float cos = Mth.cos(angle), sin = Mth.sin(angle);
            float x = .67f * cos, y = .67f * sin;
            for (int i = 1; i <= 48; i++) {
                float t = i * Mth.TWO_PI / 48;
                float u = .67f * Mth.cos(t), v = .265f * Mth.sin(t);
                float nextX = u * cos - v * sin, nextY = u * sin + v * cos;
                p.stroke(x, y, nextX, nextY, .032f, ring == 1 ? IVORY : GOLD);
                x = nextX;
                y = nextY;
            }
        }
        for (int i = 0; i < 4; i++) {
            float a = i / 4f;
            p.chord(a, .23f, a + .125f, .073f, .052f, IVORY);
            p.chord(a + .125f, .073f, a + .25f, .23f, .052f, GEM);
        }
        return p.finish();
    }
}
