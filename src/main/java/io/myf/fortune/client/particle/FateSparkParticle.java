package io.myf.fortune.client.particle;

import io.myf.fortune.client.fx.FortunePalette;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * 金色闪光（fortune_spells:fate_spark）。贴图是白/浅灰，默认染成牌桌金；
 * 客户端特效代码可以拿到实例后 setColor 改成红色（孤注一掷）。满亮度、无重力。
 */
public class FateSparkParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected FateSparkParticle(ClientLevel level, double x, double y, double z,
                                double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);
        this.sprites = sprites;
        // 父类构造器会给速度加随机抖动，这里改回调用方给的精确速度
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.friction = 0.86f;
        this.gravity = 0f;
        this.hasPhysics = false;
        this.lifetime = 10 + random.nextInt(7);
        this.quadSize = 0.09f + random.nextFloat() * 0.04f;
        setColor(FortunePalette.r(FortunePalette.GOLD), FortunePalette.g(FortunePalette.GOLD), FortunePalette.b(FortunePalette.GOLD));
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed) {
            setSpriteFromAge(sprites);
            float life = (float) age / lifetime;
            alpha = life > 0.7f ? 1f - (life - 0.7f) / 0.3f : 1f;
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new FateSparkParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
