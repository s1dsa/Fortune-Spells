package io.myf.fortune.expansion.audience;

import io.myf.fortune.FortuneIds;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class AudienceContent {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FortuneIds.MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, FortuneIds.MODID);
    public static final RegistryObject<Item> STAFF = ITEMS.register("audience_staff", AudienceStaffItem::new);
    public static final RegistryObject<EntityType<ResurrectionSeal>> RESURRECTION_SEAL = ENTITIES.register("resurrection_seal",
            () -> EntityType.Builder.<ResurrectionSeal>of(ResurrectionSeal::new, MobCategory.MISC).sized(1, .1f)
                    .clientTrackingRange(8).updateInterval(20).noSummon().noSave().fireImmune().build("fortune_spells:resurrection_seal"));
    public static final RegistryObject<EntityType<CataclysmField>> CATACLYSM_FIELD = ENTITIES.register("cataclysm_field",
            () -> EntityType.Builder.<CataclysmField>of(CataclysmField::new, MobCategory.MISC).sized(1, .2f)
                    .clientTrackingRange(48).updateInterval(20).noSummon().noSave().fireImmune().build("fortune_spells:cataclysm_field"));
    public static final RegistryObject<EntityType<AudienceRitual>> RITUAL = ENTITIES.register("audience_ritual",
            () -> EntityType.Builder.<AudienceRitual>of(AudienceRitual::new, MobCategory.MISC).sized(1f, .3f)
                    .clientTrackingRange(12).updateInterval(20).noSummon().fireImmune().build("fortune_spells:audience_ritual"));

    private AudienceContent() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        ENTITIES.register(bus);
    }
}
