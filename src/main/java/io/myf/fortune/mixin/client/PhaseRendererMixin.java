package io.myf.fortune.mixin.client;

import io.myf.fortune.expansion.movement.MovementSpells;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
abstract class PhaseRendererMixin {
    @Unique private boolean fortuneSpells$transparent;

    @SuppressWarnings("unchecked")
    @Inject(method = {"getRenderType", "m_7225_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void fortuneSpells$renderGhost(LivingEntity entity, boolean visible, boolean ghost, boolean glowing,
                                         CallbackInfoReturnable<RenderType> ci) {
        fortuneSpells$transparent = visible && entity.hasEffect(MovementSpells.PHASING.get());
        if (fortuneSpells$transparent) {
            var renderer = (LivingEntityRenderer<LivingEntity, ?>)(Object)this;
            ci.setReturnValue(RenderType.entityTranslucent(renderer.getTextureLocation(entity)));
        }
    }

    @Group(name = "fortuneSpellsPhaseAlpha", min = 1)
    @ModifyArg(method = {"render", "m_7392_"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"),
            index = 7, require = 0, remap = false)
    private float fortuneSpells$alphaDev(float normal) { return fortuneSpells$transparent ? .4f : normal; }

    @Group(name = "fortuneSpellsPhaseAlpha", min = 1)
    @ModifyArg(method = {"render", "m_7392_"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/model/EntityModel;m_7695_(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V"),
            index = 7, require = 0, remap = false)
    private float fortuneSpells$alphaGame(float normal) { return fortuneSpells$transparent ? .4f : normal; }
}
