package io.myf.fortune.mixin;

import io.myf.fortune.expansion.movement.MovementSpells;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Player.class)
abstract class BoneHealthMixin {
    // Vanilla subtracts this remainder from the mitigated hit to find how much gold to spend.
    @ModifyVariable(method = {"actuallyHurt", "m_6475_"}, at = @At(value = "STORE", ordinal = 0), index = 3, remap = false)
    private float fortuneSpells$keepGoldBehindBone(float remainder, DamageSource source, float mitigatedHit) {
        float bone = MovementSpells.boneHealth((Player)(Object)this);
        return Math.max(remainder, Math.min(bone, mitigatedHit));
    }

    // The second store is Forge's final onLivingDamage result, immediately before HP loss.
    @ModifyVariable(method = {"actuallyHurt", "m_6475_"}, at = @At(value = "STORE", ordinal = 1), index = 3, remap = false)
    private float fortuneSpells$spendBoneHearts(float applied) {
        MovementSpells.spendBoneHealth((Player)(Object)this, applied);
        return applied;
    }
}
