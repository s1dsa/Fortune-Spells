package io.myf.fortune.client;

import io.myf.fortune.FortuneIds;
import io.myf.fortune.client.audience.AudienceRitualRenderer;
import io.myf.fortune.client.audience.CataclysmRenderer;
import io.myf.fortune.client.audience.ResurrectionSealRenderer;
import io.myf.fortune.client.audience.StaffCinematic;
import io.myf.fortune.expansion.audience.AudienceContent;
import io.myf.fortune.client.cardgame.CardGameHud;
import io.myf.fortune.client.particle.CardShardParticle;
import io.myf.fortune.client.particle.FateSparkParticle;
import io.myf.fortune.client.particle.ForsakenMoteParticle;
import io.myf.fortune.client.render.FateCardModel;
import io.myf.fortune.client.render.FateCardRenderer;
import io.myf.fortune.client.render.RoseWraithRenderer;
import io.myf.fortune.expansion.crafting.ExpansionCrafting;
import io.myf.fortune.expansion.rose.RoseGuard;
import io.myf.fortune.registry.FSEntities;
import io.myf.fortune.registry.FSParticles;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterItemDecorationsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FortuneIds.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class FSClientSetup {
    private static final ResourceLocation INK_SLOT_BORDER = FortuneIds.id("textures/gui/ink_slot_border.png");

    private FSClientSetup() {}

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(StaffCinematic.RESTORE_VIEW);
        event.register(StaffCinematic.FREE_VIEW);
    }

    @SubscribeEvent
    public static void registerItemDecorations(RegisterItemDecorationsEvent event) {
        event.register(ExpansionCrafting.INK.get(), (graphics, font, stack, x, y) -> {
            // A slot has a one-pixel bevel around its 16-by-16 item.
            graphics.blit(INK_SLOT_BORDER, x - 1, y - 1, 0, 0, 18, 18, 18, 18);
            return false;
        });
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(FateCardModel.LAYER, FateCardModel::createLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(FSEntities.FATE_CARD.get(), FateCardRenderer::new);
        event.registerEntityRenderer(RoseGuard.WRAITH.get(), RoseWraithRenderer::new);
        event.registerEntityRenderer(AudienceContent.RITUAL.get(), AudienceRitualRenderer::new);
        event.registerEntityRenderer(AudienceContent.CATACLYSM_FIELD.get(), CataclysmRenderer::new);
        event.registerEntityRenderer(AudienceContent.RESURRECTION_SEAL.get(), ResurrectionSealRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(FSParticles.FATE_SPARK.get(), FateSparkParticle.Provider::new);
        event.registerSpriteSet(FSParticles.CARD_SHARD.get(), CardShardParticle.Provider::new);
        event.registerSpriteSet(FSParticles.FORSAKEN_MOTE.get(), ForsakenMoteParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerOverlay(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "card_game", CardGameHud.INSTANCE);
    }
}
