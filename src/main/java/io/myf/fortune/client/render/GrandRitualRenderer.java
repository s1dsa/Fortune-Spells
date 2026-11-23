package io.myf.fortune.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.myf.fortune.FortuneIds;
import io.myf.fortune.expansion.offense.SpellFigure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.WeakHashMap;

/** World-space ritual layers. The server supplies the cast progress, outcome and transition time. */
final class GrandRitualRenderer {
    private record School(RenderType outer, RenderType seal, RenderType satellite, RenderType crest, RenderType dial,
                          int light, int dark, int ivory, DustParticleOptions sparks) {
        School(String name,int light,int dark,int ivory,int spark) {
            this(layer(name,"outer"),layer(name,"seal"),layer(name,"satellite"),layer(name,"crest"),layer(name,"dial_cache"),
                    light,dark,ivory,new DustParticleOptions(new Vector3f((spark>>16)/255f,
                    ((spark>>8)&255)/255f,(spark&255)/255f),1.1f));
        }
        private static RenderType layer(String school,String name) {
            return RenderType.entityTranslucentEmissive(
                    FortuneIds.id("textures/entity/spell/ritual/"+school+"/"+name+".png"),false);
        }
    }

    private static final School SWAMP = new School("swamp",0xB7D45A,0x3D5A24,0xE8E2C8,0x8C7B4F);
    private static final School BELL = new School("bell",0x7FE3D8,0x2E7C78,0xEDEFE6,0xF2C14E);
    private static final int SECTORS=48;
    private static final float[] X=new float[SECTORS+1], Z=new float[SECTORS+1];
    private static final int JAW_SEGMENTS = 28;
    private static final float[] JAW_X = new float[JAW_SEGMENTS + 1], JAW_Z = new float[JAW_SEGMENTS + 1];
    private static final int JADE = 0x55E6DC, AMBER = 0xF4BD76, LILAC = 0xB6A0F9;
    private static final int BRONZE = 0xF4C688, RECESS = 0x4A3444;
    // The full bell uses twelve emblems, one inner seal and eight gate layers.
    private static final class Plane {
        final Matrix4f model = new Matrix4f();
        final Matrix3f normal = new Matrix3f();
        RenderType material;
        float radius, drawn, brightness;
    }
    private static final Plane[] PLANES = new Plane[24];
    private static final Matrix4f FRAGMENT = new Matrix4f();
    private static final Matrix3f FRAGMENT_NORMAL = new Matrix3f();
    private static final Matrix4f TOOTH = new Matrix4f();
    private static final Matrix4f[] LOWER_TURNS = turns(12), UPPER_TURNS = turns(8);
    private static Matrix4f[] turns(int count) {
        Matrix4f[] turns = new Matrix4f[count];
        for (int i = 0; i < count; i++) turns[i] = new Matrix4f().rotationY(i * Mth.TWO_PI / count);
        return turns;
    }
    static {
        for (int i=0;i<PLANES.length;i++) PLANES[i]=new Plane();
        for(int i=0;i<=SECTORS;i++) {
            float a=i*Mth.TWO_PI/SECTORS;
            X[i]=Mth.sin(a); Z[i]=Mth.cos(a);
        }
        for (int i = 0; i <= JAW_SEGMENTS; i++) {
            float angle = (-70 + 140f * i / JAW_SEGMENTS) * Mth.DEG_TO_RAD;
            JAW_X[i] = Mth.sin(angle);
            JAW_Z[i] = Mth.cos(angle);
        }
    }
    private static final WeakHashMap<SpellFigure,Long> LAST_EMISSION=new WeakHashMap<>();
    private static long budgetTick=Long.MIN_VALUE;
    private static int availableParticles;

    private final PoseStack pose;
    private final MultiBufferSource buffers;
    // Only used by draw(), before flushPlanes changes the active material.
    private final VertexConsumer geometry;
    private final School school;
    private final RitualDialCache dialCache;
    private final boolean settled;
    private final float progress,rotationClock,release,shatter,opacity,intensity;
    private final boolean flashing;
    private final int overlay;
    private int planeCount;

