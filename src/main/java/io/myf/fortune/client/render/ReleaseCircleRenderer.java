package io.myf.fortune.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.myf.fortune.expansion.offense.SpellFigure;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** Rotating world-space seals. All four spells share the same untextured strokes. */
final class ReleaseCircleRenderer {
    private record Palette(int band, int rim, int gem) {}
    private static final Palette CROCODILE = new Palette(0x30986D, 0xFFF3BF, 0xBBFF68);
    private static final Palette BELL = new Palette(0x257DB1, 0xFFF0B0, 0x6FFFF5);
    private static final Palette ROSE = new Palette(0xB42357, 0xFFF4BC, 0xFF773A);
    private static final Palette WINGS = new Palette(0x318F82, 0xF3FFDB, 0x92FFD0);
    private static final int SIDES = 64;
    private static final float[] X = new float[SIDES + 1], Z = new float[SIDES + 1];
    static {
        for (int i=0; i<=SIDES; i++) {
            double angle = Math.PI * 2 * i / SIDES;
            X[i] = (float)Math.sin(angle);
            Z[i] = (float)Math.cos(angle);
        }
    }

    private ReleaseCircleRenderer() {}

    static void render(SpellFigure figure, float partial, PoseStack pose, MultiBufferSource buffers) {
        float t = figure.age() + partial;
        boolean dance = figure.kind() == SpellFigure.Kind.DANCE;
        Palette colors = switch (figure.kind()) {
            case DANCE, FIRE_FIELD -> ROSE;
            case JAWS -> CROCODILE;
            case TOWER -> BELL;
            case WING_HALO -> WINGS;
            default -> throw new IllegalArgumentException("Missing release palette");
        };
        float duration = switch (figure.kind()) {
            case DANCE -> SpellFigure.DANCE_TICKS;
            case TOWER -> 150;
            case JAWS -> SpellFigure.JAWS_DURATION;
            case WING_HALO -> 52;
            default -> 48;
        };
        float opacity = Mth.clamp((duration - t) / 12, 0, 1);
        if (opacity <= 0) return;
        // Keep the first-person view open. The ground, overhead crown and orbiting gates
        // have clear gaps; the release is visible from inside as well as across the field.
        float bloom = Mth.clamp(t / (dance ? 18 : 9), 0, 1);
        float spread = 1 - (1-bloom)*(1-bloom);
        float pulse = dance ? Mth.clamp(1 - (figure.ticksAfterStrike()+partial)/8, 0, 1)
                : .5f + .5f*Mth.cos(t*.33f);
        VertexConsumer out = buffers.getBuffer(RenderType.debugQuads());
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-figure.getYRot()));
        float size = dance ? 21 : 34;
        float radius = size*(.36f+.64f*spread);
        int alpha = (int)(opacity*(190+45*pulse));
        dial(pose,out,0,.24f,0,radius,t*.36f,0,colors,alpha,true);
        dial(pose,out,0,9+16*spread,0,radius*.86f,-t*.66f,5,colors,alpha,true);
        dial(pose,out,0,12+22*spread,0,radius*.61f,t*.94f,-8,colors,alpha,false);
        // A vertical backplate gives the seals a silhouette at the caster's eye level.
        pose.pushPose();
        pose.translate(0,2+9*spread,-radius*.53f);
        pose.mulPose(Axis.XP.rotationDegrees(88));
        dial(pose,out,0,0,0,radius*.37f,-t*.48f,0,colors,alpha,true);
        pose.popPose();
        ReleaseBeacon.render(pose.last().pose(), out, t, radius, spread, opacity,
                colors.gem(), colors.rim(), !dance);
        // Stagger the lower and upper banks. The faces point into the circle,
        // so the surrounding gates read as volumes even while the camera moves.
        for (int i=0; i<8; i++) {
            float angle = i*45 + t*(dance ? .13f : .28f);
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(angle));
            float high = (i & 1)==0 ? 1.45f : 9.8f;
            pose.translate(0,high*spread,radius*.76f);
            pose.mulPose(Axis.XP.rotationDegrees((i&1)==0 ? 16 : 70));
            dial(pose,out,0,0,0, (3.2f+2*spread)*(dance ? .68f : 1),t*((i&1)==0 ? 1.3f : -1.3f),
                    0,colors,alpha,false);
            pose.popPose();
        }
        if (dance && figure.strikeNumber()>0)
            strikeArcs(pose,out,figure,partial,t,radius,colors,alpha);
        if (!dance) {
            float wave = Mth.clamp(t/21,0,1);
            int waveAlpha = (int)(210*opacity*(1-wave));
            if (waveAlpha>0) {
                pose.pushPose();
                pose.translate(0,.44,0);
                band(out,pose.last().pose(),4+36*wave,.19f+(1-wave)*.49f,1,colors.rim,waveAlpha);
                pose.popPose();
            }
        }
        pose.popPose();
    }

    private static void dial(PoseStack p, VertexConsumer out, float x, float y, float z,
                             float radius, float spin, float tilt, Palette c, int a, boolean crown) {
        p.pushPose();
        p.translate(x,y,z);
        if (tilt!=0) p.mulPose(Axis.XP.rotationDegrees(tilt));
        p.mulPose(Axis.YP.rotationDegrees(spin));
        p.scale(radius, radius, radius);
        Matrix4f m=p.last().pose();
        int step = crown ? 1 : 2;
        band(out,m,1,.034f,step,c.band,a);
        band(out,m,.996f,.008f,step,c.rim,a);
        band(out,m,.868f,.020f,step,c.gem,a);
        band(out,m,.59f,.011f,step,c.rim,a);
        for (int n=0; n<8; n++) {
            int i=n*8, end=(i+24)%64, tip=(i+4)%64, next=(i+8)%64;
            // Alternating simple runes leave the center open from every viewing distance.
            if ((n&1)==0) {
                stroke(out,m,.88f*X[tip],.88f*Z[tip],.96f*X[tip],.96f*Z[tip],.010f,c.rim,a);
                stroke(out,m,.94f*X[tip],.94f*Z[tip],.91f*X[tip+1],.91f*Z[tip+1],.008f,c.gem,a);
            } else {
                stroke(out,m,.90f*X[tip-1],.90f*Z[tip-1],.955f*X[tip],.955f*Z[tip],.009f,c.rim,a);
                stroke(out,m,.955f*X[tip],.955f*Z[tip],.90f*X[tip+1],.90f*Z[tip+1],.009f,c.rim,a);
            }
            if (crown) stroke(out,m,.61f*X[i],.61f*Z[i],.70f*X[i],.70f*Z[i],.011f,c.band,a);
        }
        // Four long leaves meet at the central aperture.
        for (int leaf=0;leaf<4;leaf++) {
            float sx=X[leaf*16], sz=Z[leaf*16];
            for(int side=-1;side<=1;side+=2) {
                float px=.075f*sx,pz=.075f*sz;
                for(int j=1;j<=10;j++) {
                    float u=j*.1f, length=.075f+.75f*u;
                    float bow=side*.15f*Mth.sin(Mth.PI*u);
                    float nx=length*sx+bow*sz,nz=length*sz-bow*sx;
                    stroke(out,m,px,pz,nx,nz,.006f,c.rim,a);
                    px=nx; pz=nz;
                }
            }
        }
        p.popPose();
    }

    /** A sweeping blade and separate, drifting petals emerge from alternating pairs of gates. */
    private static void strikeArcs(PoseStack p, VertexConsumer out, SpellFigure f, float partial, float t,
                                   float radius, Palette c, int alpha) {
        float progress = (f.ticksAfterStrike()+partial)/12;
        if (progress>=1) return;
        int strike=f.strikeNumber();
        for (int k=0;k<2;k++) {
            int gate=(strike*3+k*4)%8;
            p.pushPose();
            p.mulPose(Axis.YP.rotationDegrees(gate*45+t*.13f));
            p.translate(0,((gate&1)==0?1.45f:9.8f)*(1-progress)+progress*1.8f,
                    radius*.76f*(1-progress)+3*progress);
            p.mulPose(Axis.ZP.rotationDegrees((k==0?1:-1)*(24+60*progress)));
            float width = 1.2f + 2.7f*progress;
            Matrix4f m=p.last().pose();
            int a=(int)(alpha*(1-progress));
            for (int j=-5;j<5;j++) {
                float u=j*.2f,v=(j+1)*.2f;
                float left=1-u*u,right=1-v*v;
                vertex(out,m,u*width,left*.55f,left*.9f,c.rim,a);
                vertex(out,m,v*width,right*.55f,right*.9f,c.rim,a);
                vertex(out,m,v*width,right*.16f,right*.24f,c.gem,a);
                vertex(out,m,u*width,left*.16f,left*.24f,c.gem,a);
            }
            for (int n=0;n<4;n++) {
                float x=(n-1.5f)*(.40f+progress*.93f);
                float y=.45f*Mth.sin(t*.23f+n)+progress*.7f;
                float z=.4f+n*.48f;
                vertex(out,m,x-.13f,y,z,c.band,a);
                vertex(out,m,x+.07f,y+.30f,z+.20f,c.gem,a);
                vertex(out,m,x+.27f,y+.34f,z+.38f,c.rim,a);
                vertex(out,m,x+.10f,y+.02f,z+.24f,c.band,a);
            }
            p.popPose();
        }
    }

    private static void band(VertexConsumer out, Matrix4f m,float r,float width,int step,int color,int a) {
        float inner=r-width;
        for (int i=0;i<SIDES;i+=step) {
            int j=i+step;
            vertex(out,m,r*X[i],0,r*Z[i],color,a);
            vertex(out,m,r*X[j],0,r*Z[j],color,a);
            vertex(out,m,inner*X[j],0,inner*Z[j],color,a);
            vertex(out,m,inner*X[i],0,inner*Z[i],color,a);
        }
    }

    private static void stroke(VertexConsumer out, Matrix4f m,float x,float z,float u,float v,
                               float width,int color,int a) {
        float dx=u-x,dz=v-z,scale=width/Mth.sqrt(dx*dx+dz*dz);
        float nx=dz*scale,nz=-dx*scale;
        vertex(out,m,x+nx,0,z+nz,color,a);
        vertex(out,m,u+nx,0,v+nz,color,a);
        vertex(out,m,u-nx,0,v-nz,color,a);
        vertex(out,m,x-nx,0,z-nz,color,a);
    }

    private static void vertex(VertexConsumer out, Matrix4f m,float x,float y,float z,int c,int a) {
        out.vertex(m,x,y,z).color(c>>16&255,c>>8&255,c&255,a).endVertex();
    }
}
