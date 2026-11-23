package io.myf.fortune.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** Crossing threads concentrate into a suspended light, then the release opens a tapered shaft. */
final class ReleaseBeacon {
    private static final int THREADS = 6;
    private static final int SEGMENTS = 16;
    private static final int SIDES = 12;
    private static final float TAU = (float) (Math.PI * 2);

    private ReleaseBeacon() {}

    static void render(Matrix4f pose, VertexConsumer out, float age, float radius, float spread,
                       float opacity, int accent, int ivory, boolean released) {
        float height = 12 + 22 * spread;
        int alpha = (int) (175 * opacity * spread);
        // Opposite chiralities bridge the ground dial and crown without drawing across the player's face.
        for (int arm = 0; arm < THREADS; arm++) {
            float phase = TAU * arm / THREADS + age * .014f;
            float twist = (arm % 2 == 0 ? 1 : -1) * 3.8f;
            float x = Mth.cos(phase) * radius * .66f;
            float z = Mth.sin(phase) * radius * .66f;
            for (int i = 1; i <= SEGMENTS; i++) {
                float u = i / (float) SEGMENTS;
                float r = radius * (.66f - .49f * Mth.sin(u * Mth.PI));
                float nextX = Mth.cos(phase + u * twist) * r;
                float nextZ = Mth.sin(phase + u * twist) * r;
                float y = u * height;
                float width = .09f + .045f * Mth.sin(age * .16f - u * TAU);
                int color = arm % 2 == 0 ? accent : ivory;
                vertex(out,pose,x,y-height/SEGMENTS-width,z,color,alpha);
                vertex(out,pose,nextX,y-width,nextZ,color,alpha);
                vertex(out,pose,nextX,y+width,nextZ,color,alpha);
                vertex(out,pose,x,y-height/SEGMENTS+width,z,color,alpha);
                x = nextX;
                z = nextZ;
            }
        }
        float flash = released ? Mth.clamp((age - 8) / 8, 0, 1) * Mth.clamp((47 - age) / 20, 0, 1) : 0;
        float orb = (.6f + 2.3f * flash + .24f * Mth.sin(age * .18f)) * spread;
        mantle(out,pose,height + 3,0,height,orb,ivory,alpha);
        mantle(out,pose,height,orb,height - 3,0,ivory,alpha);
        if (flash > 0) {
            // Stop above eye level so a caster can follow the bell or the bite inside the shaft.
            float bottom = Mth.lerp(flash, height, 3.4f);
            mantle(out,pose,height,orb * .85f,bottom,.16f,ivory,(int)(215 * opacity * flash));
            mantle(out,pose,height,orb * 1.65f,bottom,.55f,accent,(int)(64 * opacity * flash));
        }
    }

    private static void mantle(VertexConsumer out, Matrix4f pose, float top, float topRadius,
                               float bottom, float bottomRadius, int color, int alpha) {
        float x = 1, z = 0;
        for (int side = 1; side <= SIDES; side++) {
            float angle = side * TAU / SIDES;
            float nextX = Mth.cos(angle), nextZ = Mth.sin(angle);
            vertex(out,pose,x*topRadius,top,z*topRadius,color,alpha);
            vertex(out,pose,nextX*topRadius,top,nextZ*topRadius,color,alpha);
            vertex(out,pose,nextX*bottomRadius,bottom,nextZ*bottomRadius,color,alpha);
            vertex(out,pose,x*bottomRadius,bottom,z*bottomRadius,color,alpha);
            x = nextX;
            z = nextZ;
        }
    }

    private static void vertex(VertexConsumer out, Matrix4f pose, float x, float y, float z, int color, int alpha) {
        out.vertex(pose,x,y,z).color(color >> 16 & 255,color >> 8 & 255,color & 255,alpha).endVertex();
    }
}
