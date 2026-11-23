package io.myf.fortune.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.myf.fortune.FortuneIds;
import io.myf.fortune.entity.FateCardEntity;
import java.util.Set;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

/**
 * 命运之牌模型：一个 28×40×1 像素的盒子，64×64 贴图。
 * 几何和 UV 布局与迎战女爵的卡牌相同（DESIGN 9.4），所以能直接贴迎战的牌面贴图。
 * 正面 UV 区域 x 1–28、y 1–40；背面 x 30–57、y 1–40。
 */
public class FateCardModel extends EntityModel<FateCardEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(FortuneIds.ENTITY_FATE_CARD, "main");

    /** 卡牌盒子在部件坐标里的尺寸（方块单位），给发光边框用。 */
    public static final float HALF_WIDTH = 14f / 16f;
    public static final float HEIGHT = 40f / 16f;
    public static final float HALF_DEPTH = 0.5f / 16f;

    private final ModelPart card;
    private final ModelPart frontOverlay;
    private final ModelPart backUvOverlay;

    public FateCardModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.card = root.getChild("card");
        this.frontOverlay = root.getChild("front_overlay");
        this.backUvOverlay = root.getChild("back_uv_overlay");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("card",
                CubeListBuilder.create().texOffs(0, 0).addBox(-14f, -40f, -0.5f, 28f, 40f, 1f),
                PartPose.offset(0f, 24f, 0f));
        // Both overlays face the player. The two UV origins match their respective texture layouts.
        addFaceOverlay(root, "front_overlay", 1);
        addFaceOverlay(root, "back_uv_overlay", 30);
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void addFaceOverlay(PartDefinition root, String name, int u) {
        root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, 1)
                .addBox(-14f, -40f, -0.6f, 28f, 40f, 0f, Set.of(Direction.NORTH)), PartPose.ZERO);
    }

    /** Draw in card-local coordinates, 0.1 model pixels in front of the base face. */
    public void renderOverlay(PoseStack poseStack, VertexConsumer buffer, int light, int overlay,
                              float alpha, boolean backUv) {
        ModelPart face = backUv ? backUvOverlay : frontOverlay;
        face.render(poseStack, buffer, light, overlay, 1f, 1f, 1f, alpha);
    }

    /** yaw 是实体朝向（度）。加 180° 后牌面正对 yaw 指向的一侧。 */
    public void setYaw(float yawDegrees) {
        card.yRot = Mth.wrapDegrees(180f + yawDegrees) * Mth.DEG_TO_RAD;
        card.xRot = 0f;
        card.zRot = 0f;
    }

    /** 把 PoseStack 变换到卡牌部件的本地坐标（之后盒子占 x∈[-0.875,0.875]、y∈[-2.5,0]）。 */
    public void translateToCard(PoseStack poseStack) {
        card.translateAndRotate(poseStack);
    }

    @Override
    public void setupAnim(FateCardEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        setYaw(netHeadYaw);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        card.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
