package io.myf.fortune.client.audience;

import io.myf.fortune.FortuneIds;
import io.myf.fortune.expansion.audience.AudienceStaffItem;
import io.myf.fortune.expansion.audience.CataclysmField;
import io.myf.fortune.expansion.offense.SpellFigure;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(modid = FortuneIds.MODID, value = Dist.CLIENT)
public final class StaffCinematic {
    public static final KeyMapping RESTORE_VIEW = new KeyMapping("key.fortune_spells.restore_view",
            GLFW.GLFW_KEY_U, "key.categories.gameplay");
    public static final KeyMapping FREE_VIEW = new KeyMapping("key.fortune_spells.free_view",
            GLFW.GLFW_KEY_I, "key.categories.gameplay");
    private enum View { AERIAL, FREE, CHARACTER }
    private static View view = View.CHARACTER;
    private static Vec3 freePosition, previousPosition;
    private static float freeYaw, freePitch;
    private static ClientLevel world;
    private static long start, end, freeEnd;
    private static Vec3 focus;
    private static float heading;
    private static boolean grand, nearCompletion;
    private static int duration;
    private static CataclysmField field;
    private static final Set<CataclysmField> fields = new HashSet<>();

    private StaffCinematic() {}

    public static void begin(String spell, CastSource source, String slot, int castTicks) {
        reset();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || source != CastSource.SWORD || !AudienceStaffItem.isStaffSlot(mc.player, slot)) return;
        grand = "fortune_spells:cataclysm".equals(spell);
        world = mc.level;
        view = View.AERIAL;
        start = world.getGameTime();
        duration = castTicks;
        end = start + duration + (grand ? 60 : 28);
        // Free flight lasts through the final bite, toll, fire field or wing reveal.
        int releaseTicks = switch (spell) {
            case "fortune_spells:croc_devour" -> SpellFigure.JAWS_DURATION;
            case "fortune_spells:phantom_belfry" -> 150;
            case "fortune_spells:sunset_blade_dance" -> 100;
            case "fortune_spells:bone_wings" -> 52;
            default -> CataclysmField.FALL_TICKS + CataclysmField.AFTERGLOW_TICKS;
        };
        freeEnd = start + duration + releaseTicks;
        focus = mc.player.position();
        heading = mc.player.getYRot();
        mc.player.displayClientMessage(Component.translatable("message.fortune_spells.aerial_view",
                RESTORE_VIEW.getTranslatedKeyMessage(), FREE_VIEW.getTranslatedKeyMessage()), true);
    }

    private static void reset() {
        world = null; field = null; focus = null; nearCompletion = false;
        view = View.CHARACTER;
        freePosition = previousPosition = null;
    }
    public static boolean active() {
        return world != null && Minecraft.getInstance().level == world && view != View.CHARACTER;
    }
    public static boolean wideView() {
        return active() || !fields.isEmpty();
    }

    @SubscribeEvent public static void fieldAppears(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() && event.getEntity() instanceof CataclysmField cast) fields.add(cast);
    }
    @SubscribeEvent public static void fieldLeaves(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide() && event.getEntity() instanceof CataclysmField cast) fields.remove(cast);
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        // Take ownership before LocalPlayer samples movement, including I and W pressed together.
        if (event.phase != TickEvent.Phase.START) return;
        Minecraft mc = Minecraft.getInstance();
        boolean enterFree = false;
        boolean restore = false;
        while (FREE_VIEW.consumeClick()) enterFree = true;
        while (RESTORE_VIEW.consumeClick()) restore = true;
        if (mc.level == null || mc.player == null) { reset(); fields.clear(); return; }
        if (world != null && world != mc.level) reset();
        if (mc.isPaused()) return;
        if (world != null && grand && field == null) {
            for (CataclysmField cast : fields) {
                if (cast.level() == world && cast.ownerId() == mc.player.getId() && !cast.released()) { field = cast; break; }
            }
        }
        if (world == null) return;
        long now = world.getGameTime();
        if (!mc.player.isAlive() || mc.getCameraEntity() != mc.player || now >= Math.max(end, freeEnd)) { reset(); return; }
        if ((view == View.FREE && now >= freeEnd) || (view == View.AERIAL && now >= end)) view = View.CHARACTER;
        if (field != null) focus = field.position();
        else if (!grand && ClientMagicData.isCasting()) focus = mc.player.position();
        if (ClientMagicData.isCasting()) {
            nearCompletion |= ClientMagicData.getCastDurationRemaining() <= 3;
        } else if (now > start + 4 && !nearCompletion && now < start + duration - 3) {
            reset();
            return;
        }
        if (restore) view = View.CHARACTER;
        else if (enterFree && view != View.FREE && now < freeEnd && mc.screen == null) {
            Camera camera = mc.gameRenderer.getMainCamera();
            freePosition = previousPosition = camera.getPosition();
            freeYaw = camera.getYRot();
            freePitch = camera.getXRot();
            view = View.FREE;
        }
        if (view == View.FREE) {
            previousPosition = freePosition;
            if (mc.screen == null) fly(mc);
        }
    }

    private static void fly(Minecraft mc) {
        var keys = mc.options;
        int forward = axis(keys.keyUp, keys.keyDown);
        int right = axis(keys.keyRight, keys.keyLeft);
        int up = axis(keys.keyJump, keys.keyShift);
        if (forward == 0 && right == 0 && up == 0) return;
        double yaw = Math.toRadians(freeYaw);
        Vec3 direction = new Vec3(-Math.sin(yaw) * forward - Math.cos(yaw) * right, up,
                Math.cos(yaw) * forward - Math.sin(yaw) * right);
        freePosition = freePosition.add(direction.normalize().scale(keys.keySprint.isDown() ? 6 : 1.65));
    }

    private static int axis(KeyMapping positive, KeyMapping negative) {
        return (positive.isDown() ? 1 : 0) - (negative.isDown() ? 1 : 0);
    }

    public static boolean turnFreeCamera(double dx, double dy) {
        if (view != View.FREE || !active()) return false;
        freeYaw = Mth.wrapDegrees(freeYaw + (float)dx * .15f);
        freePitch = Mth.clamp(freePitch + (float)dy * .15f, -90, 90);
        return true;
    }

    @SubscribeEvent public static void captureFlightKeys(MovementInputUpdateEvent event) {
        if (view != View.FREE || !active()) return;
        var input = event.getInput();
        input.leftImpulse = input.forwardImpulse = 0;
        input.up = input.down = input.left = input.right = input.jumping = input.shiftKeyDown = false;
    }

    public static void place(Camera camera, float partial) {
        if (!active() || camera.getEntity() != Minecraft.getInstance().player) return;
        if (view == View.FREE) {
            camera.setPosition(previousPosition.lerp(freePosition, partial));
            camera.setRotation(freeYaw, freePitch);
            return;
        }
        float elapsed = world.getGameTime() - start + partial;
        float rise = ease(elapsed / (grand ? 168 : Math.min(60, duration * .55f)));
        float amount = rise * ease((end - start - elapsed) / 18f);
        double angle = Math.toRadians(heading);
        Minecraft mc = Minecraft.getInstance();
        double aspect = (double)mc.getWindow().getWidth() / Math.max(1, mc.getWindow().getHeight());
        double halfFov = Math.toRadians(mc.options.fov().get() * .5);
        double fit = Mth.clamp(.70 / (Math.tan(halfFov) * Math.min(1, aspect)), 1, 5);
        double height = (grand ? 800 : 104) * fit;
        double back = (grand ? 740 : 85) * fit;
        Vec3 eye = focus.add(Math.sin(angle) * back, height, -Math.cos(angle) * back);
        float pitch = (float) Math.toDegrees(Math.atan2(height - (grand ? CataclysmRenderer.TOP * .25f : 14), back));
        camera.setPosition(camera.getPosition().lerp(eye, amount));
        camera.setRotation(Mth.rotLerp(amount, camera.getYRot(), heading), Mth.lerp(amount, camera.getXRot(), pitch));
    }

    private static float ease(float progress) {
        float t = Mth.clamp(progress, 0, 1);
        return t * t * (3 - 2 * t);
    }

    @SubscribeEvent public static void hand(RenderHandEvent event) {
        if (active()) event.setCanceled(true);
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        reset(); fields.clear();
    }
}