    private GrandRitualRenderer(SpellFigure figure,int phaseAge,float partial,PoseStack pose,MultiBufferSource buffers) {
        this.pose=pose;
        this.buffers=buffers;
        geometry = buffers.getBuffer(RenderType.debugQuads());
        school=figure.kind()==SpellFigure.Kind.SWAMP_RUNE ? SWAMP : BELL;
        dialCache = new RitualDialCache(school.dial);
        SpellFigure.RunePhase phase=figure.runePhase();
        float elapsed=phaseAge+partial;
        release=phase==SpellFigure.RunePhase.RELEASED ? elapsed : -1;
        shatter=phase==SpellFigure.RunePhase.SHATTERING ? clamp(elapsed/10) : 0;
        progress=Math.min(200,figure.runeProgress()+(phase==SpellFigure.RunePhase.DRAWING ? partial : 0));
        settled = phase == SpellFigure.RunePhase.DRAWING && progress >= 56;
        rotationClock = progress + Math.max(release, 0);
        intensity = .66f + .22f * stage(19, 94) + .12f * stage(172, 200);
        opacity=(1-shatter)*(release<2 ? 1 : 1-clamp((release-2)/15));
        flashing=release>=0 && release<2;
        overlay=flashing ? OverlayTexture.pack(15,10) : OverlayTexture.NO_OVERLAY;
    }

    static void render(SpellFigure figure,float partial,PoseStack pose,MultiBufferSource buffers) {
        if (figure.runePhase()==SpellFigure.RunePhase.DRAWING) RitualAfterglow.observed(figure);
        render(figure,figure.runePhaseAge(),partial,pose,buffers);
    }

    static void render(SpellFigure figure,int phaseAge,float partial,PoseStack pose,MultiBufferSource buffers) {
        var circle=new GrandRitualRenderer(figure,phaseAge,partial,pose,buffers);
        circle.draw(figure);
        circle.dialCache.flush(buffers, circle.intensity);
        circle.flushPlanes(circle.school.outer);
        circle.flushPlanes(circle.school.seal);
        circle.flushPlanes(circle.school.satellite);
        circle.flushPlanes(circle.school.crest);
        circle.emitSparks(figure);
    }

    private void draw(SpellFigure figure) {
        pose.pushPose();
        pose.translate(0, .075, 0);
        if (shatter > 0) pose.translate(0, .55f * Mth.sin(Mth.PI * shatter) - .36f * shatter * shatter, 0);

        // 3.2 seconds of the storyboard correspond to the spell's 200-tick chant.
        // Once it resolves, geometry uses the tracked release clock, including for late observers.
        innerSeal();
        float ground = stage(19, 56);
        if (ground > 0) ground(ground);
        crown(9, 8, 56, 86, -11, false);
        crown(6, 10, 68, 94, 17, true);
        gates(figure.getYRot());
        if (release >= 0) releaseBoundary(figure.getYRot());
        pose.popPose();
    }

