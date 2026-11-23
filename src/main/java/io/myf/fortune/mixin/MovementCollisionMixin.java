package io.myf.fortune.mixin;

import io.myf.fortune.expansion.movement.MovementSpells;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
abstract class MovementCollisionMixin {
    @Inject(method = {"collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
            "m_20272_(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"},
            at = @At("HEAD"), cancellable = true, remap = false)
    private void fortuneSpells$phaseMovement(Vec3 requested, CallbackInfoReturnable<Vec3> ci) {
        if ((Object)this instanceof Player player && player.hasEffect(MovementSpells.PHASING.get()))
            ci.setReturnValue(requested);
    }

    @Inject(method = {"collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
            "m_20272_(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"},
            at = @At("RETURN"), cancellable = true, remap = false)
    private void fortuneSpells$burrowMovement(Vec3 requested, CallbackInfoReturnable<Vec3> ci) {
        if ((Object)this instanceof Player player && player.hasEffect(MovementSpells.BOG_BURROW.get())
                && !player.hasEffect(MovementSpells.PHASING.get()))
            ci.setReturnValue(MovementSpells.burrowMovement(player, requested, ci.getReturnValue()));
    }
}
