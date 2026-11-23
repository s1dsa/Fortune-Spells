package io.myf.fortune.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class AllInEffect extends MobEffect {
    public AllInEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xF0506E);
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        return new ArrayList<>();
    }
}
