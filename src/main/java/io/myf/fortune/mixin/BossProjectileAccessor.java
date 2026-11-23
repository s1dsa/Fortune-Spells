package io.myf.fortune.mixin;

import lykrast.meetyourfight.entity.ProjectileBossAbstract;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ProjectileBossAbstract.class, remap = false)
public interface BossProjectileAccessor {
    @Accessor("fired")
    boolean fortuneSpells$hasFired();
}
