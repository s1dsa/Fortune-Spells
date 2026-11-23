package io.myf.fortune.client.audience;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.myf.fortune.expansion.audience.CataclysmField;
import io.myf.fortune.expansion.audience.CataclysmSpell;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

import static io.myf.fortune.client.audience.AudienceGlyphs.*;

public final class CataclysmRenderer extends EntityRenderer<CataclysmField> {
    public static final float FIELD_RADIUS = 544;
    private static final int[] FIRE = {0xFF483E,0xFF9C3B,0xFFC373,0xE754A8,0xC198FF,0xFF784B};
    private static final int HOUSES = 12;
    private static final int SKY_HOUSES = 18;
    private static final int GATES = 9;
    private static final float GATE_SPACING = 42;
    public static final float TOP = 22 + (GATES - 1) * GATE_SPACING;

    public CataclysmRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public ResourceLocation getTextureLocation(CataclysmField field) { return InventoryMenu.BLOCK_ATLAS; }
    @Override public boolean shouldRender(CataclysmField field, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(new AABB(field.position(), field.position()).inflate(FIELD_RADIUS + 5, 3, FIELD_RADIUS + 5)
                .expandTowards(0, TOP + 28, 0));
    }

    @Override public void render(CataclysmField field, float yaw, float partial, PoseStack pose,
                                 MultiBufferSource buffers, int light) {
        float age = field.age(partial);
        float t = field.released() ? CataclysmSpell.CHANT_TICKS : Math.min(CataclysmSpell.CHANT_TICKS, age);
        float burn = field.impacted() ? field.impactAge(partial) : 0;
        float visible = 1 - smooth((burn - 4) / 35);
        VertexConsumer out = buffers.getBuffer(RenderType.debugQuads());
        // Independent elevations and angular rates let the complete hierarchy read from below and above.
        if (visible > .002f) {
            float grow = smooth((t + 2) / 135);
            stamp(pose, out, CataclysmSeals.CALENDAR, 0, .24f, 0, 0, age * .065f,
                    FIELD_RADIUS * grow, RUBY, visible);
            stamp(pose, out, CataclysmSeals.ASTROLABE, 0, 1, 0, 0, -age * .041f,
                    FIELD_RADIUS * grow, FIRE[1], visible);
            stamp(pose, out, CROWN, 0, 3, 0, 0, age * .09f, 186 * grow, RUBY, visible);
            sites(pose, out, age, t, visible, false);
            sites(pose, out, age, t, visible, true);
            for (int i = 0; i < GATES; i++) {
                float open = smooth((t - 212 - i * 31) / 36);
                float radius = gateRadius(i) * open;
                float y = gateY(i);
                float spin = (i % 2 == 0 ? 1 : -1) * (age * .31f + 15 * i);
                stamp(pose, out, CataclysmSeals.HOUSES[i % 6], 0, y, 0, 0, spin,
                        radius, FIRE[i % 6], visible);
                stamp(pose, out, CROWN, 0, y + 1.2f, 0, 0, -spin, radius * .74f, FIRE[(i + 1) % 6], visible);
                float link = smooth((t - 452 - i * 4) / 90);
                if (link > 0) stream(pose.last().pose(), out, radius, y, 0, age, i, link, visible, FIRE[i % 6]);
            }
        }
        if (field.released()) {
            float fall = Mth.clamp(field.phaseAge(partial) / CataclysmField.FALL_TICKS, 0, 1);
            float foot = field.impacted() ? 0 : Math.max(.8f, TOP * (1 - fall * fall));
            float beam = 1 - smooth((burn - 5) / 27);
            if (beam > 0) {
                Matrix4f m = pose.last().pose();
                shaft(m, out, TOP + 3, foot, 19 * beam, 0xFF692F, (int)(155 * beam));
                shaft(m, out, TOP + 3, foot, 11 * beam, 0xFFCB66, (int)(220 * beam));
                shaft(m, out, TOP + 3, foot, 5 * beam, 0xFFFFDF, (int)(255 * beam));
                for (int i = 0; i < GATES; i++) {
                    float y = gateY(i);
                    if (foot <= y) stamp(pose, out, WAVE, 0, y + .5f, 0, 0, 0, gateRadius(i), IVORY, beam);
                }
            }
            if (field.impacted()) aftershock(pose, out, age, burn);
        }
    }

    private static float gateY(int i) { return TOP - i * GATE_SPACING; }
    private static float gateRadius(int i) { return 96 - i * 4; }

    private static void sites(PoseStack pose, VertexConsumer out, float age, float t, float alpha, boolean sky) {
        int count = sky ? SKY_HOUSES : HOUSES;
        for (int i = 0; i < count; i++) {
            float open = smooth((t - (sky ? 126 : 62) - i * (sky ? 7 : 6)) / 50);
            if (open <= 0) continue;
            float angle = Mth.TWO_PI * i / count + (sky ? .075f : 0);
            float orbit = sky ? 286 + (i % 3) * 52 : 405;
            float x = orbit * Mth.cos(angle), z = orbit * Mth.sin(angle);
            float y = sky ? 92 + (i % 3) * 54 : 2;
            float tilt = sky ? 24 + (i % 3) * 18 : 0;
            float r = sky ? 41 + (i % 3) * 12 : 64;
            int color = FIRE[i % FIRE.length];
            stamp(pose, out, CataclysmSeals.HOUSES[i % 6], x, y, z, tilt,
                    (i % 2 == 0 ? 1 : -1) * age * .12f + i * 25, r * open, color, alpha);
            float link = smooth((t - 335 - i * 4) / 120);
            if (link > 0) stream(pose.last().pose(), out, x, y + 1, z, age, i, link, alpha, color);
        }
    }

