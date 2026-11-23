package io.myf.fortune.expansion.offense;

import io.myf.fortune.FortuneIds;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = FortuneIds.MODID)
public final class OffensiveControl {
    private static final String STUN = "fortune_spells:stun_origin";
    private static final String BURIAL = "fortune_spells:burial_origin";
    private static final Map<LivingEntity, SpellFigure> DANCES = new WeakHashMap<>();
    public static final String DANCE_ID = FortuneIds.MODID + ":sunset_blade_dance";

    private OffensiveControl() {}

    static void beginDance(LivingEntity caster, SpellFigure dance) {
        cancelDance(caster);
        if (caster instanceof ServerPlayer player) {
            MagicData data = MagicData.getPlayerMagicData(player);
            if (data.isCasting() && DANCE_ID.equals(data.getCastingSpellId())) {
                dance.bindToCast(SpellSelectionManager.OFFHAND.equals(data.getCastingEquipmentSlot())
                        ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            }
        }
        DANCES.put(caster, dance);
    }

    public static void advanceDance(ServerPlayer caster, int elapsed) {
        SpellFigure dance = DANCES.get(caster);
        if (dance != null && !dance.isRemoved()) dance.channelTick(caster, elapsed);
    }

    public static void finishDance(ServerPlayer caster) {
        SpellFigure dance = DANCES.get(caster);
        if (dance != null && !dance.isRemoved() && caster.isAlive()) dance.releaseDance(caster.serverLevel(), caster);
    }

    static void endDance(LivingEntity caster, SpellFigure dance) {
        DANCES.remove(caster, dance);
    }

    static boolean isActiveDance(LivingEntity caster, SpellFigure dance) {
        return DANCES.get(caster) == dance;
    }

    public static void cancelDance(LivingEntity caster) {
        SpellFigure dance = DANCES.remove(caster);
        if (dance != null) dance.discard();
    }

    @SubscribeEvent public static void leaveLevel(EntityLeaveLevelEvent event) {
        // Forge can stop tracking a whole chunk while iterating its live entity list.
        // Revoke the stance now and let the figure clean up from its own tick.
        if (!event.getLevel().isClientSide && event.getEntity() instanceof LivingEntity caster) DANCES.remove(caster);
    }

    @SubscribeEvent
    public static void invulnerability(LivingAttackEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        if (event.getSource().getDirectEntity() instanceof LivingEntity attacker
                && attacker.hasEffect(OffensiveSpells.STUNNED.get())) event.setCanceled(true);
        SpellFigure dance = DANCES.get(entity);
        if (dance != null && !dance.isRemoved() && dance.level() == entity.level()
                && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) event.setCanceled(true);
        if (entity.getPersistentData().contains(BURIAL) && event.getSource().is(DamageTypes.IN_WALL))
            event.setCanceled(true);
    }

    static void bury(LivingEntity entity) {
        if (!entity.isAlive()) return;
        if (!entity.getPersistentData().contains(BURIAL)) {
            remember(entity, BURIAL);
            entity.getPersistentData().getCompound(BURIAL).putLong("end", entity.level().getGameTime() + 60);
        }
    }

    @SubscribeEvent
    public static void stunApplied(MobEffectEvent.Added event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || event.getEffectInstance().getEffect() != OffensiveSpells.STUNNED.get()) return;
        remember(entity, STUN);
        cancelDance(entity);
        entity.stopUsingItem();
        if (entity instanceof ServerPlayer player) Utils.serverSideCancelCast(player);
    }

    @SubscribeEvent public static void blockSpell(SpellPreCastEvent event) {
        if (event.getEntity().hasEffect(OffensiveSpells.STUNNED.get())) event.setCanceled(true);
    }
    @SubscribeEvent public static void blockAttack(AttackEntityEvent event) {
        if (handsBusy(event.getEntity())) event.setCanceled(true);
    }
    @SubscribeEvent public static void blockInteract(PlayerInteractEvent event) {
        if (event.isCancelable() && handsBusy(event.getEntity())) event.setCanceled(true);
    }
    @SubscribeEvent public static void blockItem(LivingEntityUseItemEvent.Start event) {
        if (handsBusy(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent public static void blockMining(net.minecraftforge.event.level.BlockEvent.BreakEvent event) {
        if (DANCES.containsKey(event.getPlayer())) event.setCanceled(true);
    }

    private static boolean handsBusy(LivingEntity entity) {
        return entity.hasEffect(OffensiveSpells.STUNNED.get()) || (!entity.level().isClientSide && DANCES.containsKey(entity));
    }

    private static void remember(LivingEntity entity, String key) {
        if (entity.getPersistentData().contains(key)) return;
        CompoundTag tag = new CompoundTag();
        tag.putDouble("x", entity.getX());
        tag.putDouble("y", entity.getY());
        tag.putDouble("z", entity.getZ());
        tag.putString("dim", entity.level().dimension().location().toString());
        entity.getPersistentData().put(key,tag);
    }

    @SubscribeEvent
    public static void constrain(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !entity.isAlive()) return;
        CompoundTag data = entity.getPersistentData();
        boolean buried = false;
        if (data.contains(BURIAL)) {
            CompoundTag anchor = data.getCompound(BURIAL);
            int remaining = (int)(anchor.getLong("end") - entity.level().getGameTime());
            if (sameDimension(entity, anchor)) {
                buried = remaining > 0;
                double sink = Math.min(1, Math.min(60 - remaining, Math.max(0, remaining)) / 4d);
                pin(entity, anchor, -Math.max(0, sink) * .5);
            }
            if (remaining <= 0 || !sameDimension(entity, anchor)) data.remove(BURIAL);
        }
        if (entity.hasEffect(OffensiveSpells.STUNNED.get())) {
            CompoundTag anchor = data.getCompound(STUN);
            if (!sameDimension(entity, anchor)) {
                boolean resumeAI = anchor.getBoolean("resumeAI");
                data.remove(STUN);
                remember(entity, STUN);
                anchor = data.getCompound(STUN);
                anchor.putBoolean("resumeAI", resumeAI);
            }
            if (!buried) pin(entity, anchor, 0);
            if (entity instanceof Mob mob && !mob.isNoAi()) {
                anchor.putBoolean("resumeAI",true);
                mob.setNoAi(true);
            }
        } else if (data.contains(STUN)) {
            if (entity instanceof Mob mob && data.getCompound(STUN).getBoolean("resumeAI")) mob.setNoAi(false);
            data.remove(STUN);
        }
    }

    private static boolean sameDimension(LivingEntity entity, CompoundTag anchor) {
        return entity.level().dimension().location().toString().equals(anchor.getString("dim"));
    }

    private static void pin(LivingEntity entity, CompoundTag tag, double offset) {
        Vec3 pos = new Vec3(tag.getDouble("x"),tag.getDouble("y")+offset,tag.getDouble("z"));
        if (entity.distanceToSqr(pos) > .0001) entity.teleportTo(pos.x, pos.y, pos.z);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.fallDistance = 0;
        if (entity instanceof Mob mob) mob.getNavigation().stop();
    }
}
