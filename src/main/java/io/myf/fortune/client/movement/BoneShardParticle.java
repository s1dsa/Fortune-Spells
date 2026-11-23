package io.myf.fortune.client.movement;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

final class BoneShardParticle extends TextureSheetParticle {
    private static final ResourceLocation GRAIN = new ResourceLocation("minecraft", "block/bone_block_side");
    private final Vector3f along = new Vector3f();
    private final Vector3f across = new Vector3f();
    private final float spin;

    private BoneShardParticle(ClientLevel level, double x, double y, double z,
                              double vx, double vy, double vz) {
        super(level, x, y, z);
        xd = vx;
        yd = vy;
        zd = vz;
        lifetime = 20 + random.nextInt(10);
        gravity = 1.08f;
        friction = .96f;
        quadSize = .14f + random.nextFloat() * .13f;
        spin = (random.nextFloat() - .5f) * .75f;
        oRoll = roll = random.nextFloat() * Mth.TWO_PI;
        setSize(.07f, .07f);
        setSprite(Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(GRAIN));
    }

    static void scatter(ClientLevel level, Player player) {
        Vec3 back = Vec3.directionFromRotation(0, player.yBodyRot + 180);
        Vec3 right = new Vec3(-back.z, 0, back.x);
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < 12; i++) {
                double span = (i + level.random.nextDouble()) / 12;
                double offset = side * (.20 + 1.50 * span);
                double sweep = .23 + .50 * span;
                double x = player.getX() + right.x * offset + back.x * sweep;
                double z = player.getZ() + right.z * offset + back.z * sweep;
                double y = player.getY() + 1.24 + .55 * Math.sin(span * Math.PI)
                        + (level.random.nextDouble() - .5) * .54;
                double speed = side * (.045 + .19 * level.random.nextDouble());
                double vx = speed * right.x + (level.random.nextDouble() - .5) * .16;
                double vz = speed * right.z + (level.random.nextDouble() - .5) * .16;
                Minecraft.getInstance().particleEngine.add(new BoneShardParticle(level, x, y, z, vx,
                        .06 + level.random.nextDouble() * .22, vz));
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        oRoll = roll;
        if (!onGround) roll += spin;
        alpha = Math.min(1, (lifetime - age) / 7f);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.TERRAIN_SHEET;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partial) {
        Vec3 eye = camera.getPosition();
        float x = (float)(Mth.lerp(partial, xo, this.x) - eye.x);
        float y = (float)(Mth.lerp(partial, yo, this.y) - eye.y);
        float z = (float)(Mth.lerp(partial, zo, this.z) - eye.z);
        float angle = Mth.lerp(partial, oRoll, roll);
        float dx = Mth.sin(angle);
        float dy = Mth.cos(angle);
        along.set(dx, dy, 0).rotate(camera.rotation()).mul(quadSize);
        across.set(dy, -dx, 0).rotate(camera.rotation()).mul(.55f * quadSize);
        int light = getLightColor(partial);
        light = LightTexture.pack(Math.max(10, LightTexture.block(light)), LightTexture.sky(light));
        // Two convex quads share the ridge. Wind both clockwise in camera-local XY,
        // the same order as SingleQuadParticle, so the terrain pass draws both faces.
        point(buffer, x, y, z, -.22f, -1, 1, light);
        point(buffer, x, y, z, .22f, 1, 1, light);
        point(buffer, x, y, z, .84f, .56f, 1, light);
        point(buffer, x, y, z, .87f, -.10f, 1, light);
        point(buffer, x, y, z, -.22f, -1, .70f, light);
        point(buffer, x, y, z, -.79f, -.53f, .70f, light);
        point(buffer, x, y, z, -.88f, .20f, .70f, light);
        point(buffer, x, y, z, .22f, 1, .70f, light);
    }

    private void point(VertexConsumer out, float x, float y, float z, float wide, float length,
                       float shade, int light) {
        out.vertex(x + wide * across.x + length * along.x, y + wide * across.y + length * along.y,
                        z + wide * across.z + length * along.z)
                .uv(sprite.getU(8 + wide * 5), sprite.getV(8 + length * 7))
                .color(shade, shade, shade * .88f, alpha).uv2(light).endVertex();
    }
}
