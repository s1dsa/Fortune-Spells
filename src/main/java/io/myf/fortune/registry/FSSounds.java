package io.myf.fortune.registry;

import io.myf.fortune.FortuneIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class FSSounds {
    public static final DeferredRegister<SoundEvent> REGISTER = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, FortuneIds.MODID);
    public static final RegistryObject<SoundEvent> SPELL_FORTUNE_CAST = sound(FortuneIds.SOUND_SPELL_FORTUNE_CAST);
    public static final RegistryObject<SoundEvent> SPELL_FORTUNE_CHARGE = sound(FortuneIds.SOUND_SPELL_FORTUNE_CHARGE);
    public static final RegistryObject<SoundEvent> CARD_APPEAR = sound(FortuneIds.SOUND_CARD_APPEAR);
    public static final RegistryObject<SoundEvent> CARD_SHUFFLE = sound(FortuneIds.SOUND_CARD_SHUFFLE);
    public static final RegistryObject<SoundEvent> CARD_FLIP = sound(FortuneIds.SOUND_CARD_FLIP);
    public static final RegistryObject<SoundEvent> CARD_RIGHT = sound(FortuneIds.SOUND_CARD_RIGHT);
    public static final RegistryObject<SoundEvent> CARD_WRONG = sound(FortuneIds.SOUND_CARD_WRONG);
    public static final RegistryObject<SoundEvent> FATE_LAUGH = sound(FortuneIds.SOUND_FATE_LAUGH);
    public static final RegistryObject<SoundEvent> FATE_STRIKE = sound(FortuneIds.SOUND_FATE_STRIKE);
    public static final RegistryObject<SoundEvent> ALL_IN_CAST = sound(FortuneIds.SOUND_ALL_IN_CAST);
    public static final RegistryObject<SoundEvent> ALL_IN_PROC = sound(FortuneIds.SOUND_ALL_IN_PROC);

    private FSSounds() {}

    private static RegistryObject<SoundEvent> sound(ResourceLocation id) {
        return REGISTER.register(id.getPath(), () -> SoundEvent.createVariableRangeEvent(id));
    }
}
