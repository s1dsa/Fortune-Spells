package io.myf.fortune.client.render;

import io.myf.fortune.FortuneIds;
import io.myf.fortune.expansion.offense.SpellFigure;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** Finish an observed visual locally if release and removal arrive in the same network burst. */
@Mod.EventBusSubscriber(modid=FortuneIds.MODID,value=Dist.CLIENT)
public final class RitualAfterglow {
    private record Tail(SpellFigure figure,long started) {}
    private static final Set<SpellFigure> OBSERVED=Collections.newSetFromMap(new WeakHashMap<>());
    private static final ArrayList<Tail> TAILS=new ArrayList<>();
    private static long tick;

    private RitualAfterglow() {}

    static void observed(SpellFigure figure) {
        OBSERVED.add(figure);
    }

    @SubscribeEvent public static void removed(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide || !(event.getEntity() instanceof SpellFigure figure)
                || !OBSERVED.remove(figure) || figure.runePhase()!=SpellFigure.RunePhase.RELEASED) return;
        int phaseAge=figure.runePhaseAge();
        if (phaseAge<17) TAILS.add(new Tail(figure,tick-phaseAge));
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase!=TickEvent.Phase.START || Minecraft.getInstance().isPaused()) return;
        tick++;
        if (!TAILS.isEmpty()) {
            var world=Minecraft.getInstance().level;
            TAILS.removeIf(tail -> tail.figure.level()!=world || tick-tail.started>=17
                    || world.getEntity(tail.figure.getId())!=null);
        }
    }

    @SubscribeEvent public static void draw(RenderLevelStageEvent event) {
        if (TAILS.isEmpty() || event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        var mc=Minecraft.getInstance();
        var camera=event.getCamera().getPosition();
        var pose=event.getPoseStack();
        var buffers=mc.renderBuffers().bufferSource();
        for (Tail tail:TAILS) {
            SpellFigure f=tail.figure;
            if (f.level()!=mc.level || !f.shouldRenderAtSqrDistance(f.distanceToSqr(camera))
                    || !event.getFrustum().isVisible(f.getBoundingBoxForCulling())) continue;
            pose.pushPose();
            pose.translate(f.getX()-camera.x,f.getY()-camera.y,f.getZ()-camera.z);
            int scale=f.effectScale();
            pose.scale(scale,scale,scale);
            GrandRitualRenderer.render(f,(int)(tick-tail.started),event.getPartialTick(),pose,buffers);
            pose.popPose();
        }
    }
}
