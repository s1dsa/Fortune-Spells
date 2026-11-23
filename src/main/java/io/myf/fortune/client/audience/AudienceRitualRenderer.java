package io.myf.fortune.client.audience;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.myf.fortune.expansion.audience.AudienceRitual;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import static io.myf.fortune.client.audience.AudienceGlyphs.*;

/** A throne room of open light above the thirteen physical offering positions. */
public final class AudienceRitualRenderer extends EntityRenderer<AudienceRitual> {
    private static final int CENTER = 12;
    private final ItemRenderer items;

    public AudienceRitualRenderer(EntityRendererProvider.Context context) {
        super(context);
        items = context.getItemRenderer();
    }

    @Override public ResourceLocation getTextureLocation(AudienceRitual ritual) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    @Override public boolean shouldRender(AudienceRitual ritual, Frustum view, double x, double y, double z) {
        // The camera may see the crown or the outer shockwave with the origin entirely off-screen.
        AABB court = ritual.getBoundingBox().inflate(69, 0, 69).expandTowards(0, 69, 0).expandTowards(0, -8, 0);
        return view.isVisible(court);
    }

    @Override public void render(AudienceRitual ritual, float yaw, float partial, PoseStack pose,
                                 MultiBufferSource buffers, int light) {
        float age = ritual.age(partial);
        float stageAge = ritual.stageAge(partial);
        int stage = ritual.stage();
        float charge = stage == 1 ? ease(stageAge / AudienceRitual.ACTIVATION_TICKS) : stage == 2 ? 1 : 0;
        float finale = stage == 2 ? Mth.clamp(stageAge / AudienceRitual.FINALE_TICKS, 0, 1) : 0;
        float visibility = 1 - ease((finale - .26f) / .70f);
        VertexConsumer lines = buffers.getBuffer(RenderType.debugQuads());

        float progress = ritual.drawingProgress(partial);
        court(pose, lines, age, progress, charge, finale, visibility);
        for (int i = 0; i <= CENTER; i++) {
            Vec3 slot = AudienceRitual.slotOffset(i);
            float x = (float)slot.x, z = (float)slot.z;
            float ground = (float)ritual.slotY(i);
            int hue = JEWELS[(i < 8 ? i : (i - 8) * 2 + 1) & 7];
            boolean filled = stage > 0 || !ritual.offering(i).isEmpty();
            float open = ease((progress - .10f - i * .028f) / .40f);
            if (open > 0 && visibility > 0) {
                float radius = (i < 8 ? 2.35f : i < CENTER ? 3.45f : 4.9f) * open;
                float pulse = .5f + .5f * Mth.sin(age * .095f + i);
                float alpha = visibility * (filled ? .90f + .10f * pulse : .69f + .13f * pulse);
                float spin = (i % 2 == 0 ? 1 : -1) * (age * .16f + 90 * charge);
                stamp(pose, lines, STATION, x, ground + .12f, z, 0, 0, spin, radius,
                        i == CENTER ? GOLD : hue, alpha, open);
                if (i == CENTER || filled) {
                    stamp(pose, lines, SIGNET, x, ground + .17f, z, 0, 0, -spin,
                            radius * (i == CENTER ? 1.34f : 1.15f), hue, alpha, open);
                    // The second ring floats above the vessel to make occupied sites visible from the edge.
                    stamp(pose, lines, WAVE, x, ground + .60f + pulse * .10f, z, 0, 0, 0,
                            radius * .48f, hue, alpha * .6f, open);
                }
            }
            if (stage == 1 && i < CENTER)
                convergence(pose.last().pose(), lines, x, ground + .75f, z, stageAge, i, hue);
        }
        if (stage == 2) coronation(pose, lines, finale, stageAge);

        // The item renderer can switch buffer types. Submit every glyph before rendering any item.
        if (stage >= 0 && stage < 2) renderOfferings(ritual, pose, buffers, age, charge);
    }

