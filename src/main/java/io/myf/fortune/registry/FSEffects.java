package io.myf.fortune.registry;

import io.myf.fortune.FortuneIds;
import io.myf.fortune.effect.BlessedByFortuneEffect;
import io.myf.fortune.effect.CursedByFateEffect;
import io.myf.fortune.effect.AllInEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class FSEffects {
    public static final DeferredRegister<MobEffect> REGISTER = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, FortuneIds.MODID);
    public static final RegistryObject<MobEffect> BLESSED_BY_FORTUNE = REGISTER.register(
            FortuneIds.EFFECT_BLESSED_BY_FORTUNE.getPath(), BlessedByFortuneEffect::new);
    public static final RegistryObject<MobEffect> CURSED_BY_FATE = REGISTER.register(
            FortuneIds.EFFECT_CURSED_BY_FATE.getPath(), CursedByFateEffect::new);
    public static final RegistryObject<MobEffect> ALL_IN = REGISTER.register(
            FortuneIds.EFFECT_ALL_IN.getPath(), AllInEffect::new);

    private FSEffects() {}
}
