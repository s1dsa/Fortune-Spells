package io.myf.fortune.mixin;

import io.myf.fortune.combat.FortuneCombatHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Player.class)
abstract class PlayerMixin {
    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
    private DamageSource fortuneSpells$selectPlayerSource(DamageSource source, DamageSource original, float amount) {
        return FortuneCombatHandler.selectSource((Player) (Object) this, source, amount);
    }
}
