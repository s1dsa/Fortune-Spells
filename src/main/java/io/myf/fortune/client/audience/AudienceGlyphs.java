package io.myf.fortune.client.audience;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** Flat, unit-radius inscriptions. Each is constructed once and instanced at its world-space anchor. */
final class AudienceGlyphs {
    static final int GOLD = 0xFFD276;
    static final int IVORY = 0xFFF1C5;
    static final int RUBY = 0xCF2849;
    static final int[] JEWELS = {
            0xFF5368, 0xFFAA45, 0xF5DA5E, 0x69DF9D,
            0x5FE2E4, 0x84A9FF, 0xC695FF, 0xFF83C4
    };
    private static final int ACCENT = -1;
    static final Seal COURT = court();
    static final Seal GATE = gate();
    static final Seal CROWN = crown();
    static final Seal STATION = station();
    static final Seal SIGNET = signet();
    static final Seal WAVE = wave();
    static final Seal ASTROLABE = astrolabe();
    static final Seal ROSETTE = rosette();

    // A brass instrument dial with two interleaved dodecagons and individually indexed hour marks.
    private static Seal astrolabe() {
        Pen p = new Pen();
        for (int i = 0; i < 48; i++) {
            float a = i / 48f;
            boolean hour = i % 4 == 0;
            p.chord(a, hour ? .84f : .95f, a, 1, hour ? .010f : .005f, hour ? IVORY : GOLD);
            if (hour) {
                p.chord(a, .81f, a + 5 / 12f, .81f, .006f, ACCENT);
                p.chord(a + .035f, .72f, a + .285f, .72f, .005f, GOLD);
                p.lozenge(a, .84f, .94f, .008f, .006f, ACCENT);
            }
        }
        p.arc(0, 1, .98f, 72, .008f, GOLD);
        return p.finish();
    }

    private static Seal rosette() {
        Pen p = new Pen();
        for (int i = 0; i < 8; i++) {
            float axis = i / 8f;
            for (int j = 0; j < 12; j++) {
                float a = j / 12f, b = (j + 1) / 12f;
                p.chord(axis + (a - .5f) / 8, .42f + .56f * Mth.sin(a * Mth.PI),
                        axis + (b - .5f) / 8, .42f + .56f * Mth.sin(b * Mth.PI), .013f, ACCENT);
            }
            p.lozenge(axis, .34f, .91f, .025f, .009f, GOLD);
        }
        p.arc(0, 1, .39f, 48, .014f, IVORY);
        return p.finish();
    }

    private AudienceGlyphs() {}

    private record Stroke(float x, float z, float u, float v, float nx, float nz, int color) {}

    static final class Seal {
        private final Stroke[] marks;

        private Seal(List<Stroke> marks) { this.marks = marks.toArray(Stroke[]::new); }

        void draw(Matrix4f pose, VertexConsumer vertices, int accent, int alpha) {
            draw(pose, vertices, accent, alpha, 1);
        }

        void draw(Matrix4f pose, VertexConsumer vertices, int accent, int alpha, float progress) {
            float count = Mth.clamp(progress, 0, 1) * marks.length;
            for (int i = 0; i < count; i++) {
                Stroke s = marks[i];
                float portion = Math.min(1, count - i);
                float u = Mth.lerp(portion, s.x, s.u);
                float v = Mth.lerp(portion, s.z, s.v);
                int color = s.color == ACCENT ? accent : s.color;
                vertex(pose, vertices, s.x + s.nx, 0, s.z + s.nz, color, alpha);
                vertex(pose, vertices, u + s.nx, 0, v + s.nz, color, alpha);
                vertex(pose, vertices, u - s.nx, 0, v - s.nz, color, alpha);
                vertex(pose, vertices, s.x - s.nx, 0, s.z - s.nz, color, alpha);
            }
        }
    }

    static final class Pen {
        private final List<Stroke> marks = new ArrayList<>();

        void stroke(float x, float z, float u, float v, float width, int color) {
            float dx = u - x, dz = v - z;
            float scale = width * .5f / Mth.sqrt(dx * dx + dz * dz);
            marks.add(new Stroke(x, z, u, v, dz * scale, -dx * scale, color));
        }

        // Angles are measured in full turns, starting along the ritual's eastern axis.
        void chord(float a, float r, float b, float s, float width, int color) {
            stroke(r * Mth.cos(a * Mth.TWO_PI), r * Mth.sin(a * Mth.TWO_PI),
                    s * Mth.cos(b * Mth.TWO_PI), s * Mth.sin(b * Mth.TWO_PI), width, color);
        }

        void arc(float start, float end, float radius, int divisions, float width, int color) {
            for (int i = 0; i < divisions; i++) {
                float a = Mth.lerp((float)i / divisions, start, end);
                float b = Mth.lerp((float)(i + 1) / divisions, start, end);
                chord(a, radius, b, radius, width, color);
            }
        }

