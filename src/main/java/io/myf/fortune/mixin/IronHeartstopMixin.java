package io.myf.fortune.mixin;

import io.myf.fortune.registry.FSDamageTypes;
import io.redspace.ironsspellbooks.player.ServerPlayerEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ServerPlayerEvents.class, remap = false)
abstract class IronHeartstopMixin {
    // Iron's 3.16.4 checks Heartstop here before banking half the damage and zeroing the event.
    @Redirect(method = "onBeforeDamageTaken", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/world/effect/MobEffect;)Z"),
            allow = 1)
    private static boolean fortuneSpells$deferToHeartstop(LivingEntity target, MobEffect heartstop,
                                                         LivingDamageEvent event) {
        return !event.getSource().is(FSDamageTypes.FORTUNE) && target.hasEffect(heartstop);
    }
}