    // The ribbon and its four bright comets use exactly the same curved path into the firing lens.
    private static void stream(Matrix4f m, VertexConsumer out, float x, float y, float z, float age,
                               int index, float progress, float visible, int color) {
        float prevX = x, prevY = y, prevZ = z;
        for (int n = 1; n <= 14; n++) {
            float u = progress * n / 14, v = 1 - u;
            float curve = Mth.sin(u * Mth.PI) * .18f * (index % 2 == 0 ? 1 : -1);
            float nx = v * (x * Mth.cos(curve) - z * Mth.sin(curve));
            float nz = v * (x * Mth.sin(curve) + z * Mth.cos(curve));
            float ny = Mth.lerp(u, y, TOP) + 25 * Mth.sin(u * Mth.PI);
            ribbon(m, out, prevX, prevY, prevZ, nx, ny, nz, .46f, color, (int)(visible * 178));
            prevX = nx; prevY = ny; prevZ = nz;
        }
        for (int n = 0; n < 4; n++) {
            float u = (age * .008f + index * .137f + n * .25f) % 1;
            if (u > progress) continue;
            float v = 1 - u;
            float curve = Mth.sin(u * Mth.PI) * .18f * (index % 2 == 0 ? 1 : -1);
            jewel(m,out,v * (x * Mth.cos(curve) - z * Mth.sin(curve)),
                    Mth.lerp(u,y,TOP) + 25 * Mth.sin(u * Mth.PI),
                    v * (x * Mth.sin(curve) + z * Mth.cos(curve)),1.9f,IVORY,(int)(visible * 248));
        }
    }

    private static void aftershock(PoseStack pose, VertexConsumer out, float age, float burn) {
        float p = smooth(burn / CataclysmField.AFTERGLOW_TICKS);
        for (int i = 0; i < 3; i++) {
            float ring = smooth((burn - i * 4) / 24);
            if (ring > 0 && ring < 1)
                stamp(pose, out, WAVE, 0, .8f + i * .6f, 0, 0, age,
                        CataclysmField.IMPACT_RADIUS * ring, i == 0 ? IVORY : FIRE[i], 1 - ring * ring);
        }
        for (int n = 0; n < 64; n++) {
            float a = n * 2.39996f, radius = p * (34 + n % 44);
            float y = 1 + Mth.sin(p * Mth.PI) * (9 + n % 35);
            jewel(pose.last().pose(), out, radius * Mth.cos(a), y, radius * Mth.sin(a),
                    (1 - p) * 2.1f, FIRE[n % 6], (int)(248 * (1 - p)));
        }
    }

    private static void shaft(Matrix4f m, VertexConsumer out, float top, float bottom, float radius, int rgb, int alpha) {
        for (int n = 0; n < 28; n++) {
            float a = n * Mth.TWO_PI / 28, b = (n + 1) * Mth.TWO_PI / 28;
            float x = radius * Mth.cos(a), z = radius * Mth.sin(a);
            float u = radius * Mth.cos(b), v = radius * Mth.sin(b);
            vertex(m, out, x, top, z, rgb, alpha);
            vertex(m, out, u, top, v, rgb, alpha);
            vertex(m, out, u * .8f, bottom, v * .8f, rgb, alpha);
            vertex(m, out, x * .8f, bottom, z * .8f, rgb, alpha);
        }
    }

    private static void stamp(PoseStack pose, VertexConsumer out, Seal seal, float x, float y, float z,
                              float tilt, float spin, float r, int color, float alpha) {
        if (r < .01f) return;
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(Axis.XP.rotationDegrees(tilt));
        pose.mulPose(Axis.YP.rotationDegrees(spin % 360));
        pose.scale(r, r, r);
        seal.draw(pose.last().pose(), out, color, (int)(alpha * 255));
        pose.popPose();
    }

    private static void ribbon(Matrix4f m, VertexConsumer out, float x,float y,float z,
                               float u,float v,float w, float width, int color, int alpha) {
        float dx = u - x, dz = w - z;
        float d = Mth.sqrt(dx * dx + dz * dz);
        float nx = d < .0001f ? width : dz * width / d, nz = d < .0001f ? 0 : -dx * width / d;
        vertex(m,out,x + nx,y,z + nz,color,alpha);
        vertex(m,out,u + nx,v,w + nz,color,alpha);
        vertex(m,out,u - nx,v,w - nz,color,alpha);
        vertex(m,out,x - nx,y,z - nz,color,alpha);
    }

    private static void jewel(Matrix4f m, VertexConsumer out, float x, float y, float z, float r, int color, int alpha) {
        for (int i = 0; i < 3; i++) {
            float dx = i == 0 ? 0 : r, dz = i == 0 ? r : 0;
            vertex(m,out,x + dx,y,z + dz,color,alpha);
            vertex(m,out,x,y + (i == 2 ? 0 : r * 2),z + (i == 2 ? r : 0),IVORY,alpha);
            vertex(m,out,x - dx,y,z - dz,color,alpha);
            vertex(m,out,x,y - (i == 2 ? 0 : r * 2),z - (i == 2 ? r : 0),color,alpha);
        }
    }

    private static float smooth(float x) {
        float t = Mth.clamp(x, 0, 1);
        return t * t * (3 - 2 * t);
    }
}
