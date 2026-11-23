package io.myf.fortune.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.myf.fortune.FortuneIds;
import io.myf.fortune.expansion.offense.OffensiveSpells;
import io.myf.fortune.expansion.offense.SpellFigure;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/** Translucent, emissive spell geometry, built in world units. */
@Mod.EventBusSubscriber(modid = FortuneIds.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SpellFigureRenderer extends EntityRenderer<SpellFigure> {
    private static final ResourceLocation ATLAS = new ResourceLocation("minecraft","textures/atlas/blocks.png");
    private static final int IVORY=0xE8E2C8, JADE=0x6B8F3A, FANG=0xEDEFE6, AMBER=0xF2C14E,
            AQUA=0x7FE3D8, GOLD=0xB07A3A;
    private static final ResourceLocation SKULL=texture("croc_skull");
    private static final float SKULL_SCALE = 3.25f;

    private static ResourceLocation texture(String name) { return FortuneIds.id("textures/entity/spell/"+name+".png"); }

    public SpellFigureRenderer(EntityRendererProvider.Context context) { super(context); }
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(OffensiveSpells.FIGURE.get(), SpellFigureRenderer::new);
    }
    @Override public ResourceLocation getTextureLocation(SpellFigure e) { return ATLAS; }
    @Override public void render(SpellFigure e, float yaw, float partial, PoseStack p, MultiBufferSource buffers, int light) {
        p.pushPose();
        int scale = e.effectScale();
        p.scale(scale,scale,scale);
        renderFigure(e, partial, p, buffers);
        p.popPose();
    }

    private void renderFigure(SpellFigure e, float partial, PoseStack p, MultiBufferSource buffers) {
        if (e.isRune()) {
            if (e.runePhase() == SpellFigure.RunePhase.DRAWING) {
                ChantArrayRenderer.render(e, Mth.clamp((e.runeProgress() + partial) / 200f, 0, 1), p, buffers);
            }
            if (e.kind() != SpellFigure.Kind.WING_RUNE) GrandRitualRenderer.render(e,partial,p,buffers);
            return;
        }
        if (e.kind() == SpellFigure.Kind.DANCE) {
            ChantArrayRenderer.render(e, Mth.clamp((e.age() + partial) / SpellFigure.DANCE_TICKS, 0, 1), p, buffers);
        }
        if (e.hasReleaseCircles()) {
            ReleaseCircleRenderer.render(e, partial, p, buffers);
            if (e.kind() == SpellFigure.Kind.WING_HALO) return;
        }
        if (e.kind() == SpellFigure.Kind.DANCE || e.kind() == SpellFigure.Kind.PETAL || e.kind() == SpellFigure.Kind.FIRE_FIELD) {
            SunsetDanceRenderer.render(e,e.age()+partial,p,buffers);
            return;
        }
        VertexConsumer v = buffers.getBuffer(RenderType.lightning());
        float t = e.age()+partial;
        p.pushPose();
        p.mulPose(Axis.YP.rotationDegrees(-e.getYRot()));
        switch(e.kind()) {
            case JAWS -> {
                float sinceBite = (t - SpellFigure.FIRST_BITE) % SpellFigure.BITE_INTERVAL;
                if (t >= SpellFigure.FIRST_BITE && sinceBite < 5)
                    arc(p,v,SpellFigure.JAWS_RANGE,.26f,-70,70,IVORY,(int)((5-sinceBite)*36));
                p.scale(SKULL_SCALE, SKULL_SCALE, SKULL_SCALE);
                p.pushPose();
                jaws(p,buffers.getBuffer(RenderType.debugQuads()),t);
                p.popPose();
                float opening = opening(t);
                p.translate(0,.78 + Math.min(0,(t-3)*.24),.25);
                p.mulPose(Axis.XP.rotationDegrees(-opening));
                for (int side=-1;side<=1;side+=2) {
                    p.pushPose();
                    p.translate(side*1.07,.58,3.15);
                    p.mulPose(Axis.YP.rotationDegrees(side*90));
                    sprite(p,buffers,SKULL,5.2f,2.75f, (int)(205*Mth.clamp((SpellFigure.JAWS_DURATION-t)/6,0,1)));
                    p.popPose();
                }
            }
            case TOWER -> {
                ring(p,v,SpellFigure.TOWER_RADIUS,.12f,.1f,AQUA,110);
                HollowBellRenderer.render(p,buffers.getBuffer(RenderType.debugQuads()),t);
            }
            case CHIME -> {
                p.mulPose(Axis.XP.rotationDegrees(e.getXRot()));
                p.mulPose(Axis.XP.rotationDegrees(90));
                ring(p,v,1.1f,.11f,0,AMBER,210);
                ring(p,v,.82f,.06f,.35f,FANG,150);
                ring(p,v,.50f,.05f,.65f,GOLD,110);
            }
            case COLUMN -> {
                float h = SpellFigure.AREA_HEIGHT;
                lathe(p,v,-h,.25f*SpellFigure.COLUMN_RADIUS,h,.18f,12,AQUA,(int)Math.max(0,105-t*9));
                ring(p,v,1.8f,.12f,h*.65f,AQUA,200);
                ring(p,v,1.45f,.10f,h*.92f,IVORY,190);
                ring(p,v,SpellFigure.COLUMN_RADIUS,.12f,.08f,AQUA,155);
            }
        }
        p.popPose();
    }

    private static void jaws(PoseStack p,VertexConsumer v,float t) {
        float fade = Mth.clamp((SpellFigure.JAWS_DURATION-t)/6f,0,1);
        int a = (int)(195*fade);
        p.translate(0,.78 + Math.min(0,(t-3)*.24),.25);
        p.pushPose();
        p.mulPose(Axis.XP.rotationDegrees(-opening(t)));
        // Bony eye ridges, elongated snout and two nostrils.
        boolean closed = t >= SpellFigure.FIRST_BITE
                && (t - SpellFigure.FIRST_BITE) % SpellFigure.BITE_INTERVAL < 1;
        box(p,v,-1.36f,0,0,1.36f,.92f,2.4f,closed?FANG:JADE,a);
        box(p,v,-1.04f,0,2,1.04f,.54f,5.3f,IVORY,a);
        box(p,v,-1.11f,0,5,1.11f,.57f,5.78f,JADE,a);
        for (int side=-1;side<=1;side+=2) {
            p.pushPose();
            p.scale(side,1,1);
            box(p,v,.8f,.84f,1.05f,1.35f,1.27f,2.23f,IVORY,a);
            box(p,v,1.17f,.87f,1.54f,1.37f,1.08f,1.95f,AMBER,(int)(245*fade));
            box(p,v,.68f,.54f,5.24f,.85f,.64f,5.51f,IVORY,a);
            for (int i=0;i<7;i++) tooth(p,v,.88f,.015f,1.95f+i*.54f,-(.3f+i%3*.13f),a);
            p.popPose();
        }
        p.popPose();
        p.pushPose();
        p.mulPose(Axis.XP.rotationDegrees(opening(t)*.12f));
        box(p,v,-1.03f,-.55f,.45f,1.03f,-.18f,5.54f,IVORY,a);
        for (int i=0;i<6;i++) for (int side=-1;side<=1;side+=2)
            tooth(p,v,side*.92f,-.19f,2.15f+i*.55f,.29f,a);
        p.popPose();
    }
    private static void tooth(PoseStack p,VertexConsumer v,float x,float y,float z,float h,int a) {
        p.pushPose(); p.translate(x,y,z);
        lathe(p,v,0,.16f,h,.015f,4,FANG,a);
        p.popPose();
    }

    private static float opening(float t) {
        if (t < SpellFigure.FIRST_BITE)
            return 60 * Math.min(Mth.clamp(t/5, 0, 1), Mth.clamp((SpellFigure.FIRST_BITE-t)/5, 0, 1));
        if (t >= SpellFigure.LAST_BITE) return 0;
        float phase = (t-SpellFigure.FIRST_BITE) % SpellFigure.BITE_INTERVAL;
        return 60 * Math.min(Mth.clamp(phase/8, 0, 1), Mth.clamp((SpellFigure.BITE_INTERVAL-phase)/4, 0, 1));
    }
    private static VertexConsumer surface(MultiBufferSource buffers,ResourceLocation texture) {
        return buffers.getBuffer(RenderType.entityTranslucentEmissive(texture,false));
    }
    private static void sprite(PoseStack p,MultiBufferSource buffers,ResourceLocation tex,float w,float h,int alpha) {
        VertexConsumer v=surface(buffers,tex);
        textured(v,p,-w/2,-h/2,0,0,1,alpha); textured(v,p,w/2,-h/2,0,1,1,alpha);
        textured(v,p,w/2,h/2,0,1,0,alpha); textured(v,p,-w/2,h/2,0,0,0,alpha);
    }
    private static void textured(VertexConsumer v,PoseStack p,float x,float y,float z,float u,float t,int a) {
        v.vertex(p.last().pose(),x,y,z).color(255,255,255,a).uv(u,t).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(0xF000F0).normal(p.last().normal(),0,0,1).endVertex();
    }

    private static void ring(PoseStack p,VertexConsumer v,float radius,float width,float y,int color,int a) {
        p.pushPose(); p.translate(0,y,0); arc(p,v,radius,width,0,360,color,a); p.popPose();
    }
    private static void arc(PoseStack p,VertexConsumer v,float radius,float width,float from,float to,int color,int a) {
        int steps=Math.max(4,(int)((to-from)/8));
        Matrix4f m=p.last().pose();
        for(int i=0;i<steps;i++) {
            float t1=(from+(to-from)*i/steps)*Mth.DEG_TO_RAD,t2=(from+(to-from)*(i+1)/steps)*Mth.DEG_TO_RAD;
            point(v,m,Mth.sin(t1)*radius,0,Mth.cos(t1)*radius,color,a);
            point(v,m,Mth.sin(t2)*radius,0,Mth.cos(t2)*radius,color,a);
            point(v,m,Mth.sin(t2)*(radius-width),0,Mth.cos(t2)*(radius-width),color,a);
            point(v,m,Mth.sin(t1)*(radius-width),0,Mth.cos(t1)*(radius-width),color,a);
        }
    }
    private static void lathe(PoseStack p,VertexConsumer v,float y1,float r1,float y2,float r2,int n,int color,int a) {
        Matrix4f m=p.last().pose();
        for(int i=0;i<n;i++) {
            float t1=2*Mth.PI*i/n,t2=2*Mth.PI*(i+1)/n;
            int shade=(int)(a*(.84+.16*Mth.cos(t1)));
            point(v,m,r1*Mth.sin(t1),y1,r1*Mth.cos(t1),color,shade);
            point(v,m,r1*Mth.sin(t2),y1,r1*Mth.cos(t2),color,shade);
            point(v,m,r2*Mth.sin(t2),y2,r2*Mth.cos(t2),color,shade);
            point(v,m,r2*Mth.sin(t1),y2,r2*Mth.cos(t1),color,shade);
        }
    }
    private static final int[][] FACES={{0,1,3,2},{4,6,7,5},{0,4,5,1},{2,3,7,6},{0,2,6,4},{1,5,7,3}};
    private static void box(PoseStack p,VertexConsumer v,float x,float y,float z,float X,float Y,float Z,int color,int a) {
        Matrix4f m=p.last().pose();
        for (int face=0;face<6;face++) for(int i:FACES[face])
            point(v,m,(i&1)==0?x:X,(i&2)==0?y:Y,(i&4)==0?z:Z,color,face==3?a:(int)(a*.79));
    }
    private static void point(VertexConsumer v,Matrix4f m,float x,float y,float z,int color,int alpha) {
        v.vertex(m,x,y,z).color((color>>16)&255,(color>>8)&255,color&255,alpha).endVertex();
    }
}
