package io.myf.fortune.client.audience;

import com.google.common.collect.MapMaker;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.myf.fortune.FortuneIds;
import io.myf.fortune.expansion.audience.StaffAttributes;
import io.myf.fortune.expansion.audience.StaffOrbitClock;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

@Mod.EventBusSubscriber(modid = FortuneIds.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class StaffOrbitLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final Map<Player, Long> STARTS = new MapMaker().weakKeys().makeMap();
    private static final long STOPPED = Long.MIN_VALUE;
    private static final int DRAW = 64;
    private static final int PAIR_DELAY = 6;
    private static final int PAIRS = 4;

    private StaffOrbitLayer(PlayerRenderer parent) { super(parent); }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer)
                renderer.addLayer(new StaffOrbitLayer(renderer));
        }
    }

    public static void sync(Player wearer, int age) {
        // A stop can arrive before the equipment update. Keep it until the next equip event.
        STARTS.put(wearer, age < 0 ? STOPPED : wearer.level().getGameTime() - age);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer wearer,
                       float stride, float speed, float partial, float ageInTicks, float yaw, float pitch) {
        if (!StaffAttributes.heldBy(wearer)) return;
        if (wearer.isInvisible() || wearer.isSpectator() || !wearer.isAlive()) return;
        long now = wearer.level().getGameTime();
        long start = STARTS.computeIfAbsent(wearer, p -> now);
        if (start == STOPPED) return;
        long elapsed = Math.max(0, now - start);
        int cycle = StaffOrbitClock.CYCLE;
        float age = (elapsed < cycle ? elapsed : cycle + elapsed % cycle) + partial;
        float travel = Math.max(0, age - DRAW - PAIRS * PAIR_DELAY);
        // Ease in from stationary over the first second; one revolution takes 16 seconds at full speed.
        float phase = (travel < 20 ? travel * travel / 40 : travel - 10) * (Mth.TWO_PI / 320);
        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        // Leave room for the lowest dial's rim above the floor during a full revolution.
        pose.translate(0, -.28, .54);
        pose.pushPose();
        pose.scale(.65f, .65f, .65f);
        pose.mulPose(Axis.ZP.rotation(-phase));
        StaffOrbitSeals.render(pose, buffers, StaffOrbitSeals.AXIS, age / DRAW, age);
        pose.popPose();
        for (int i = 0; i < PAIRS * 2; i++) {
            float angle = (i + .5f) * (Mth.PI / PAIRS) + phase;
            boolean principal = (i & 1) == 0;
            float radius = principal ? .49f : .35f;
            pose.pushPose();
            pose.translate(1.60f * Mth.cos(angle), 1.24f * Mth.sin(angle), principal ? .05 : .19);
            pose.scale(radius, radius, radius);
            int delay = (1 + i % PAIRS) * PAIR_DELAY;
            StaffOrbitSeals.render(pose, buffers, i, (age - delay) / DRAW, age);
            pose.popPose();
        }
        pose.popPose();
    }
}