    private static void court(PoseStack pose, VertexConsumer out, float t, float progress, float charge, float finale, float alpha) {
        if (alpha <= 0) return;
        float ground = ease(progress / .52f);
        float spread = .35f + .65f * ground;
        float spin = t * .12f + 65 * charge;

        stamp(pose, out, COURT, 0, .16f, 0, 0, 0, spin, 48 * spread, RUBY, alpha * .97f, ground);
        stamp(pose, out, CROWN, 0, .23f, 0, 0, 0, -spin, 30.5f * spread, RUBY, alpha * .85f, ground);
        // A moving calendar at floor level and a counter-rotating rose under the court.
        // Where there is open space below the altar its suspended underside is visible too.
        stamp(pose, out, ASTROLABE, 0, .29f, 0, 0, 0, -spin * .32f, 44 * spread, 0xDD687C, alpha, ground);
        stamp(pose, out, ROSETTE, 0, .35f, 0, 0, 0, spin * .28f, 18 * spread, RUBY, alpha, ground);
        stamp(pose, out, COURT, 0, -2.3f, 0, 0, 0, spin, 40 * spread, RUBY, alpha * .8f, ground);
        stamp(pose, out, ASTROLABE, 0, -5.6f, 0, 0, 0, -spin, 27 * spread, 0x969CFF, alpha, ground);
        for (int i = 0; i < 8; i++) {
            float lift = ease((progress - .20f - i * .032f) / .35f);
            if (lift == 0) continue;
            float angle = i * Mth.PI / 4;
            float x = 36 * Mth.cos(angle), z = 36 * Mth.sin(angle);
            float height = (i % 2 == 0 ? 11 : 16) * lift;
            float pulse = .82f + .18f * Mth.cos(t * .105f - i);
            stamp(pose, out, ROSETTE, x, .31f, z, 0, 0, spin + i * 45,
                    4.6f * lift, JEWELS[i], alpha * .9f, lift);
            stamp(pose, out, GATE, x, height, z, 90 - 45 * i, 84, 0,
                    (6.2f + .8f * charge) * lift, JEWELS[i], alpha * pulse, lift);
        }
        for (int i = 0; i < 3; i++) {
            float raised = ease((progress - .39f - i * .105f) / .40f);
            if (raised == 0) continue;
            float collapse = 1 - ease(finale / .64f);
            float y = 4 + (23 + 13 * i) * raised * collapse;
            float radius = (38 - i * 9) * raised * (.55f + .45f * collapse);
            float direction = i == 1 ? -1 : 1;
            int hue = i == 1 ? 0xDF4E78 : i == 2 ? 0xA188FF : RUBY;
            stamp(pose, out, i == 1 ? COURT : CROWN, 0, y, 0, i * 36, 4 * direction,
                    direction * (spin * 1.6f + i * 30), radius, hue, alpha * (.90f + .10f * Mth.sin(t * .08f + i)), raised);
            if (i == 2)
                stamp(pose, out, SIGNET, 0, y + 1.5f, 0, 0, 0, -spin, radius * .70f, RUBY, alpha, raised);
            stamp(pose, out, ASTROLABE, 0, y + 2.2f, 0, 30 * i, 11 * direction,
                    -spin * direction, radius * .88f, JEWELS[i * 2], alpha * .9f, raised);
            stamp(pose, out, ROSETTE, 0, y + 4.4f, 0, 0, 0, spin * direction,
                    radius * .54f, hue, alpha, raised);
        }
    }

    private void renderOfferings(AudienceRitual ritual, PoseStack pose, MultiBufferSource buffers,
                                 float age, float charge) {
        for (int i = 0; i <= CENTER; i++) {
            ItemStack offering = ritual.offering(i);
            if (offering.isEmpty()) continue;
            Vec3 slot = AudienceRitual.slotOffset(i);
            pose.pushPose();
            float lift = ease(ritual.offeringAge(i, age) / 28f);
            float hover = 1.65f + .36f * Mth.sin(age * .046f + i);
            pose.translate(slot.x, ritual.slotY(i) + .24f + lift * hover + charge * .75f, slot.z);
            pose.mulPose(Axis.YP.rotationDegrees((age * .78f + 27 * i) % 360));
            float size = i == CENTER ? 1.8f : 1.25f;
            pose.scale(size, size, size);
            items.renderStatic(offering, ItemDisplayContext.GROUND, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, pose, buffers, ritual.level(), ritual.getId() + i);
            pose.popPose();
        }
    }

    /** Each station sends an arcing ribbon to the same point. A moving glint marks the travelling pulse. */
    private static void convergence(Matrix4f m, VertexConsumer out, float x, float y, float z,
                                    float time, int station, int hue) {
        float head = Mth.clamp((time - station * .8f) / 23, 0, 1);
        if (head <= 0) return;
        float pulse = (time * .042f + station * .17f) % 1;
        float bend = station % 2 == 0 ? 1 : -1;
        int steps = 16;
        float px = x, py = y, pz = z;
        for (int i = 1; i <= steps; i++) {
            float u = head * i / steps;
            float turn = Mth.sin(u * Mth.PI) * .37f * bend;
            float scale = 1 - u;
            float nx = (x * Mth.cos(turn) - z * Mth.sin(turn)) * scale;
            float nz = (x * Mth.sin(turn) + z * Mth.cos(turn)) * scale;
            float ny = Mth.lerp(u, y, 2.6f) + 4.6f * Mth.sin(u * Mth.PI);
            float distance = Math.abs(u - pulse);
            line(m, out, px, py, pz, nx, ny, nz, distance < .12f ? .20f : .081f,
                    distance < .12f ? IVORY : hue, distance < .12f ? 251 : 195);
            if (distance < .035f) spark(m, out, nx, ny, nz, .23f, hue, 240);
            px = nx; py = ny; pz = nz;
        }
    }

