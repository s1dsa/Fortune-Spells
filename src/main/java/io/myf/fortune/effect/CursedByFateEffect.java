package io.myf.fortune.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class CursedByFateEffect extends MobEffect {
    public CursedByFateEffect() {
        super(MobEffectCategory.HARMFUL, 0x6B5B7B);
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        return new ArrayList<>();
    }
}
