package io.myf.fortune.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.myf.fortune.expansion.offense.SpellFigure;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** Linked, world-space projections composed from reusable, progressively revealed line meshes. */
final class ChantArrayRenderer {
    private static final int INK = 0, METAL = 1, LIGHT = 2;
    private static final int MOONS = 12;
    private static final float TURN = Mth.TWO_PI;

    private record Palette(int ink, int metal, int light) {
        int color(int channel) {
            return channel == LIGHT ? light : channel == METAL ? metal : ink;
        }
    }

    private static final Palette MARSH = new Palette(0x5FA853, 0xE0D16F, 0xF2FFE0);
    private static final Palette BELL = new Palette(0x39ABC7, 0xFFD065, 0xDCFFFD);
    private static final Palette BONE = new Palette(0x4CCBA6, 0xB5D8B1, 0xFCF4D6);
    private static final Palette ROSE = new Palette(0xE24D71, 0xFFAD65, 0xFFF3CC);

    private static final Seal FOUNDATION = foundation();
    private static final Seal DIAL = dial();
    private static final Seal GATE = gate();
    private static final Seal LENS = lens();

    private ChantArrayRenderer() {}

    static void render(SpellFigure figure, float progress, PoseStack pose, MultiBufferSource buffers) {
        Palette color = switch (figure.kind()) {
            case SWAMP_RUNE -> MARSH;
            case BELL_RUNE -> BELL;
            case WING_RUNE -> BONE;
            case DANCE -> ROSE;
            default -> null;
        };
        if (color == null || progress <= 0) return;
        float time = Mth.clamp(progress, 0, 1);
        float raised = ease(time, .16f, .85f);
        float charged = ease(time, .73f, 1);
        float rotation = 36 * time + 68 * charged;
        float strength = .84f + .16f * charged;
        VertexConsumer out = buffers.getBuffer(RenderType.debugQuads());

        // The initial inscription opens underfoot, then sweeps across the entire field.
        float ground = ease(time, 0, .32f);
        plane(pose,out,color,FOUNDATION,0,.29f,0,0,0,-rotation * .24f,
                5 + 43 * ground,ground,strength);

        // Four groups of three gates uncurl at successively higher elevations.
        for (int i = 0; i < MOONS; i++) {
            float start = .09f + .022f * i;
            float opened = ease(time, start, start + .22f);
            if (opened == 0) continue;
            float angle = i * TURN / MOONS;
            float orbit = 18 + 21 * opened;
            float height = 1 + (8 + (i % 3) * 3) * opened;
            plane(pose,out,color,GATE,orbit*Mth.sin(angle),height,orbit*Mth.cos(angle),
                    i*30,opened*(i%2==0 ? 79 : 57),-rotation + i*30,
                    1 + 6.3f*opened,opened,strength);
        }

        // Three crowns rise through one another, rotating in opposite directions.
        for (int tier = 0; tier < 3; tier++) {
            float start = .24f + tier * .09f;
            float unfolded = ease(time,start,start + .29f);
            if (unfolded == 0) continue;
            float polar = tier == 1 ? -1 : 1;
            plane(pose,out,color,tier == 1 ? FOUNDATION : DIAL,0,8 + (17 + tier*10)*unfolded,0,
                    tier*42,polar*(4+tier*2),polar*(rotation+tier*23),
                    (39 - tier*6)*unfolded,unfolded,strength);
        }

        // Two meridians form an open three-dimensional astrolabe. Their centres stay
        // empty so a caster can see through the descending charge into the sky.
        float meridian = ease(time,.50f,.86f);
        if (meridian > 0) {
            for (int side = 0; side < 2; side++) {
                plane(pose,out,color,LENS,0,21 + 7*raised,0,45 + side*90 + rotation*.28f,
                        side == 0 ? 84 : 96,side == 0 ? rotation : -rotation,
                        21*meridian,meridian,strength);
            }
        }
        if (charged > 0) {
            plane(pose,out,color,DIAL,0,49 + 5*charged,0,rotation,8,-rotation*1.5f,
                    2 + 8*charged,charged,strength);
        }
    }

    private static float ease(float time, float start, float end) {
        float u = Mth.clamp((time - start) / (end - start), 0, 1);
        return u * u * (3 - 2 * u);
    }

    private static void plane(PoseStack pose, VertexConsumer out, Palette color, Seal seal,
                              float x, float y, float z, float yaw, float tilt, float spin,
                              float radius, float drawn, float strength) {
        pose.pushPose();
        pose.translate(x,y,z);
        if (yaw != 0) pose.mulPose(Axis.YP.rotationDegrees(yaw));
        if (tilt != 0) pose.mulPose(Axis.XP.rotationDegrees(tilt));
        pose.mulPose(Axis.YP.rotationDegrees(spin));
        pose.scale(radius,radius,radius);
        seal.draw(pose.last().pose(),out,color,drawn,strength);
        pose.popPose();
    }

