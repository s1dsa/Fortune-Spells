package io.myf.fortune.client.audience;

import static io.myf.fortune.client.audience.AudienceGlyphs.*;

/** Unit-sized brass engraving plates. Layer transforms are sampled live; their strokes are baked once. */
final class CataclysmSeals {
    private static final int ACCENT = -1;
    static final Seal CALENDAR = calendar();
    static final Seal ASTROLABE = astrolabe();
    static final Seal[] HOUSES = {house(3), house(4), house(5), house(6), house(7), house(8)};
    private CataclysmSeals() {}

    private static Seal calendar() {
        Pen pen = new Pen();
        pen.arc(0, 1, 1, 144, .0038f, GOLD);
        pen.arc(0, 1, .984f, 144, .0020f, IVORY);
        for (int i = 0; i < 48; i++) {
            float a = i / 48f;
            pen.arc(a + .0009f, a + .019f, .939f, 3, .0041f, ACCENT);
            pen.chord(a, .945f, a, .980f, .0028f, GOLD);
            // Discrete broken sigils between the index marks, grouped in fours like celestial calendar pages.
            pen.chord(a + .004f, .951f, a + .006f, .969f, .0023f, IVORY);
            pen.chord(a + .006f, .969f, a + .014f, .960f, .0023f, IVORY);
            pen.chord(a + .014f, .960f, a + .012f, i % 4 == 0 ? .978f : .950f, .0023f, IVORY);
            if (i % 4 == 0) {
                pen.lozenge(a, .84f, .94f, .0075f, .0033f, GOLD);
                pen.chord(a, .84f, a, .795f, .004f, ACCENT);
            }
        }
        return pen.finish();
    }

    private static Seal astrolabe() {
        Pen pen = new Pen();
        for (int i = 0; i < 12; i++) {
            float a = i / 12f;
            pen.chord(a, .92f, a + 5 / 12f, .92f, .0035f, ACCENT);
            pen.chord(a, .91f, a + 3 / 12f, .91f, .0023f, GOLD);
            pen.chord(a, .78f, a + 1 / 12f, .78f, .003f, IVORY);
            for (int j = 0; j < 12; j++) {
                float t = j / 12f, n = (j + 1) / 12f;
                pen.chord(a + t / 12f, .62f - .17f * (float)Math.sin(t * Math.PI),
                        a + n / 12f, .62f - .17f * (float)Math.sin(n * Math.PI), .0028f, GOLD);
            }
            pen.lozenge(a, .226f, .38f, .012f, .004f, IVORY);
        }
        pen.arc(0, 1, .40f, 72, .004f, ACCENT);
        pen.arc(0, 1, .215f, 56, .003f, GOLD);
        return pen.finish();
    }

    private static Seal house(int sides) {
        Pen p = new Pen();
        p.arc(0, 1, .995f, 48, .012f, ACCENT);
        p.arc(0, 1, .92f, 48, .007f, GOLD);
        for (int i = 0; i < sides; i++) {
            float a = (float)i / sides;
            p.chord(a, .865f, a + 1f / sides, .865f, .014f, GOLD);
            p.chord(a, .865f, a + (sides > 4 ? 2f : 1f) / sides + .5f, .56f, .0105f, ACCENT);
            p.lozenge(a, .56f, .88f, .025f, .011f, IVORY);
            p.chord(a, .35f, a + .5f / sides, .15f, .009f, GOLD);
            p.chord(a + .5f / sides, .15f, a + 1f / sides, .35f, .009f, GOLD);
        }
        return p.finish();
    }
}
