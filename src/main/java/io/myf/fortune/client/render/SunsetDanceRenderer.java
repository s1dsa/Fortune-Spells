package io.myf.fortune.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.myf.fortune.FortuneIds;
import io.myf.fortune.expansion.offense.SpellFigure;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

/** The server figures own slashes, petal flight, the detonation and the five-second field. */
final class SunsetDanceRenderer {
    private static final RenderType PETAL = surface("petal");
    private static final RenderType EMBER = surface("ember");
    private static final RenderType[] FLAMES = {surface("fire_0"), surface("fire_1"), surface("fire_2")};
    private static final int CRIMSON = 0xC41E46, ORANGE = 0xFF8B29, WHITE = 0xFFFFE8;
    private static final Map<SpellFigure, Float> PETAL_FLOORS = new WeakHashMap<>();
    private static final Map<SpellFigure, FieldFloor> FIELD_FLOORS = new WeakHashMap<>();
    private static final float GOLDEN_ANGLE = 2.3999632f;
    private static final FirePatch[] FIRE_PATCHES = prepareFirePatches();

    private SunsetDanceRenderer() {}

    private static RenderType surface(String name) {
        return RenderType.entityTranslucentEmissive(FortuneIds.id("textures/entity/sunset_dance/" + name + ".png"), false);
    }

    static void render(SpellFigure figure, float age, PoseStack pose, MultiBufferSource buffers) {
        switch (figure.kind()) {
            case DANCE -> slash(figure, age - figure.age(), pose, buffers);
            case PETAL -> petal(figure, age, pose, buffers);
            case FIRE_FIELD -> field(figure, age, pose, buffers);
            default -> {}
        }
    }