    /** The gold core rises, splits into circling embers, then sends two delayed rings over the floor. */
    private static void coronation(PoseStack pose, VertexConsumer out, float t, float ticks) {
        float fade = 1 - ease((t - .55f) / .45f);
        float height = 59 * ease(t / .16f);
        Matrix4f matrix = pose.last().pose();
        int alpha = (int)(245 * fade);
        if (height > .05f && alpha != 0) {
            for (int i = 0; i < 8; i++) {
                float a = i * Mth.PI / 4 + t * .8f;
                float cs = Mth.cos(a), sn = Mth.sin(a);
                float r = 1.1f + 2.2f * Mth.sin(t * Mth.PI);
                line(matrix, out, cs * r, .2f, sn * r, cs * .18f, height, sn * .18f,
                        .27f, (i & 1) == 0 ? GOLD : RUBY, alpha);
                for (int j = 0; j < 6; j++) {
                    float life = (t + j / 6f) % 1;
                    float phase = a + 2.7f * life + .042f * ticks;
                    float radius = 2 + 29 * life;
                    float y = 3 + height * .41f * (1 - life) + 4 * Mth.sin(Mth.PI * life);
                    spark(matrix, out, radius * Mth.cos(phase), y, radius * Mth.sin(phase),
                            .18f + .45f * (1 - life), JEWELS[i], alpha);
                }
            }
            stamp(pose, out, CROWN, 0, height * .65f, 0, 0, 0, -ticks * 3.8f, 2 + 12 * t, GOLD, fade);
        }
        for (int i = 0; i < 2; i++) {
            float wave = Mth.clamp((t - i * .17f) / .72f, 0, 1);
            if (wave <= 0 || wave >= 1) continue;
            float radius = 3 + 61 * (1 - (1 - wave) * (1 - wave));
            stamp(pose, out, WAVE, 0, .49f + i * .25f, 0, 0, 0, 0, radius,
                    i == 0 ? GOLD : RUBY, 1 - ease((wave - .45f) / .55f));
        }
    }

    private static void stamp(PoseStack pose, VertexConsumer out, Seal seal, float x, float y, float z,
                              float yaw, float tilt, float spin, float radius, int hue, float opacity) {
        stamp(pose, out, seal, x, y, z, yaw, tilt, spin, radius, hue, opacity, 1);
    }

    static void stamp(PoseStack pose, VertexConsumer out, Seal seal, float x, float y, float z,
                      float yaw, float tilt, float spin, float radius, int hue, float opacity, float progress) {
        if (radius < .01f || opacity <= 0) return;
        pose.pushPose();
        pose.translate(x, y, z);
        if (yaw != 0) pose.mulPose(Axis.YP.rotationDegrees(yaw));
        if (tilt != 0) pose.mulPose(Axis.XP.rotationDegrees(tilt));
        if (spin != 0) pose.mulPose(Axis.YP.rotationDegrees(spin % 360));
        pose.scale(radius, radius, radius);
        seal.draw(pose.last().pose(), out, hue, (int)(255 * opacity), progress);
        pose.popPose();
    }

    private static float ease(float value) {
        float t = Mth.clamp(value, 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static void spark(Matrix4f m, VertexConsumer out, float x, float y, float z,
                              float size, int color, int alpha) {
        for (int axis = 0; axis < 2; axis++) {
            float dx = axis == 0 ? size * .5f : 0;
            float dz = axis == 1 ? size * .5f : 0;
            vertex(m, out, x, y - size, z, color, 0);
            vertex(m, out, x + dx, y, z + dz, color, alpha);
            vertex(m, out, x, y + size, z, IVORY, alpha);
            vertex(m, out, x - dx, y, z - dz, color, alpha);
        }
    }

    private static void line(Matrix4f m, VertexConsumer out, float x, float y, float z,
                             float u, float v, float w, float width, int color, int alpha) {
        float dx = u - x, dz = w - z;
        float distance = Mth.sqrt(dx * dx + dz * dz);
        float nx = distance < .001f ? width * .5f : dz * width * .5f / distance;
        float nz = distance < .001f ? 0 : -dx * width * .5f / distance;
        vertex(m, out, x + nx, y, z + nz, color, alpha);
        vertex(m, out, u + nx, v, w + nz, color, alpha);
        vertex(m, out, u - nx, v, w - nz, color, alpha);
        vertex(m, out, x - nx, y, z - nz, color, alpha);
    }
}