        void lozenge(float angle, float inner, float outer, float span, float width, int color) {
            float middle = (inner + outer) * .5f;
            chord(angle, inner, angle - span, middle, width, color);
            chord(angle - span, middle, angle, outer, width, color);
            chord(angle, outer, angle + span, middle, width, color);
            chord(angle + span, middle, angle, inner, width, color);
        }

        Seal finish() { return new Seal(marks); }
    }

    // Segmented, bejewelled border of the royal court. The wide empty middle leaves room for offerings.
    private static Seal court() {
        Pen p = new Pen();
        for (int i = 0; i < 16; i++) {
            float a = i / 16f, b = (i + 1) / 16f, mid = (a + b) / 2;
            p.arc(a + .002f, b - .002f, .995f, 5, .010f, ACCENT);
            p.arc(a + .002f, b - .002f, .980f, 5, .004f, IVORY);
            p.arc(a + .008f, b - .008f, .916f, 4, .006f, GOLD);
            p.lozenge(mid, .886f, .968f, .006f, .0045f, JEWELS[i & 7]);
            p.chord(a, .956f, a, .91f, .006f, GOLD);
            if ((i & 1) == 0) {
                p.chord(a - .011f, .90f, a, .80f, .004f, GOLD);
                p.chord(a, .80f, a + .011f, .90f, .004f, GOLD);
            }
        }
        return p.finish();
    }

    // A pointed arch framing the outer stations; the clear doorway is visible from inside the court.
    private static Seal gate() {
        Pen p = new Pen();
        p.arc(0, 1, .97f, 40, .036f, ACCENT);
        p.arc(0, 1, 1, 40, .012f, GOLD);
        for (int i = 0; i < 8; i++) {
            float a = i / 8f;
            p.arc(a + .012f, a + .113f, .83f, 4, .014f, GOLD);
            p.lozenge(a, .77f, .93f, .016f, .014f, IVORY);
        }
        for (int side = -1; side <= 1; side += 2) {
            p.stroke(.38f * side, .51f, .38f * side, -.19f, .023f, ACCENT);
            p.stroke(.38f * side, -.19f, .22f * side, -.43f, .026f, GOLD);
            p.stroke(.22f * side, -.43f, 0, -.66f, .026f, IVORY);
            p.stroke(.26f * side, .49f, .26f * side, -.13f, .010f, IVORY);
            p.stroke(.26f * side, -.13f, 0, -.46f, .012f, GOLD);
        }
        return p.finish();
    }

    // Open crown rim, with a repeated three-point crest instead of a second ground pentagram.
    private static Seal crown() {
        Pen p = new Pen();
        p.arc(0, 1, 1, 64, .012f, GOLD);
        p.arc(0, 1, .89f, 64, .022f, ACCENT);
        for (int i = 0; i < 12; i++) {
            float a = i / 12f;
            p.arc(a + .011f, a + .072f, .79f, 4, .010f, IVORY);
            p.chord(a - .027f, .83f, a - .013f, 1.06f, .010f, GOLD);
            p.chord(a - .013f, 1.06f, a, .94f, .010f, ACCENT);
            p.chord(a, .94f, a + .013f, 1.06f, .010f, ACCENT);
            p.chord(a + .013f, 1.06f, a + .027f, .83f, .010f, GOLD);
        }
        return p.finish();
    }

    private static Seal station() {
        Pen p = new Pen();
        p.arc(0, 1, .98f, 40, .035f, ACCENT);
        p.arc(0, 1, .925f, 40, .016f, GOLD);
        for (int i = 0; i < 5; i++) {
            float a = .05f + i / 5f;
            p.chord(a, .79f, a + .4f, .79f, .048f, ACCENT);
            p.chord(a, .79f, a + .4f, .79f, .014f, IVORY);
            p.lozenge(a, .80f, 1.06f, .015f, .020f, GOLD);
            p.chord(a, .86f, a, 1.00f, .031f, JEWELS[(i * 3) & 7]);
        }
        return p.finish();
    }

    private static Seal signet() {
        Pen p = new Pen();
        for (int i = 0; i < 10; i++) {
            float a = i / 10f;
            p.arc(a + .007f, a + .093f, .94f, 5, .025f, JEWELS[i & 7]);
            p.lozenge(a, .77f, 1.03f, .021f, .014f, GOLD);
            p.chord(a - .03f, .87f, a, .72f, .012f, ACCENT);
            p.chord(a, .72f, a + .03f, .87f, .012f, ACCENT);
        }
        return p.finish();
    }

    private static Seal wave() {
        Pen p = new Pen();
        p.arc(0, 1, 1, 80, .015f, ACCENT);
        p.arc(0, 1, .97f, 80, .006f, IVORY);
        return p.finish();
    }

    static void vertex(Matrix4f pose, VertexConsumer out, float x, float y, float z, int color, int alpha) {
        out.vertex(pose, x, y, z).color(color >> 16 & 255, color >> 8 & 255, color & 255, alpha).endVertex();
    }
}
