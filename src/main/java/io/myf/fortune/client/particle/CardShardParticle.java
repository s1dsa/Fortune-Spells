package io.myf.fortune.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * 纸片 / 筹码碎屑（fortune_spells:card_shard）。4 种贴图随机选一种，自带颜色不染色；
 * 受重力下落、边落边转。
 */
public class CardShardParticle extends TextureSheetParticle {
    private final float spin;

    protected CardShardParticle(ClientLevel level, double x, double y, double z,
                                double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.friction = 0.96f;
        this.gravity = 0.6f;
        this.hasPhysics = true;
        this.lifetime = 20 + random.nextInt(11);
        this.quadSize = 0.06f + random.nextFloat() * 0.03f;
        this.spin = (random.nextFloat() - 0.5f) * 0.5f;
        this.roll = random.nextFloat() * 6.2831855f;
        this.oRoll = roll;
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        oRoll = roll;
        super.tick();
        if (!removed) {
            if (onGround) {
                xd *= 0.7;
                zd *= 0.7;
            } else {
                roll += spin;
            }
            float life = (float) age / lifetime;
            alpha = life > 0.75f ? 1f - (life - 0.75f) / 0.25f : 1f;
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new CardShardParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
