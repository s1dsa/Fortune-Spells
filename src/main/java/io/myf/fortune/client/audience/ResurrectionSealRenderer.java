package io.myf.fortune.client.audience;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import io.myf.fortune.expansion.audience.ResurrectionSeal;

import static io.myf.fortune.client.audience.AudienceGlyphs.*;
import static io.myf.fortune.client.audience.AudienceRitualRenderer.stamp;

public final class ResurrectionSealRenderer extends EntityRenderer<ResurrectionSeal> {
    public ResurrectionSealRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override public void render(ResurrectionSeal seal, float yaw, float partial, PoseStack pose,
                                 MultiBufferSource buffers, int light) {
        float age = seal.age(partial);
        float t = Mth.clamp(age / seal.duration(), 0, 1);
        var lines = buffers.getBuffer(RenderType.debugQuads());
        float show = seal.rebirth() ? 1 : smooth(t * 2.7f);
        float fade = 1 - smooth((t - .76f) / .24f);
        float span = seal.rebirth() ? 4 + 5 * smooth(t) : 2 + 4 * show;
        stamp(pose, lines, ASTROLABE, 0,.04f,0,0,0,age*.72f,span,0xC4CCD6,fade,show);
        stamp(pose, lines, SIGNET, 0,.08f,0,0,0,-age,span*.64f,IVORY,fade,show);
        for (int i = 0; i < 3; i++) {
            float reveal = seal.rebirth() ? 1 : smooth((t - .13f*i) * 2.6f);
            float h = seal.rebirth() ? (1 + i) * (1 + t) : (i + 1) * 1.18f * reveal;
            stamp(pose, lines, CROWN, 0,h,0,0,0,(i%2==0 ? age : -age)*1.9f,
                    span * (.71f - i*.12f),0xBEC5D3,fade,reveal);
        }
    }

    private static float smooth(float t) {
        float f = Mth.clamp(t,0,1);
        return f*f*(3-2*f);
    }
    @Override public ResourceLocation getTextureLocation(ResurrectionSeal seal) { return TextureAtlas.LOCATION_BLOCKS; }
}
