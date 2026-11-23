package io.myf.fortune.expansion;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;

public final class SpellTargets {
    private SpellTargets() {}

    public static boolean canHit(LivingEntity caster, LivingEntity target) {
        return canHit(caster, target, false);
    }

    public static boolean canHit(LivingEntity caster, LivingEntity target, boolean staffSpell) {
        if (target == caster || !target.isAlive() || (!staffSpell && !target.isAttackable()) || target.isSpectator()
                || target instanceof ArmorStand) return false;
        if (target instanceof Player player) {
            if (player.isCreative()) return false;
            if (caster instanceof Player attacker) {
                var server = attacker.getServer();
                return server != null && server.isPvpAllowed() && attacker.canHarmPlayer(player);
            }
        }
        return true;
    }
}
