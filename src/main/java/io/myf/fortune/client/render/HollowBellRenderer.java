package io.myf.fortune.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** A fixed belfry with a cast shell and a freely swinging clapper, in world units. */
final class HollowBellRenderer {
    private static final int BRONZE = 0xB98742, GILT = 0xFFD581, PATINA = 0x50857C;
    private static final int INNER = 0x624B39, FRAME = 0x355D60, ENAMEL = 0xB3F8E0;
    private static final int SIDES = 24;
    private static final float[] X = new float[SIDES + 1];
    private static final float[] Z = new float[SIDES + 1];
    private static final float[] LIGHT = new float[SIDES];

    // The contour travels down the outside, across the solid lip and back up the inside.
    private static final float[] SHELL_Y = {-.9f,-1.2f,-1.7f,-1.9f,-2.2f,-3.8f,-4.9f,
            -5.3f,-5.5f,-5.8f,-6.12f,-6.35f,-6.28f,-5.88f,-5.2f,-3.8f,-2.28f,-1.55f};
    private static final float[] SHELL_R = {.3f,1.18f,1.6f,1.72f,1.61f,1.82f,2.19f,
            2.46f,2.63f,2.74f,3.16f,3.18f,2.84f,2.59f,2.12f,1.54f,1.32f,.22f};
    private static final int[] STRIP = {GILT,BRONZE,GILT,PATINA,BRONZE,BRONZE,
            BRONZE,GILT,PATINA,BRONZE,GILT,GILT,BRONZE,INNER,INNER,INNER,INNER};

    static {
        for (int i = 0; i < SIDES; i++) {
            float angle = Mth.TWO_PI * i / SIDES;
            X[i] = Mth.cos(angle);
            Z[i] = Mth.sin(angle);
            LIGHT[i] = .79f + .21f * Mth.cos(angle + .75f);
        }
        X[SIDES] = X[0];
        Z[SIDES] = Z[0];
    }

    private HollowBellRenderer() {}

    static void render(PoseStack pose, VertexConsumer vertices, float age) {
        float rise = Mth.clamp(age / 12, 0, 1);
        pose.pushPose();
        pose.translate(0, -3 * (1-rise) * (1-rise), 0);
        Matrix4f stand = pose.last().pose();
        // The two open trestles leave room for the bell to pivot between them.
        for (int side = -1; side <= 1; side += 2) {
            float x = side * 4.35f;
            for (int end = -1; end <= 1; end += 2) {
                float z = end * 2.45f;
                box(stand, vertices, x-.55f,0,z-.58f, x+.55f,.6f,z+.58f, GILT);
                box(stand, vertices, x-.25f,.6f,z-.25f, x+.25f,12.25f,z+.25f, FRAME);
                box(stand, vertices, x-.32f,10.95f,z-.32f, x+.32f,11.3f,z+.32f, GILT);
            }
            box(stand, vertices, x-.44f,11.85f,-2.9f, x+.44f,12.5f,2.9f, PATINA);
        }
        box(stand, vertices, -4.8f,12.05f,-.32f, 4.8f,12.52f,.32f, GILT);
        pose.translate(0,12.2,0);

        // The eight 15-tick strikes alternate sides; wind up again during the two-beat finale.
        float beat = age <= 120 ? age / 15 : 8 + (age-120) / 30;
        float amplitude = 23 * rise + 5 * Mth.clamp((age-120)/30, 0, 1);
        pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(beat * Mth.PI) * amplitude));
        Matrix4f bell = pose.last().pose();
        box(bell, vertices, -.52f,-1.32f,-.32f, .52f,.2f,.32f, PATINA);
        for (int i = 0; i < STRIP.length; i++)
            band(bell, vertices, SHELL_Y[i], SHELL_R[i], SHELL_Y[i+1], SHELL_R[i+1], STRIP[i]);
        // Eight raised lozenges give the shoulder a readable, texture-free ornament.
        for (int i = 0; i < SIDES; i += 3) {
            float x = X[i], z = Z[i];
            point(bell, vertices, 1.68f*x,-2.52f,1.68f*z, ENAMEL);
            point(bell, vertices, 1.78f*x-.24f*z,-3.22f,1.78f*z+.24f*x, GILT);
            point(bell, vertices, 1.91f*x,-4.12f,1.91f*z, BRONZE);
            point(bell, vertices, 1.78f*x+.24f*z,-3.22f,1.78f*z-.24f*x, GILT);
        }

        pose.translate(0,-1.55f,0);
        pose.mulPose(Axis.XP.rotationDegrees(Mth.cos(beat*Mth.PI)*22*rise));
        Matrix4f clapper = pose.last().pose();
        band(clapper, vertices, 0,.14f,-4.62f,.16f, FRAME);
        band(clapper, vertices, -4.5f,.16f,-4.95f,.51f, GILT);
        band(clapper, vertices, -4.95f,.51f,-5.37f,.35f, BRONZE);
        band(clapper, vertices, -5.37f,.35f,-5.55f,0, GILT);
        pose.popPose();
    }

    private static void band(Matrix4f matrix, VertexConsumer v, float y1, float r1,
                             float y2, float r2, int color) {
        for (int i = 0; i < SIDES; i++) {
            int c = shade(color, LIGHT[i]);
            point(matrix,v, r1*X[i],y1,r1*Z[i], c);
            point(matrix,v, r1*X[i+1],y1,r1*Z[i+1], c);
            point(matrix,v, r2*X[i+1],y2,r2*Z[i+1], c);
            point(matrix,v, r2*X[i],y2,r2*Z[i], c);
        }
    }

    private static final int[][] FACES = {{0,2,3,1},{4,5,7,6},{0,1,5,4},
            {2,6,7,3},{0,4,6,2},{1,3,7,5}};
    private static final float[] FACE_LIGHT = {.79f,.91f,.60f,1,.71f,.94f};

    private static void box(Matrix4f matrix, VertexConsumer v, float x1,float y1,float z1,
                            float x2,float y2,float z2,int color) {
        for (int face = 0; face < FACES.length; face++) {
            int c = shade(color, FACE_LIGHT[face]);
            for (int corner : FACES[face])
                point(matrix,v,(corner&1)==0?x1:x2,(corner&2)==0?y1:y2,(corner&4)==0?z1:z2,c);
        }
    }

    private static int shade(int color, float light) {
        return ((int)((color >> 16)*light) << 16) | ((int)(((color >> 8)&255)*light) << 8)
                | (int)((color&255)*light);
    }

    private static void point(Matrix4f matrix, VertexConsumer v, float x,float y,float z,int color) {
        v.vertex(matrix,x,y,z).color(color>>16,(color>>8)&255,color&255,255).endVertex();
    }
}
