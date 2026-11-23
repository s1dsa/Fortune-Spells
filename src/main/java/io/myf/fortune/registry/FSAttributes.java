package io.myf.fortune.registry;

import io.myf.fortune.FortuneIds;
import io.redspace.ironsspellbooks.api.attribute.MagicRangedAttribute;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = FortuneIds.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class FSAttributes {
    public static final DeferredRegister<Attribute> REGISTER = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, FortuneIds.MODID);
    public static final RegistryObject<Attribute> FORTUNE_SPELL_POWER = attribute(FortuneIds.ATTR_FORTUNE_SPELL_POWER);
    public static final RegistryObject<Attribute> FORTUNE_MAGIC_RESIST = attribute(FortuneIds.ATTR_FORTUNE_MAGIC_RESIST);

    private FSAttributes() {}

    static RegistryObject<Attribute> attribute(ResourceLocation id) {
        return REGISTER.register(id.getPath(), () ->
                new MagicRangedAttribute(Util.makeDescriptionId("attribute", id), 1.0, -100, 100).setSyncable(true));
    }

    @SubscribeEvent
    public static void addToLivingEntities(EntityAttributeModificationEvent event) {
        for (var type : event.getTypes()) {
            for (var school : BossSchools.values()) {
                event.add(type, school.power.get());
                event.add(type, school.resist.get());
            }
            event.add(type, FORTUNE_SPELL_POWER.get());
            event.add(type, FORTUNE_MAGIC_RESIST.get());
        }
    }
}