    private static void slash(SpellFigure figure, float partial, PoseStack pose, MultiBufferSource buffers) {
        int strike = figure.strikeNumber();
        float phase = (figure.ticksAfterStrike() + partial) / SpellFigure.SWING_TICKS;
        if (strike == 0 || phase >= 1) return;
        int side = (strike & 1) == 1 ? 1 : -1;
        float end = Math.min(1, .08f + phase * 1.42f);
        float begin = Mth.clamp((phase - .61f) * 1.8f, 0, .8f);
        float opacity = Math.min(1, (1 - phase) * 3.5f);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-figure.strikeYaw()));
        Matrix4f matrix = pose.last().pose();
        VertexConsumer v = buffers.getBuffer(RenderType.debugQuads());
        int slices = 36;
        for (int i = 0; i < slices; i++) {
            float a = (float)i / slices, b = (i + 1f) / slices;
            bladeBand(v,matrix,a,b,begin,end,side,.40f,1.29f,CRIMSON,(int)(215*opacity));
            bladeBand(v,matrix,a,b,begin,end,side,.11f,.40f,ORANGE,(int)(250*opacity));
            bladeBand(v,matrix,a,b,begin,end,side,0,.11f,WHITE,(int)(255*opacity));
        }
        pose.popPose();
    }

    private static void bladeBand(VertexConsumer v, Matrix4f matrix, float a, float b, float begin, float end, int side,
                                  float outer, float inner, int color, int alpha) {
        bladePoint(v,matrix,a,begin,end,side,outer,color,alpha);
        bladePoint(v,matrix,b,begin,end,side,outer,color,alpha);
        bladePoint(v,matrix,b,begin,end,side,inner,color,alpha);
        bladePoint(v,matrix,a,begin,end,side,inner,color,alpha);
    }

    private static void bladePoint(VertexConsumer v, Matrix4f matrix, float t, float begin, float end, int side,
                                   float inset, int color, int alpha) {
        float angle = side * (Mth.lerp(t,begin,end)*160-80) * Mth.DEG_TO_RAD;
        // Shape the crescent over the currently visible sweep, so the leading tip also closes mid-stroke.
        float fullness = Mth.sin(t*Mth.PI) * Mth.sqrt(end-begin);
        float radius = SpellFigure.SLASH_RANGE - inset * fullness;
        float x = radius * Mth.sin(angle);
        float y = 1.26f + side * x * .17f + fullness * (.38f - inset * .46f);
        colored(v,matrix,x,y,radius * Mth.cos(angle),color,alpha);
    }

    private static float landing(float age) {
        float t = Mth.clamp((age - 8) / 9, 0, 1);
        return t * t * (3 - 2*t);
    }

    private static void petal(SpellFigure figure, float age, PoseStack pose, MultiBufferSource buffers) {
        float settle = landing(age);
        float floor = 0;
        if (settle > 0) {
            if (age >= 17) floor = PETAL_FLOORS.computeIfAbsent(figure, f -> groundOffset(f, 0, 0, .1f));
            else floor = groundOffset(figure, 0, 0, .1f);
            if (!Float.isFinite(floor)) settle = 0;
        }
        float seed = figure.getId() * GOLDEN_ANGLE;
        pose.pushPose();
        pose.translate(0, (settle == 0 ? 0 : (floor + .13f) * settle)
                + (1-settle) * Mth.sin(age * .33f + seed) * .18f, 0);
        pose.mulPose(Axis.YP.rotationDegrees(seed * Mth.RAD_TO_DEG + Math.min(age,17) * 9));
        // Face up on landing, retaining a shallow curl in the tip. The emissive vein reads after dark.
        pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(settle, 24 + 28*Mth.sin(seed + age*.28f), -87)));
        PoseStack.Pose p = pose.last();
        int tint = settle < .5f ? 0xFFFFFF : 0xDF746E;
        VertexConsumer v = buffers.getBuffer(PETAL);
        tex(v,p,-.64f,-.42f,-.07f,0,1,tint,244);
        tex(v,p,.64f,-.42f,-.07f,1,1,tint,244);
        tex(v,p,.64f,.48f,.03f,1,0,tint,244);
        tex(v,p,-.64f,.48f,.03f,0,0,tint,244);
        pose.popPose();
    }

    private static float groundOffset(SpellFigure figure, float x, float z, float startY) {
        int scale = figure.effectScale();
        Vec3 start = figure.position().add(x*scale,startY*scale,z*scale);
        HitResult hit = figure.level().clip(new ClipContext(start,start.add(0,-12*scale,0),
                ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,figure));
        return hit.getType() == HitResult.Type.MISS ? Float.NaN : (float)(hit.getLocation().y - figure.getY()) / scale;
    }

    /** A stationary field only samples each terrain column once. Petal bursts outside its radius stay independent. */
    private static final class FieldFloor {
        final int half, width;
        final float[] heights;

        FieldFloor(SpellFigure figure) {
            half = 15 * figure.effectScale();
            width = 2 * half + 1;
            heights = new float[width * width];
            Arrays.fill(heights, Float.POSITIVE_INFINITY);
        }

        float at(SpellFigure figure, float x, float z) {
            int cellX = Mth.floor(figure.getX()+x*figure.effectScale()) - figure.getBlockX();
            int cellZ = Mth.floor(figure.getZ()+z*figure.effectScale()) - figure.getBlockZ();
            if (Math.abs(cellX)>half || Math.abs(cellZ)>half) return groundOffset(figure,x,z,2);
            int index = (cellX+half)*width + cellZ+half;
            if (heights[index] == Float.POSITIVE_INFINITY) heights[index] = groundOffset(figure,x,z,2);
            return heights[index];
        }
    }

    private static void field(SpellFigure figure, float age, PoseStack pose, MultiBufferSource buffers) {
        if (age >= 100) return;
        FieldFloor floor = FIELD_FLOORS.computeIfAbsent(figure, FieldFloor::new);
        float fade = Mth.clamp((100-age)/18,0,1);
        // Stagger the tips of the flames while sharing one material batch per animation frame.
        int frame = (int)(age/3) % FLAMES.length;
        for (int phase = 0; phase < FLAMES.length; phase++) {
            VertexConsumer v = buffers.getBuffer(FLAMES[(phase+frame)%FLAMES.length]);
            for (int i=phase; i<FIRE_PATCHES.length; i+=FLAMES.length) {
                FirePatch patch = FIRE_PATCHES[i];
                float y = floor.at(figure,patch.x,patch.z);
                if (!Float.isFinite(y)) continue;
                float lift = patch.height * (.82f + .18f*Mth.sin(age*.21f+i));
                float emergence = Mth.clamp((age+1-patch.distance*.44f)/3,0,1);
                flamePatch(v,pose.last(),patch,y+.035f,lift,(int)(228*fade*emergence));
            }
        }
        if (age < 13) wave(figure,age,pose,buffers,floor);
        if (age < 9) bursts(figure,age,pose,buffers,floor);
    }

    private record Corner(float x,float z) {
        static Corner bounded(float x,float z) {
            float distance = Mth.sqrt(x*x+z*z);
            float scale = distance>SpellFigure.FIRE_RADIUS ? SpellFigure.FIRE_RADIUS/distance : 1;
            return new Corner(x*scale,z*scale);
        }
    }

    private record FirePatch(float x,float z,float distance,float height,Corner a,Corner b,Corner c,Corner d) {}

    private static FirePatch[] prepareFirePatches() {
        FirePatch[] patches = new FirePatch[113];
        Random random = new Random(23811L);
        for (int i=0; i<patches.length; i++) {
            float radius = (SpellFigure.FIRE_RADIUS-.7f)*Mth.sqrt((i+.4f)/patches.length);
            float angle = GOLDEN_ANGLE*i;
            float x = radius*Mth.sin(angle) + (random.nextFloat()-.5f)*1.24f;
            float z = radius*Mth.cos(angle) + (random.nextFloat()-.5f)*1.24f;
            // Large overlapping lobes follow local drafts rather than pointing along the placement spiral.
            float wind = .6f + .92f*Mth.sin(.26f*x+.14f*z) - .69f*Mth.sin(.32f*z)
                    + (random.nextFloat()-.5f)*3.4f;
            float dx = Mth.sin(wind), dz = Mth.cos(wind);
            float width = 1.44f + random.nextFloat()*1.27f;
            float length = 3.06f + random.nextFloat()*2.36f;
            float lean = (random.nextFloat()-.5f)*.58f;
            float rootX = x-dx*length*.46f, rootZ = z-dz*length*.46f;
            float tipX = x+dx*length*.54f + lean*dz, tipZ = z+dz*length*.54f - lean*dx;
            float hx=width*dz, hz=-width*dx;
            patches[i] = new FirePatch(x,z,Mth.sqrt(x*x+z*z),.58f+random.nextFloat()*.64f,
                    Corner.bounded(rootX-hx,rootZ-hz), Corner.bounded(rootX+hx,rootZ+hz),
                    Corner.bounded(tipX+hx,tipZ+hz), Corner.bounded(tipX-hx,tipZ-hz));
        }
        return patches;
    }

    private static void flamePatch(VertexConsumer v, PoseStack.Pose p, FirePatch patch,float y,float lift,int alpha) {
        tex(v,p,patch.a.x,y,patch.a.z,0,1,0xFFE4B2,alpha);
        tex(v,p,patch.b.x,y,patch.b.z,1,1,0xFFE4B2,alpha);
        tex(v,p,patch.c.x,y+lift,patch.c.z,1,0,0xFFBAC4,alpha);
        tex(v,p,patch.d.x,y+lift,patch.d.z,0,0,0xFFBAC4,alpha);
    }

    private static void wave(SpellFigure figure, float age, PoseStack pose, MultiBufferSource buffers, FieldFloor floor) {
        float travel = Mth.clamp(age/8,0,1);
        float radius = Mth.lerp(1-(1-travel)*(1-travel),1.25f,SpellFigure.FIRE_RADIUS);
        int alpha = (int)(250 * Mth.clamp((13-age)/5,0,1));
        boolean flash = age < 2.5f;
        VertexConsumer v = buffers.getBuffer(RenderType.debugQuads());
        Matrix4f matrix = pose.last().pose();
        float centerY = flash ? floor.at(figure,0,0) : 0;
        for (int i=0; i<64; i++) {
            float a = i*Mth.TWO_PI/64, b = (i+1)*Mth.TWO_PI/64;
            float ax = Mth.sin(a), az=Mth.cos(a), bx=Mth.sin(b), bz=Mth.cos(b);
            if (flash && Float.isFinite(centerY)) {
                int intensity = (int)(170*(1-age/2.5f));
                float reach = SpellFigure.FIRE_RADIUS;
                colored(v,matrix,0,centerY+.13f,0,WHITE,intensity);
                colored(v,matrix,ax*reach,centerY+.18f,az*reach,WHITE,0);
                colored(v,matrix,bx*reach,centerY+.18f,bz*reach,WHITE,0);
                colored(v,matrix,0,centerY+.13f,0,WHITE,intensity);
            }
            float ay = floor.at(figure,ax*radius,az*radius), by = floor.at(figure,bx*radius,bz*radius);
            if (!Float.isFinite(ay) || !Float.isFinite(by)) continue;
            float inner = Math.max(.1f,radius-1.35f);
            float crest = radius-.29f;
            colored(v,matrix,ax*inner,ay+.07f,az*inner,CRIMSON,alpha/4);
            colored(v,matrix,bx*inner,by+.07f,bz*inner,CRIMSON,alpha/4);
            colored(v,matrix,bx*crest,by+.63f,bz*crest,flash ? WHITE : ORANGE,alpha);
            colored(v,matrix,ax*crest,ay+.63f,az*crest,flash ? WHITE : ORANGE,alpha);
            colored(v,matrix,ax*crest,ay+.63f,az*crest,WHITE,alpha);
            colored(v,matrix,bx*crest,by+.63f,bz*crest,WHITE,alpha);
            colored(v,matrix,bx*radius,by+.10f,bz*radius,ORANGE,alpha/2);
            colored(v,matrix,ax*radius,ay+.10f,az*radius,ORANGE,alpha/2);
        }
    }

    private static void bursts(SpellFigure figure, float age, PoseStack pose, MultiBufferSource buffers, FieldFloor floor) {
        VertexConsumer v = buffers.getBuffer(EMBER);
        int i=0;
        for (SpellFigure.BurstPetal petal : figure.burstPetals()) {
            float y = floor.at(figure,petal.x(),petal.z());
            y = Float.isFinite(y) ? Mth.lerp(landing(petal.age()),petal.y(),y+.08f) : petal.y();
            float scale = (1-age/9) * (age<2.5f ? 1.8f : 1.12f);
            for (int j=0;j<3;j++) {
                float angle = i * GOLDEN_ANGLE + j*Mth.TWO_PI/3;
                float dx = Mth.sin(angle), dz = Mth.cos(angle);
                float distance = .12f + age*.15f;
                float x=petal.x()+dx*distance, z=petal.z()+dz*distance;
                float top=y+.70f+age*.19f;
                PoseStack.Pose p=pose.last();
                int color = age<2.5f ? WHITE : 0xFF956B;
                tex(v,p,x-dz*scale,y,z+dx*scale,0,1,color,245);
                tex(v,p,x+dz*scale,y,z-dx*scale,1,1,color,245);
                tex(v,p,x+dz*scale+dx*.45f,top+scale,z-dx*scale+dz*.45f,1,0,color,245);
                tex(v,p,x-dz*scale+dx*.45f,top+scale,z+dx*scale+dz*.45f,0,0,color,245);
            }
            i++;
        }
    }

    private static void colored(VertexConsumer v, Matrix4f matrix, float x, float y, float z, int color, int alpha) {
        v.vertex(matrix,x,y,z).color(color>>16&255,color>>8&255,color&255,alpha).endVertex();
    }

    private static void tex(VertexConsumer v, PoseStack.Pose p,float x,float y,float z,
                            float u,float t,int color,int alpha) {
        v.vertex(p.pose(),x,y,z).color(color>>16&255,color>>8&255,color&255,alpha).uv(u,t)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(p.normal(),0,1,0).endVertex();
    }
}