    private void releaseBoundary(float yaw) {
        if (school == SWAMP) {
            // The jaws draw their own impact arc at tick 12. This anticipates that bite.
            if (release >= 12) return;
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(-yaw));
            float strength = 1 - clamp((release - 10) / 2);
            boundaryBand(JAW_X, JAW_Z, SpellFigure.JAWS_RANGE, .22f, .08f, school.light, .78f * strength);
            pose.popPose();
        } else {
            // The tower keeps its static field ring. This single travelling front reaches
            // that edge at tick 4, stays readable for ten ticks, then fades with the rune.
            float outward = clamp(release / 4);
            float radius = Mth.lerp(outward * (2 - outward), 2, SpellFigure.TOWER_RADIUS);
            float strength = 1 - clamp((release - 14) / 3);
            boundaryBand(X, Z, radius, .34f, .06f, school.light, .50f * strength);
            boundaryBand(X, Z, radius, .12f, .076f, school.ivory, .92f * strength);
        }
    }

    private void boundaryBand(float[] x, float[] z, float radius, float width, float height,
                              int color, float strength) {
        if (strength <= 0) return;
        Matrix4f matrix = pose.last().pose();
        float inner = radius - width;
        int rgb = flashing ? school.ivory : color;
        int alpha = (int)(255 * strength);
        for (int i = 0; i < x.length - 1; i++) {
            colorVertex(geometry, matrix, radius * x[i], height, radius * z[i], rgb, alpha);
            colorVertex(geometry, matrix, radius * x[i+1], height, radius * z[i+1], rgb, alpha);
            colorVertex(geometry, matrix, inner * x[i+1], height, inner * z[i+1], rgb, alpha);
            colorVertex(geometry, matrix, inner * x[i], height, inner * z[i], rgb, alpha);
        }
    }

    private void innerSeal() {
        float join = stage(0, 19);
        float radius = 2 + 1.35f * (1 - join);
        float fade = 1 - stage(18, 48) * .35f;
        for (int i = 0; i < 6; i++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(i * 60));
            stroke(-.16f, radius - .22f, 0, radius + .21f, .06f, school.ivory, fade);
            stroke(0, radius + .21f, .16f, radius - .22f, .06f, school.ivory, fade);
            pose.popPose();
        }
        layer(school.seal, 2, 0, join, .012f);
        if (join > 0) arc(2, .10f, .048f, school.light, .86f, (int)(join * SECTORS));
    }

    private void ground(float unfold) {
        float outer = stage(32, 56);
        float retreat = release < 2 ? 1 : 1 - clamp((release - 2) / 15);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(rotationClock * .20f));
        if (settled) dialCache.floor(pose, true);
        else if (outer > 0 && retreat > .06f) dial(outer * retreat);
        pose.popPose();
        if (settled) dialCache.floor(pose, false);
        else if (retreat > .2f) {
            arc(7, .10f, .04f, school.light, .90f, (int)(outer * retreat * SECTORS));
            arc(10, .08f, .055f, AMBER, .60f, (int)(outer * retreat * SECTORS));
        }

        for (int i = 0; i < 12; i++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(i * 30));
            int accent = switch (i % 3) {
                case 0 -> JADE;
                case 1 -> AMBER;
                default -> LILAC;
            };
            if ((i & 1) == 0) tracks(i / 2, unfold, retreat);
            float show = stage(34 + i % 3 * 3, 53 + i % 3);
            if (show > 0 && retreat > .14f) {
                pose.pushPose();
                pose.translate(0, .10, 10);
                pose.mulPose(Axis.YP.rotationDegrees(180));
                float size = (i % 2 == 0 ? 1.50f : 1.29f) * show * Math.min(1, retreat * 2);
                layer(school.crest, size, 0, 1, 0);
                pose.popPose();
                // Four different sockets under the break in the enamel: line, split, chevron, diamond.
                if (!settled) socket(i % 4, accent, show * retreat);
            }
            pose.popPose();
        }
    }

    // The index band turns independently of the twelve fixed emblems at ten blocks.
    // A dark bevel sits below the thin luminous brass edge to keep the dial legible on grass.
    private void dial(float drawn) {
        int sectors = Math.min(SECTORS, (int)Math.ceil(drawn * SECTORS));
        arc(12, .27f, .013f, RECESS, .93f, sectors);
        arc(12, .105f, .030f, school.ivory, 1, sectors);
        arc(11.54f, .085f, .036f, school.light, .91f, sectors);
        int marks = Math.min(72, (int)Math.ceil(drawn * 72));
        for (int i = 0; i < marks; i++) {
            float a = i * Mth.TWO_PI / 72;
            float x = Mth.sin(a), z = Mth.cos(a);
            boolean index = i % 6 == 0;
            float start = index ? 11.23f : i % 2 == 0 ? 11.51f : 11.63f;
            stroke(x * start, z * start, x * 11.84f, z * 11.84f,
                    index ? .085f : .042f, index ? AMBER : school.light, index ? 1 : .84f);
        }
    }

    private void socket(int kind, int color, float light) {
        float z = 8.62f;
        if (kind == 0 || kind == 3) stroke(0, z - .26f, 0, z + .2f, .055f, color, light);
        if (kind != 0) {
            stroke(-.22f, z + .13f, 0, z - .12f, .05f, color, light);
            stroke(0, z - .12f, .22f, z + .13f, .05f, color, light);
        }
        if (kind == 1 || kind == 3) stroke(-.18f, z + .23f, .18f, z + .23f, .05f, color, light);
    }

    // Track coordinates are shared by the recessed rail and all three moving light heads.
    private static final float[] TRACK_X = {0, 0, .72f, .72f, 0};
    private static final float[] TRACK_Z = {2.1f, 5.65f, 6.58f, 7.44f, 8.68f};
    private static final float[] TRACK_STEPS = {0, .51f, .66f, .79f, 1};

    private void tracks(int spoke, float unfold, float retreat) {
        float extent = Math.min(1, unfold * 1.55f) * retreat;
        if (!settled) {
            for (int rail = -1; rail <= 1; rail += 2) {
                float offset = rail * .16f;
                trackSegment(offset, 0, extent, .14f, RECESS, .94f);
                trackSegment(offset, 0, extent, .078f, BRONZE, 1);
            }
        }
        if (progress < 94) {
            trackSegment(0, Math.max(0, extent - .17f), extent, .10f, school.ivory, .94f);
            return;
        }
        // The priming light crosses the kink before passing to the ring at r=10.
        float cycle = ((progress - 94) / 42f - spoke / 12f + 1) % 1;
        float charge = stage(94, 112);
        if (progress > 182 || release >= 0) {
            // All six junctions answer together, then the last light moves back toward the caster.
            cycle = release < 0 ? 1 - .87f * stage(182, 200) : .13f * (1 - clamp(release / 2));
        }
        lightHead(-.16f, cycle, JADE, charge * retreat);
        lightHead(.16f, cycle - .11f, AMBER, charge * retreat);
        lightHead(.16f, cycle - .28f, LILAC, charge * retreat);
        if (release < 0 && cycle > .72f) {
            arc(10, .18f, .155f, school.light, charge, (int)(Math.min(1, (cycle - .72f) / .28f) * 8));
        }
    }

    private void lightHead(float offset, float head, int color, float strength) {
        if (head <= 0 || strength <= 0) return;
        trackSegment(offset, Math.max(0, head - .19f), head, .11f, color, .65f * strength);
        trackSegment(offset, Math.max(0, head - .07f), head, .075f, color, strength);
        trackSegment(offset, Math.max(0, head - .025f), head, .035f, school.ivory, strength);
    }

    private void trackSegment(float offset, float from, float to, float width, int color, float light) {
        for (int j = 0; j < TRACK_STEPS.length - 1; j++) {
            float a = TRACK_STEPS[j], b = TRACK_STEPS[j + 1];
            float low = Math.max(a, from), high = Math.min(b, to);
            if (high <= low) continue;
            float t0 = (low - a) / (b - a), t1 = (high - a) / (b - a);
            stroke(offset + Mth.lerp(t0, TRACK_X[j], TRACK_X[j + 1]), Mth.lerp(t0, TRACK_Z[j], TRACK_Z[j + 1]),
                   offset + Mth.lerp(t1, TRACK_X[j], TRACK_X[j + 1]), Mth.lerp(t1, TRACK_Z[j], TRACK_Z[j + 1]),
                   width, color, light);
        }
    }

    private void crown(float radius, float height, int start, int end, float degreesPerSecond, boolean upper) {
        float unfold = stage(start, end);
        float fade = release < 2 ? 1 : 1 - clamp((release - 2) / (upper ? 7 : 12));
        if (unfold <= 0 || fade <= 0) return;
        float size = radius * unfold;
        pose.pushPose();
        pose.translate(0, height - 1.4f * (1 - unfold) - shatter * shatter * 3, 0);
        pose.mulPose(Axis.YP.rotationDegrees(rotationClock * degreesPerSecond / 20));
        Matrix4f[] rotations = upper ? UPPER_TURNS : LOWER_TURNS;
        // A narrow dark enamel band keeps the crown outline visible against the daytime sky.
        belt(size, -.13f, .24f, school.dark, .74f * fade);
        belt(size + .016f, -.13f, .07f, school.ivory, .88f * fade);
        belt(size + .02f, .09f, .065f, upper ? AMBER : school.light, fade);
        ring(size, .16f, .09f, school.ivory, .65f * fade);
        for (int i = 0; i < rotations.length; i++) {
            Matrix4f tooth = TOOTH.set(pose.last().pose()).mulAffine(rotations[i]);
            if (settled && unfold >= 1) {
                dialCache.tooth(tooth, size + .025f, i % 3, upper);
                continue;
            }
            float span = (upper ? 1.12f : 1.30f) * unfold;
            float peak = (upper ? 1.05f : .87f) * unfold;
            float y = .11f;
            int tint = i % 3 == 0 ? AMBER : i % 3 == 1 ? school.light : LILAC;
            // Three tines share the ground symbol's silhouette; the open waist exposes the horizon.
            float base = size + .025f;
            crownStroke(tooth, -span, y, -.92f * span, y + .63f * peak, base, tint, fade);
            crownStroke(tooth, -.92f * span, y + .63f * peak, -.46f * span, y + .33f * peak, base, tint, fade);
            crownStroke(tooth, -.46f * span, y + .33f * peak, 0, y + peak, base, school.ivory, fade);
            crownStroke(tooth, 0, y + peak, .46f * span, y + .33f * peak, base, school.ivory, fade);
            crownStroke(tooth, .46f * span, y + .33f * peak, .92f * span, y + .63f * peak, base, tint, fade);
            crownStroke(tooth, .92f * span, y + .63f * peak, span, y, base, tint, fade);
            crownStroke(tooth, -.40f * span, -.25f, 0, -.66f, base, tint, .85f * fade);
            crownStroke(tooth, 0, -.66f, .40f * span, -.25f, base, tint, .85f * fade);
        }
        pose.popPose();
    }

    private void belt(float radius, float y, float h, int color, float light) {
        VertexConsumer v = geometry;
        Matrix4f m = pose.last().pose();
        // Twenty-four panels line up with the twelve lower / eight upper emblems.
        for (int j = 0; j < SECTORS; j += 2) {
            float x = radius * X[j], z = radius * Z[j];
            float ex = radius * X[j + 2], ez = radius * Z[j + 2];
            solid(v, m, x, y, z, color, light);
            solid(v, m, ex, y, ez, color, light);
            solid(v, m, ex, y + h, ez, color, light);
            solid(v, m, x, y + h, z, color, light);
        }
    }

    private void crownStroke(Matrix4f m, float x, float y, float ex, float ey, float z, int color, float light) {
        float dx = ex - x, dy = ey - y;
        float factor = .085f / Math.max(.001f, Mth.sqrt(dx * dx + dy * dy));
        float ox = -dy * factor, oy = dx * factor;
        VertexConsumer v = geometry;
        solid(v, m, x + ox, y + oy, z, color, light);
        solid(v, m, ex + ox, ey + oy, z, color, light);
        solid(v, m, ex - ox, ey - oy, z, color, light);
        solid(v, m, x - ox, y - oy, z, color, light);
    }

    private void gates(float yaw) {
        float open = stage(116, 156) * (release < 2 ? 1 : 1 - clamp((release - 2) / 12));
        if (open<=0) return;
        boolean swamp=school==SWAMP;
        int count=swamp ? 1 : 4;
        for (int i=0;i<count;i++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(-yaw+90*i));
            float radius=swamp ? 6 : 4;
            // The jaw starts directly behind this plane and extends through it on release.
            pose.translate(0,(swamp ? 4.85 : 4.05)-3.2*shatter*shatter,swamp ? 2.1 : 10.1);
            pose.mulPose(Axis.XP.rotationDegrees(90));
            float rotation=rotationClock*(i%2==0 ? .57f : -.57f);
            layer(school.outer, radius * open, rotation, open, 0, .62f);
            layer(school.satellite, radius * .70f * open, -rotation, open, .025f, .83f);
            pose.mulPose(Axis.YP.rotationDegrees(rotationClock*1.6f+i*40));
            arc(radius*open,.12f,.05f,school.ivory,open,5);
            pose.popPose();
        }
    }

    private void layer(RenderType texture,float radius,float rotation,float drawn,float height) {
        layer(texture, radius, rotation, drawn, height, 1);
    }

    private void layer(RenderType texture, float radius, float rotation, float drawn, float height, float brightness) {
        if (drawn<=0 || radius<=0) return;
        Plane plane=PLANES[planeCount++];
        plane.material=texture;
        plane.radius=radius;
        plane.drawn=drawn;
        plane.brightness = flashing ? 1 : brightness;
        plane.model.set(pose.last().pose()).translate(0,height,0).rotateY(rotation*Mth.DEG_TO_RAD);
        plane.normal.set(pose.last().normal()).rotateY(rotation*Mth.DEG_TO_RAD);
    }

    // One submission per material, including all shards and their translucent halos.
    private void flushPlanes(RenderType material) {
        VertexConsumer v=null;
        if (opacity <= 0) return;
        for (int i=0;i<planeCount;i++) {
            Plane plane=PLANES[i];
            if (plane.material!=material) continue;
            int halo = (int)(45 * opacity * intensity * plane.brightness);
            int ink = (int)(250 * opacity * intensity * plane.brightness);
            // The crest and gate artwork already contains its luminous edge. A second, nearly
            // coincident quad only adds fill cost during the ten-second charge.
            if (!flashing && shatter == 0 && material != school.seal) halo = 0;
            if (v==null) v=buffers.getBuffer(material);
            if (shatter==0 && plane.drawn>=1) {
                quad(v,plane,plane.radius*1.015f,0,halo);
                quad(v,plane,plane.radius,.009f,ink);
            } else {
                slices(v,plane,halo,ink);
            }
        }
    }

    private void quad(VertexConsumer v,Plane plane,float r,float y,int alpha) {
        if (alpha==0) return;
        vertex(v,plane.model,plane.normal,-1,-1,r,y,alpha);
        vertex(v,plane.model,plane.normal,-1,1,r,y,alpha);
        vertex(v,plane.model,plane.normal,1,1,r,y,alpha);
        vertex(v,plane.model,plane.normal,1,-1,r,y,alpha);
    }

    private void slices(VertexConsumer v,Plane plane,int halo,int ink) {
        float limit=plane.drawn*SECTORS;
        for(int sector=0;sector<limit;sector+=4) {
            Matrix4f model=plane.model;
            Matrix3f normal=plane.normal;
            if (shatter>0) {
                float dx=X[sector+2],dz=Z[sector+2];
                float drift=plane.radius*(.18f+.13f*Mth.sin(sector+1))*shatter;
                float lift=(.22f+.16f*Mth.cos(sector))*Mth.sin(shatter*Mth.PI)
                        -shatter*shatter*(.25f+.15f*Mth.cos(sector));
                float rx=12*Mth.DEG_TO_RAD*shatter*dz, rz=-14*Mth.DEG_TO_RAD*shatter*dx;
                model=FRAGMENT.set(model).translate(dx*drift,lift,dz*drift).rotateX(rx).rotateZ(rz);
                normal=FRAGMENT_NORMAL.set(normal).rotateX(rx).rotateZ(rz);
            }
            // Each wedge uses a real quad, with two chords along its outer rim.
            float end=Math.min(sector+4,limit), mid=Math.min(sector+2,end);
            float ex=sampleX(end), ez=sampleZ(end), mx=sampleX(mid), mz=sampleZ(mid);
            if (halo>0) wedge(v,model,normal,plane.radius*1.015f,0,halo,sector,mx,mz,ex,ez);
            wedge(v,model,normal,plane.radius,.009f,ink,sector,mx,mz,ex,ez);
        }
    }

    private static float sampleX(float p) { return p==(int)p ? X[(int)p] : Mth.sin(p*Mth.TWO_PI/SECTORS); }
    private static float sampleZ(float p) { return p==(int)p ? Z[(int)p] : Mth.cos(p*Mth.TWO_PI/SECTORS); }
    private void wedge(VertexConsumer v,Matrix4f m,Matrix3f n,float r,float y,int alpha,int start,
                       float mx,float mz,float ex,float ez) {
        vertex(v,m,n,0,0,r,y,alpha); vertex(v,m,n,-ex,ez,r,y,alpha);
        vertex(v,m,n,-mx,mz,r,y,alpha); vertex(v,m,n,-X[start],Z[start],r,y,alpha);
    }
    private void vertex(VertexConsumer v,Matrix4f m,Matrix3f n,float x,float z,float r,float y,int alpha) {
        v.vertex(m,x*r,y,z*r).color(255,255,255,alpha).uv(.5f+.5f*x,.5f+.5f*z)
                .overlayCoords(overlay).uv2(0xF000F0).normal(n.m10(),n.m11(),n.m12()).endVertex();
    }

    private void stroke(float x, float z, float ex, float ez, float w, int color, float brightness) {
        float dx = ex - x, dz = ez - z;
        float inverse = w / Math.max(.0001f, Mth.sqrt(dx * dx + dz * dz));
        float ox = -dz * inverse, oz = dx * inverse;
        VertexConsumer v = geometry;
        Matrix4f m = pose.last().pose();
        float y = color == RECESS ? .115f : color == BRONZE ? .122f : color == school.ivory ? .148f : .14f;
        solid(v, m, x + ox, y, z + oz, color, brightness);
        solid(v, m, ex + ox, y, ez + oz, color, brightness);
        solid(v, m, ex - ox, y, ez - oz, color, brightness);
        solid(v, m, x - ox, y, z - oz, color, brightness);
    }
    private void ring(float r,float w,float y,int color,float brightness) {
        arc(r,w,y,color,brightness,SECTORS);
    }
    private void arc(float r,float w,float y,int color,float brightness,int count) {
        VertexConsumer v=geometry;
        Matrix4f m=pose.last().pose();
        for(int j=0;j<count;j++) {
            solid(v,m,X[j]*r,y,Z[j]*r,color,brightness);
            solid(v,m,X[j+1]*r,y,Z[j+1]*r,color,brightness);
            solid(v,m,X[j+1]*(r-w),y,Z[j+1]*(r-w),color,brightness);
            solid(v,m,X[j]*(r-w),y,Z[j]*(r-w),color,brightness);
        }
    }
    private void solid(VertexConsumer v,Matrix4f m,float x,float y,float z,int color,float brightness) {
        int rgb=flashing ? school.ivory : color;
        colorVertex(v, m, x, y, z, rgb, (int)(255 * opacity * intensity * brightness));
    }

    private static void colorVertex(VertexConsumer v, Matrix4f m, float x, float y, float z, int rgb, int alpha) {
        // VertexConsumer's matrix overload constructs a temporary Vector4f per vertex.
        v.vertex(m.m00()*x + m.m10()*y + m.m20()*z + m.m30(),
                 m.m01()*x + m.m11()*y + m.m21()*z + m.m31(),
                 m.m02()*x + m.m12()*y + m.m22()*z + m.m32())
                .color((rgb>>16)&255,(rgb>>8)&255,rgb&255,alpha).endVertex();
    }
    private void emitSparks(SpellFigure figure) {
        if (progress < 94 || release >= 0 || shatter > 0) return;
        long tick=figure.level().getGameTime();
        Long last=LAST_EMISSION.get(figure);
        if(last!=null && last==tick)return;
        LAST_EMISSION.put(figure,tick);
        if(budgetTick!=tick) { budgetTick=tick; availableParticles=40; }
        int count=switch(Minecraft.getInstance().options.particles().get()) {
            case ALL -> 2;
            case DECREASED -> 1;
            case MINIMAL -> 0;
        };
        count=Math.min(count,availableParticles);
        availableParticles-=count;
        var world=figure.level();
        for(int i=0;i<count;i++) {
            float a=world.random.nextFloat()*Mth.TWO_PI;
            float radius=world.random.nextBoolean() ? 11.5f+world.random.nextFloat() : 7.7f+world.random.nextFloat();
            world.addParticle(school.sparks,figure.getX()+radius*Mth.sin(a),figure.getY()+.18,
                    figure.getZ()+radius*Mth.cos(a),0,.12+world.random.nextFloat()*.08,0);
        }
    }
    private float stage(float start,float end) { return clamp((progress-start)/(end-start)); }
    private static float clamp(float x) { return Mth.clamp(x,0,1); }
}
