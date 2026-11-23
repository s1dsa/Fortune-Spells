package io.myf.fortune.registry;

import io.myf.fortune.FortuneIds;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.registries.RegistryObject;

public enum BossSchools {
    BELL("bell", 0x7FE3D8), SWAMP("swamp", 0xB7D45A), ROSE("rose", 0xD7263D);

    public final ResourceLocation id;
    final RegistryObject<Attribute> power;
    final RegistryObject<Attribute> resist;

    BossSchools(String path, int color) {
        id = FortuneIds.id(path);
        power = FSAttributes.attribute(FortuneIds.id(path + "_spell_power"));
        resist = FSAttributes.attribute(FortuneIds.id(path + "_magic_resist"));
        FSSchools.REGISTER.register(path, () -> new SchoolType(id,
                TagKey.create(Registries.ITEM, FortuneIds.id(path + "_focus")),
                Component.translatable("school.fortune_spells." + path).withStyle(style -> style.withColor(color)),
                power, resist, () -> SoundEvents.EVOKER_CAST_SPELL,
                ResourceKey.create(Registries.DAMAGE_TYPE, FortuneIds.id(path + "_magic")), false, false));
    }
}
