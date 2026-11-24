package io.myf.fortune.combat;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** MmmMmmMmmMmm normally registers the player for damage numbers inside hurt(), which staff spells skip. */
final class TargetDummyTracking {
    private static final String DUMMY_CLASS = "net.mehvahdjukaar.dummmmmmy.common.TargetDummyEntity";
    private static Field playersTracker;
    private static Method track;
    private static boolean unavailable;

    private TargetDummyTracking() {}

    static void trackHit(LivingEntity target, ServerPlayer caster) {
        if (unavailable || !DUMMY_CLASS.equals(target.getClass().getName())) return;
        try {
            if (track == null) {
                playersTracker = target.getClass().getDeclaredField("playersTracker");
                playersTracker.setAccessible(true);
                track = playersTracker.getType().getDeclaredMethod("track", ServerPlayer.class);
                track.setAccessible(true);
            }
            track.invoke(playersTracker.get(target), caster);
        } catch (ReflectiveOperationException | RuntimeException e) {
            unavailable = true;
            LogUtils.getLogger().warn("Could not register staff hits with the training dummy's damage meter", e);
        }
    }
}
