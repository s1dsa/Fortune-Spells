package io.myf.fortune.registry;

import io.myf.fortune.FortuneIds;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.tags.TagKey;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class FSSchools {
    public static final DeferredRegister<SchoolType> REGISTER = DeferredRegister.create(SchoolRegistry.SCHOOL_REGISTRY_KEY, FortuneIds.MODID);
    public static final RegistryObject<SchoolType> FORTUNE = REGISTER.register(FortuneIds.SCHOOL_FORTUNE.getPath(),
            () -> new SchoolType(FortuneIds.SCHOOL_FORTUNE, TagKey.create(Registries.ITEM, FortuneIds.TAG_FORTUNE_FOCUS),
                    Component.translatable(Util.makeDescriptionId("school", FortuneIds.SCHOOL_FORTUNE))
                            .withStyle(Style.EMPTY.withColor(0xF0506E)),
                    FSAttributes.FORTUNE_SPELL_POWER, FSAttributes.FORTUNE_MAGIC_RESIST,
                    FSSounds.SPELL_FORTUNE_CAST, FSDamageTypes.FORTUNE_MAGIC, false, false));

    private FSSchools() {}
}
