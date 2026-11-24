package io.myf.fortune.combat;

import io.myf.fortune.expansion.offense.ExposedEffect;
import io.myf.fortune.mixin.TotemAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;

/** The staff's spell controllers have already validated the target and computed their hit. */
public final class StaffSpellDamage {
    private StaffSpellDamage() {}

    public static boolean apply(LivingEntity target, DamageSource source, float amount) {
        if (!(target.level() instanceof ServerLevel level) || !target.isAlive() || !(amount > 0)
                || target.isSpectator() || (target instanceof Player player && player.isCreative())) return false;
        float damage = amount * ExposedEffect.damageMultiplier(target);
        if (source.getEntity() instanceof LivingEntity caster) {
            target.setLastHurtByMob(caster);
            if (caster instanceof Player player) target.setLastHurtByPlayer(player);
            caster.setLastHurtMob(target);
            if (caster instanceof ServerPlayer player) TargetDummyTracking.trackHit(target, player);
        }
        target.getCombatTracker().recordDamage(source, damage);
        // Vanilla clamps inside setHealth. Damage meters need the untruncated delta to see overkill.
        setHealth(target, target.getHealth() - damage);
        target.hurtTime = target.hurtDuration = 10;
        level.broadcastDamageEvent(target, source);
        target.gameEvent(GameEvent.ENTITY_DAMAGE);
        // Let bosses play their native death sequences, and let players use a real totem.
        if (target.isDeadOrDying() && !((TotemAccess) target).fortuneSpells$tryTotem(source)) target.die(source);
        return true;
    }

    static void setHealth(LivingEntity target, float health) {
        if (target instanceof FortuneCombatHandler.BossInput boss) boss.fortuneSpells$setExactHealth(Math.max(0, health));
        else target.setHealth(health);
    }
}
