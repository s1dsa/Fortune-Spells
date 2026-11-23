package io.myf.fortune.client.movement;

import com.google.common.collect.MapMaker;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.myf.fortune.expansion.movement.MovementSpells;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderPlayerEvent;
import org.joml.Vector3f;

import java.util.Map;

import static io.myf.fortune.client.movement.BoneWingRenderer.Joint.*;

public final class BoneWingRenderer {
    private static final RenderType BONE = RenderType.entityCutoutNoCull(
            new ResourceLocation("minecraft", "textures/block/bone_block_side.png"));
    // Only actual casts grow in. Someone entering tracking range sees an already deployed wing.
    // Entity.equals uses the network ID, which can be reused across worlds and tracking sessions.
    private static final Map<Player, Integer> GROWTH_START = new MapMaker().weakKeys().makeMap();
    private static final int SHATTERED = -1;

    // The leading edge bends twice, then four fingers fan back from the carpus.
    enum Joint {
        ROOT(null, .14f, .04f, .04f, 0, 1),
        ELBOW(ROOT, .47f, .24f, .25f, 0, 3),
        WRIST(ELBOW, .96f, .90f, .10f, 2, 4),
        KNUCKLE(WRIST, 1.51f, .76f, .07f, 5, 4),
        TIP(KNUCKLE, 1.99f, .43f, .12f, 8, 4),
        OUTER(WRIST, 1.54f, .44f, .34f, 5, 4),
        OUTER_TIP(OUTER, 1.76f, -.02f, .49f, 8, 4),
        MIDDLE(WRIST, 1.19f, .37f, .42f, 5, 4),
        MIDDLE_TIP(MIDDLE, 1.25f, -.37f, .67f, 8, 4),
        INNER(WRIST, .84f, .28f, .36f, 5, 4),
        INNER_TIP(INNER, .66f, -.48f, .48f, 8, 4),
        HEEL(ROOT, .17f, -.27f, .19f, 3, 5),
        CLAW(WRIST, 1.01f, 1.15f, .04f, 6, 4);

        final Joint parent;
        final float x, y, z, start, duration;
        final Vector3f pos = new Vector3f();

        Joint(Joint parent, float x, float y, float z, float start, float duration) {
            this.parent = parent;
            this.x = x;
            this.y = y;
            this.z = z;
            this.start = start;
            this.duration = duration;
        }

        void grow(float age) {
            if (parent == null) {
                pos.set(x, y, z);
                return;
            }
            float fraction = Mth.clamp((age - start) / duration, 0, 1);
            fraction *= fraction * (3 - 2 * fraction);
            pos.set(parent.pos).add((x - parent.x) * fraction,
                    (y - parent.y) * fraction, (z - parent.z) * fraction);
        }
    }

    private static final Joint[] JOINTS = Joint.values();
    private static final int[] SHADE = {0xf7edd6, 0xc9bfa8, 0x898e80, 0xddd9bc};
    private static final float[] AROUND = {1, 0, -1, 0, 1};
    private static final float[] DEPTH = {0, 1, 0, -1, 0};

    private record Bone(Joint start, Joint end, float base, float width) {
        private static final Vector3f SHAFT = new Vector3f();

        void draw(VertexConsumer out, PoseStack.Pose pose, int light) {
            Vector3f a = start.pos;
            Vector3f b = end.pos;
            float dx = b.x - a.x;
            float dy = b.y - a.y;
            float length = Mth.sqrt(dx * dx + dy * dy);
            if (length < .002f) return;
            float nx = -dy / length;
            float ny = dx / length;
            float rootWidth = Math.min(base, length * .35f);
            float tipWidth = Math.min(width, length * .27f);
            // A narrow shaft and broad knuckles give each exposed segment a bone silhouette.
            a.lerp(b, .58f, SHAFT);
            float shaftWidth = .72f * Mth.lerp(.58f, rootWidth, tipWidth);
            section(out, pose, a, rootWidth, 0, SHAFT, shaftWidth, .58f, nx, ny, light);
            section(out, pose, SHAFT, shaftWidth, .58f, b, tipWidth, 1, nx, ny, light);
            for (int i = 0; i < 4; i++) edge(out, pose, a, rootWidth, nx, ny, 3 - i, 0, light, SHADE[1]);
            for (int i = 0; i < 4; i++) edge(out, pose, b, tipWidth, nx, ny, i, 1, light, SHADE[0]);
        }

