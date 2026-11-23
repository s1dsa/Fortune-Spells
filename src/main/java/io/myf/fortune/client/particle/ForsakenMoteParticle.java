package io.myf.fortune.client.particle;

import io.myf.fortune.client.fx.FortunePalette;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** 灰紫下坠微尘（fortune_spells:forsaken_mote）。贴图灰度，默认染成背弃灰紫，缓慢飘落、逐渐变小。 */
public class ForsakenMoteParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float baseSize;

    protected ForsakenMoteParticle(ClientLevel level, double x, double y, double z,
                                   double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);
        this.sprites = sprites;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.friction = 0.92f;
        this.gravity = 0.04f;
        this.hasPhysics = false;
        this.lifetime = 30 + random.nextInt(11);
        this.baseSize = 0.08f + random.nextFloat() * 0.04f;
        this.quadSize = baseSize;
        setColor(FortunePalette.r(FortunePalette.FORSAKEN_LIGHT), FortunePalette.g(FortunePalette.FORSAKEN_LIGHT),
                FortunePalette.b(FortunePalette.FORSAKEN_LIGHT));
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed) {
            setSpriteFromAge(sprites);
            float life = (float) age / lifetime;
            quadSize = baseSize * (1f - 0.5f * life);
            alpha = life > 0.6f ? 1f - (life - 0.6f) / 0.4f : 1f;
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        int base = super.getLightColor(partialTick);
        // 至少保持一半亮度，暗处也看得见
        int block = Math.max(base & 0xFF, 0x80);
        return (base & 0xFF0000) | block;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new ForsakenMoteParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