    /** Unit-space endpoints and perpendicular stroke radii are computed once on class loading. */
    private record Stroke(float x, float z, float endX, float endZ, float nx, float nz, int ink) {
        void draw(Matrix4f matrix, VertexConsumer out, int color, int alpha, float fraction) {
            float u = Mth.lerp(fraction,x,endX);
            float v = Mth.lerp(fraction,z,endZ);
            strip(matrix,out,u,v,-1,1,color,alpha,alpha);
            if (ink == LIGHT) {
                // Fade out from the bright filament on both sides. No offscreen render target.
                strip(matrix,out,u,v,-2.8f,-1,color,0,alpha/2);
                strip(matrix,out,u,v,1,2.8f,color,alpha/2,0);
            }
        }

        private void strip(Matrix4f m, VertexConsumer out, float u, float v, float a, float b,
                           int color, int alphaA, int alphaB) {
            vertex(m,out,x+nx*a,z+nz*a,color,alphaA);
            vertex(m,out,u+nx*a,v+nz*a,color,alphaA);
            vertex(m,out,u+nx*b,v+nz*b,color,alphaB);
            vertex(m,out,x+nx*b,z+nz*b,color,alphaB);
        }

        private static void vertex(Matrix4f m, VertexConsumer out, float x, float z, int color, int alpha) {
            out.vertex(m,x,0,z).color((color>>16)&255,(color>>8)&255,color&255,alpha).endVertex();
        }
    }

    private record Seal(Stroke[] strokes) {
        void draw(Matrix4f matrix, VertexConsumer out, Palette palette, float drawn, float strength) {
            float pen = drawn * strokes.length;
            for (int i = 0; i < strokes.length && i < pen; i++) {
                Stroke stroke = strokes[i];
                int alpha = (int)(strength * (stroke.ink == INK ? 204 : 248));
                stroke.draw(matrix,out,palette.color(stroke.ink),alpha,Math.min(1,pen-i));
            }
        }
    }

    /** Large, discontinuous engraved rim with twelve inward-facing arrowheads. */
    private static Seal foundation() {
        List<Stroke> pen = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            float a = i*TURN/12, b = (i+1)*TURN/12, mid=(a+b)/2;
            arc(pen,1,a,b,5,.007f,INK);
            arc(pen,.975f,a+.025f,b-.025f,5,.0033f,LIGHT);
            arc(pen,.87f,a+.065f,b-.065f,4,.007f,METAL);
            line(pen,.84f,mid-.048f,.74f,mid,.004f,LIGHT);
            line(pen,.74f,mid,.84f,mid+.048f,.004f,LIGHT);
            line(pen,.85f,a,.97f,a,.003f,METAL);
        }
        return new Seal(pen.toArray(Stroke[]::new));
    }

    /** A pentagonal star aperture within a broken gold astrolabe ring. */
    private static Seal dial() {
        List<Stroke> pen = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            float a = i*TURN/5, b = a+TURN/5;
            arc(pen,1,a,b,10,.007f,INK);
            arc(pen,.93f,a+.08f,b-.08f,8,.006f,LIGHT);
            line(pen,.81f,a,.81f,a+2*TURN/5,.004f,METAL);
            line(pen,.75f,a,.98f,a,.005f,LIGHT);
        }
        arc(pen,.31f,0,TURN,30,.006f,LIGHT);
        return new Seal(pen.toArray(Stroke[]::new));
    }

    /** An open annular door, with a pointed split arch on each side. */
    private static Seal gate() {
        List<Stroke> pen = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            float a = i*TURN/4, b=a+TURN/4;
            arc(pen,1,a,b,8,.021f,INK);
            arc(pen,.94f,a+.09f,b-.09f,7,.012f,LIGHT);
            line(pen,.88f,a,.38f,a+.4f,.013f,METAL);
            line(pen,.38f,a+.4f,.83f,b,.009f,LIGHT);
        }
        return new Seal(pen.toArray(Stroke[]::new));
    }

    private static Seal lens() {
        List<Stroke> pen = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            float a=i*TURN/4, b=a+TURN/4;
            arc(pen,1,a+.06f,b-.06f,12,.006f,LIGHT);
            arc(pen,.96f,a+.16f,b-.16f,11,.009f,INK);
            line(pen,.88f,a,1.06f,a,.008f,METAL);
        }
        return new Seal(pen.toArray(Stroke[]::new));
    }

    private static void arc(List<Stroke> pen, float radius, float a, float b, int segments,
                            float width, int ink) {
        for (int i = 0; i < segments; i++) {
            line(pen,radius,Mth.lerp((float)i/segments,a,b),radius,
                    Mth.lerp((float)(i+1)/segments,a,b),width,ink);
        }
    }

    private static void line(List<Stroke> pen, float startR, float startAngle, float endR, float endAngle,
                             float width, int ink) {
        float x = startR*Mth.sin(startAngle), z = startR*Mth.cos(startAngle);
        float u = endR*Mth.sin(endAngle), v = endR*Mth.cos(endAngle);
        float scale = width / Mth.sqrt((u-x)*(u-x) + (v-z)*(v-z));
        pen.add(new Stroke(x,z,u,v,(v-z)*scale,(x-u)*scale,ink));
    }
}