        private static void section(VertexConsumer out, PoseStack.Pose pose, Vector3f a, float waistA, float uA,
                                    Vector3f b, float waistB, float uB, float nx, float ny, int light) {
            for (int i = 0; i < 4; i++) {
                edge(out, pose, a, waistA, nx, ny, i, uA, light, SHADE[i]);
                edge(out, pose, b, waistB, nx, ny, i, uB, light, SHADE[i]);
                edge(out, pose, b, waistB, nx, ny, i + 1, uB, light, SHADE[i]);
                edge(out, pose, a, waistA, nx, ny, i + 1, uA, light, SHADE[i]);
            }
        }

        private static void edge(VertexConsumer out, PoseStack.Pose pose, Vector3f p, float width,
                                 float nx, float ny, int face, float u, int light, int rgb) {
            float cross = width * AROUND[face];
            out.vertex(pose.pose(), p.x + cross * nx, p.y + cross * ny, p.z + width * DEPTH[face])
                    .color(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, 255).uv(u, face * .25f)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                    .normal(pose.normal(), 0, .25f, .97f).endVertex();
        }
    }

    private static final Bone[] BONES = {
            new Bone(ROOT, ELBOW, .10f, .085f), new Bone(ELBOW, WRIST, .095f, .09f),
            new Bone(WRIST, CLAW, .067f, .004f), new Bone(WRIST, KNUCKLE, .085f, .06f),
            new Bone(KNUCKLE, TIP, .069f, .006f), new Bone(WRIST, OUTER, .067f, .052f),
            new Bone(OUTER, OUTER_TIP, .057f, .005f), new Bone(WRIST, MIDDLE, .062f, .05f),
            new Bone(MIDDLE, MIDDLE_TIP, .055f, .005f), new Bone(WRIST, INNER, .06f, .046f),
            new Bone(INNER, INNER_TIP, .05f, .005f), new Bone(ROOT, HEEL, .062f, .016f)
    };

    private BoneWingRenderer() {}

    public static void unfold(Player player) {
        GROWTH_START.put(player, player.tickCount);
    }

    public static void shatter(Player player) {
        GROWTH_START.put(player, SHATTERED);
        if (!(player.level() instanceof ClientLevel level) || player.isInvisible()) return;
        BoneShardParticle.scatter(level, player);
    }

    static void draw(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        Integer start = GROWTH_START.get(player);
        if (player.isInvisible() || (start != null && start == SHATTERED)) return;
        float partial = event.getPartialTick();
        float age = start == null ? 12 : Math.min(12, player.tickCount - start + partial);
        // The cast packet can arrive before the synchronized attribute. Late trackers get the
        // attribute during pairing and start with a full wing. Shattering hides both wings immediately.
        if (age >= 2 && MovementSpells.boneHealth(player) <= 0) return;
        for (Joint joint : JOINTS) joint.grow(age);
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        float yaw = Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot);
        pose.mulPose(Axis.YP.rotationDegrees(180 - yaw));
        pose.translate(0, player.isCrouching() ? 1.04f : 1.29f, .17f);
        float beat = Mth.sin((player.tickCount + partial) * (player.onGround() ? .10f : .24f));
        float sweep = player.onGround() ? 21 + beat * 5 : 12 + beat * 13;
        float lift = player.onGround() ? beat * 3 : beat * 12;
        int light = event.getPackedLight();
        light = LightTexture.pack(Math.max(10, LightTexture.block(light)), LightTexture.sky(light));
        VertexConsumer out = event.getMultiBufferSource().getBuffer(BONE);
        for (int side = -1; side <= 1; side += 2) {
            pose.pushPose();
            pose.scale(side, 1, 1);
            pose.mulPose(Axis.YP.rotationDegrees(-sweep));
            pose.mulPose(Axis.ZP.rotationDegrees(lift));
            for (Bone bone : BONES) bone.draw(out, pose.last(), light);
            pose.popPose();
        }
        pose.popPose();
    }
}
